package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.util.Date;
import java.io.*;
import java.net.*;

/**
 * A smart download.  Tries to get a group of similar files by delegating
 * to HTTPDownloader objects.  Does retries and resumes automatically.
 * Reports all changes to a DownloadManager.  This class is thread safe.<p>
 *
 * Smart downloads can use many policies, and these policies are free to change
 * as allowed by the Downloader specification.  This implementation tries the N
 * fastest hosts in parallel.  The first host to connect is declared the winner,
 * and the others are terminated.  The whole process is repeated until all hosts
 * are unreachable.  Note that the whole process will be repeated as long as we
 * get the busy signal from a host.<p>
 *
 * This class implements the Serializable interface but defines its own
 * writeObject and readObject methods.  This is necessary because parts of the
 * ManagedDownloader (e.g., sockets) are inherently unserializable.  For this
 * reason, serializing and deserializing a ManagedDownloader M results in a
 * ManagedDownloader M' that is the same as M except it is
 * unconnected. <b>Furthermore, it is necessary to explicitly call
 * initialize(..) after reading a ManagedDownloader from disk.</b> 
 */
public class ManagedDownloader implements Downloader, Serializable {
    /*********************************************************************
     * LOCKING: obtain this's monitor before modifying any of the following.
     ***********************************************************************/

    /** This' manager for callbacks and queueing. */
    private DownloadManager manager;
    /** The repository of incomplete files. */
    private IncompleteFileManager incompleteFileManager;
    /** The complete list of files passed to the constructor. */
    private RemoteFileDesc[] allFiles;

    ///////////////////////// Policy Controls ///////////////////////////
    /** The number of tries to make */
    private static final int TRIES=300;
    /** The max number of push tries to make, per host. */
    private static final int PUSH_TRIES=2;
    /** The max number of downloads to try in parallel. */
    private static final int PARALLEL_DOWNLOAD=4;
    /** The time to wait trying to establish each connection, in milliseconds.*/
    private static final int CONNECT_TIME=8000;  //8 seconds
    /** The maximum time, in SECONDS, allowed between a push request and an
     *  incoming push connection. */
    private final int PUSH_INVALIDATE_TIME=5*60;  //5 minutes
    /** Returns the amount of time to wait in milliseconds before retrying,
     *  based on tries.  This is also the time to wait for * incoming pushes to
     *  arrive, so it must not be too small.  A value of * tries==0 represents
     *  the first try.  */
    private long calculateWaitTime() {
        //60 seconds: same as BearShare.
        return 60*1000;
    }


    ////////////////////////// Core Variables /////////////////////////////
    /** The files to get, each represented by a RemoteFileDesc2 */
    private List /* of RemoteFileDesc2 */ files;
    /** The files/hosts that were busy. */
    private List /* of RemoteFileDesc2 */ busy;
    /** The list of all files we've requested via push messages.  Only files
     * matching this description may be accepted from incoming connections. */
    private List /* of RemoteFileDesc2 */ requested;
    /** If started, the thread trying to coordinate all downloads.  
     *  Otherwise null. */
    private Thread dloaderManagerThread;
    /** The connections we're using for the current attempts. */
    private List /* of HTTPDownloader */ dloaders;
    /** True iff this has been forcibly stopped. */
    private boolean stopped;


    ///////////////////////// Variables for GUI Display  /////////////////
    /** The current state.  One of Downloader.CONNECTING, Downloader.ERROR,
      *  etc.   Should be modified only through setState. */
    private int state;
    /** The time as returned by Date.getTime() that this entered the current
        state.  Should be modified only through setState. */
    private long stateTime;


    /**
     * Creates a new ManagedDownload to download the given files.  The download
     * attempts to begin immediately; there is no need to call initialize.
     * Non-blocking.
     *     @param manager the delegate for queueing purposes.  Also the callback
     *      for changes in state.
     *     @param files the list of files to get.  This stops after ANY of the
     *      files is downloaded.
     */
    public ManagedDownloader(DownloadManager manager, RemoteFileDesc[] files) {
        this.allFiles=files;
        incompleteFileManager=new IncompleteFileManager();
        initialize(manager);
    }

    /** See note on serialization at top of file */
    private void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.writeObject(allFiles);
        stream.writeObject(incompleteFileManager);
    }

    /** See note on serialization at top of file.  You must call initialize on
     *  this!  */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {        
        allFiles=(RemoteFileDesc[])stream.readObject();
        incompleteFileManager=(IncompleteFileManager)stream.readObject();
    }

    /** 
     * Initializes a ManagedDownloader read from disk.  Also used for internally
     * initializing or resuming a normal download; there is no need to
     * explicitly call this method in that case.  After the call, this is in the
     * queued state, at least for the moment.
     *     @requires this is uninitialized or stopped, 
     *      and allFiles, and incompleteFileManager are set
     *     @modifies everything but the above fields */
    public void initialize(DownloadManager manager) {
        this.manager=manager;

        //Copy allFiles into files, augmenting firewalled status.  We used to
        //sort before inerting, but that's no longer necessary.
        this.files=new LinkedList();
        for (int i=0; i<allFiles.length; i++) {
            RemoteFileDesc rfd=allFiles[i];           
            this.files.add(new RemoteFileDesc2(rfd, false));
        }
        busy=new LinkedList();
        requested=new LinkedList();
        dloaders=new LinkedList();
        stopped=false;
        setState(QUEUED);
            
        this.dloaderManagerThread=new Thread() {
            public void run() {
                tryAllDownloadsWithRetry();
            }
        };
        dloaderManagerThread.setDaemon(true);
        dloaderManagerThread.start();       
    }

    /**
     * Returns true if 'other' could conflict with one of the files in this.  In
     * other if this.conflicts(other)==true, no other ManagedDownloader should
     * attempt to download other.
     */
    public boolean conflicts(RemoteFileDesc other) {
        synchronized (this) {
            File otherFile=incompleteFileManager.getFile(other);
            //These iterators include any download in progress.
            for (Iterator iter=files.iterator(); iter.hasNext(); ) {
                RemoteFileDesc rfd=(RemoteFileDesc)iter.next();
                File thisFile=incompleteFileManager.getFile(rfd);
                if (thisFile.equals(otherFile))
                    return true;
            }
        }
        return false;
    }

    /**
     * Accepts a push download.  If this chooses to download the given file
     * (with given index and clientGUID) from socket, returns true.  In this
     * case, the caller may not make any modifications to the socket.  If this
     * rejects the given file, returns false without modifying this or socket.
     * If this could has problems with the socket, throws IOException.  In this
     * case the caller should close the socket.  Non-blocking.
     *     @modifies this, socket
     *     @requires GIV string (and nothing else) has been read from socket
     */
    public boolean acceptDownload(
            String file, Socket socket, int index, byte[] clientGUID)
            throws IOException {
        return false;   //TODO: re-enable
        /*
        //Authentication: check if we requested file from this host via push.
        //This ignores timestamps.  So first we clear out very old entries.
        RemoteFileDesc rfd=null;   
        synchronized (this) {
            purgeOldPushRequests();
            
            //Get original RemoteFileDesc to get file size to find temporary
            //file.  Unfortunately the size is NOT available from the GIV line.
            //(That's sort of a flaw of the Gnutella protocol.)  So we search
            //through the list of files in this, comparing file index numbers
            //and client GUID's.  We also check filenames, though this is not
            //strictly needed.
            for (Iterator iter=requested.iterator(); iter.hasNext(); ) {
                RemoteFileDesc other=(RemoteFileDesc)iter.next();
                if (other.getIndex()==index 
                        && other.getFileName().equals(file)
                        && (new GUID(other.getClientGUID())).
                             equals(new GUID(clientGUID))) {
                    rfd=other;
                    break;
                }
            }
            
            //No luck.
            if (rfd==null) 
                return false;
        }

        //Authentication ok.  Make and queue downloader.  Notify downloader
        //thread to consume this downloader.  If downloader isn't waiting, this
        //may not be serviced for some time.
        System.out.println("ACCEPT_DOWNLOAD: accepting download");
        final HTTPDownloader downloader=new HTTPDownloader(
            socket, rfd, incompleteFileManager.getFile(rfd));
        final RemoteFileDesc rfdAlias=rfd;  //make compiler happy
        Thread runner=new Thread() {
            public void run() {
                tryOneDownload(downloader, rfdAlias);
            }
         };
        runner.setDaemon(true);
        runner.start();
        return true;
        */
    }

    public synchronized void stop() {
        //This method is tricky.  Look carefully at run.  The most important
        //thing is to set the stopped flag.  That guarantees run will terminate
        //eventually.
        stopped=true;
        //This guarantees any downloads in progress will be killed.  New
        //downloads will not start because of the flag above.
        for (Iterator iter=dloaders.iterator(); iter.hasNext(); )
            ((HTTPDownloader)iter.next()).stop();

        //Interrupt thread if waiting to retry.  This is actually just an
        //optimization since the thread will terminate upon exiting wait.  In
        //fact, this may not do anything at all if the thread is just about to
        //enter the wait!  Still this is nice in case the thread is waiting for
        //a long time.
        if (dloaderManagerThread!=null)
            dloaderManagerThread.interrupt();
    }

    public synchronized boolean resume() throws AlreadyDownloadingException {
        //Ignore request if already in the download cycle.
        if (! (state==WAITING_FOR_RETRY || state==GAVE_UP))
            return false;
        //Sometimes a resume can cause a conflict.  So we check.
        String conflict=this.manager.conflicts(allFiles, this);
        if (conflict!=null)
            throw new AlreadyDownloadingException(conflict);

        if (stopped) {
            //This stopped because all hosts were tried.  (Note that this
            //couldn't have been user aborted.)  Therefore no threads are
            //running in this and it may be safely resumed.
            initialize(this.manager);
        } else {
            //Interrupt any waits.
            if (dloaderManagerThread!=null)
                dloaderManagerThread.interrupt();
        }
        return true;
    }

    public synchronized void launch() {
        /*
          //TODO: re-enable
        //We haven't started yet.
        if (dloader==null)
            return;

        //Unfortunately this must be done in a background thread because the
        //copying (see below) can take a lot of time.  If we can avoid the copy
        //in the future, we can avoid the thread.
        Thread worker=new Thread() {
            public void run() {              
                String name=dloader.getFileName();
                File file=null;

                //a) If the file is being downloaded, create *copy* of
                //incomplete file.  The copy is needed because some programs,
                //notably Windows Media Player, attempt to grab exclusive file
                //locks.  If the download hasn't started, the incomplete file
                //may not even exist--not a problem.
                if (dloader.getAmountRead()<dloader.getFileSize()) {
                    File incomplete=incompleteFileManager.
                        getFile(name, dloader.getFileSize());            
                    file=new File(incomplete.getParent(),
                                  IncompleteFileManager.PREVIEW_PREFIX
                                      +incomplete.getName());
                    if (! CommonUtils.copy(incomplete, file)) //note side-effect
                        return;
                }
                //b) Otherwise, choose completed file.
                else {
					File saveDir = null;
					try {
						saveDir = SettingsManager.instance().getSaveDirectory();
					} catch(java.io.FileNotFoundException fnfe) {
						// simply return if we could not get the save directory.
						return;
					}
					file=new File(saveDir,name);     
				}

                try {
                    Launcher.launchFile(file);
                } catch (IOException e) { }
            }
        };
        worker.setDaemon(true);
        worker.setName("Launcher thread");
        worker.start();
        */
    }


    ///////////////////////////// Core Downloading Logic ///////////////////////

    /** Actually does the download, trying all locations, resuming, waiting, and
     *  retrying as necessary.  Called from dloadManagerThread. */
    private void tryAllDownloadsWithRetry() {
        //0. Verify it's safe to download.  Filename must not have "..", "/",
        //   etc.  We check this by looking where the downloaded file will
        //   end up.
        RemoteFileDesc rfd0=allFiles[0];       //TODO: assumes all the same!
        int fileSize=rfd0.getSize(); 
        String filename=rfd0.getFileName(); 
        File incompleteFile=incompleteFileManager.getFile(rfd0);
        File sharedDir;
        File completeFile;
        try {
            sharedDir=SettingsManager.instance().getSaveDirectory();
            completeFile=new File(sharedDir, filename);
            String sharedPath = sharedDir.getCanonicalPath();		
            String completeFileParentPath = 
            new File(completeFile.getParent()).getCanonicalPath();
            if (!sharedPath.equals(completeFileParentPath))
                throw new InvalidPathException();  
        } catch (IOException e) {
            setState(COULDNT_MOVE_TO_LIBRARY);
            return;
        }        

        //1. Start up to P downloads at once
        setState(CONNECTING);
        HTTPDownloader previousDownloader=null;
    outerLoop:
        for (int i=0; i<PARALLEL_DOWNLOAD; i++) {
            //Search for a connectable downloader
            HTTPDownloader downloader;
            RemoteFileDesc rfd;
            int start=(i*fileSize)/PARALLEL_DOWNLOAD;
            int stop=fileSize-start;
            while (true) {
                if (files.size()==0)
                    break outerLoop;
                rfd=removeBest(files);      
                downloader=new HTTPDownloader(rfd, incompleteFile, start, stop);
                try {
                    System.out.println("MANAGER: trying connect to "+rfd);
                    downloader.connect(CONNECT_TIME);
                    break;
                } catch (IOException e) {
                    continue;
                }
            }
            
            //Tell previous downloader not to clobber the first            
            if (previousDownloader!=null)
                previousDownloader.stopAt(start);
            
            //And spawn a thread to start the first
            System.out.println(
                "MANAGER: downloading from "+start+" to "+stop+" with "+rfd);
            setState(DOWNLOADING);
            synchronized (this) {
                if (stopped)
                    return;
                dloaders.add(downloader);
            }
            final RemoteFileDesc rfd2=rfd;
            final HTTPDownloader downloader2=downloader;
            Thread worker=new Thread() {
                public void run() {
                    tryOneDownload(downloader2, rfd2);
                }
            };
            worker.setDaemon(true);
            worker.start();            
            previousDownloader=downloader;
        }
  
        
        //2. Wait for them to finish.  TODO: handle interrupts, couldn't
        //download, etc.
        System.out.println("MANAGER: waiting for complete");
        synchronized (this) {
            if (stopped)
                return;
            while (dloaders.size()>0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    if (stopped)
                        return;
                }
            }
            if (stopped)
                return;
        }

        //3. Move to library.  
        System.out.println("MANAGER: completed");
        //Delete target.  If target doesn't exist, this will fail silently.
        completeFile.delete();
        //Try moving file.  If we couldn't move the file, i.e., because
        //someone is previewing it or it's on a different volume, try copy
        //instead.  If that failed, notify user.
        if (!incompleteFile.renameTo(completeFile))
            if (! CommonUtils.copy(incompleteFile, completeFile))
                setState(COULDNT_MOVE_TO_LIBRARY);        
        //Add file to library.
        FileManager.instance().addFileIfShared(completeFile);  
        setState(COMPLETE);            
    }

    private void tryOneDownload(HTTPDownloader downloader, 
                                RemoteFileDesc rfd) {
        try {
            downloader.doDownload();
        } catch (IOException e) {
        } finally {
            System.out.println("    WORKER: terminating from "+rfd);
            synchronized (this) {
                dloaders.remove(downloader);
                this.notifyAll();
            }
        }
    }

    /** 
     * Removes and returns the RemoteFileDesc with the smallest estimated
     * remaining download time in filesLeft.  
     *     @requires !filesLeft.isEmpty()
     *     @modifies filesLeft
     */
    public RemoteFileDesc2 removeBest(List filesLeft) {
        //The best rfd found so far...
        RemoteFileDesc2 ret=null;
        //...with an estimated download time of "time" seconds.
        long lowestTime=Integer.MAX_VALUE;
        
        for (Iterator iter=filesLeft.iterator(); iter.hasNext(); ) {
            RemoteFileDesc2 rfd=(RemoteFileDesc2)iter.next();
            //The size of the incomplete file for this rfd.  If it doesn't
            //exist, the incompleteFile.length() returns 0, so amountLeft is
            //the full length of the file.
            File incompleteFile=incompleteFileManager.getFile(rfd);
            long amountLeft=rfd.getSize()-incompleteFile.length();
            //The speed of this connection in kiloBYTES/sec.
            long speed=rfd.getSpeed()/8;
            //The estimated time
            long estimatedTime=999999999;
            if (speed != 0)  // Stop dividing by zero bug.
                estimatedTime=amountLeft/speed;

            if (estimatedTime < lowestTime) {
                lowestTime=estimatedTime;
                ret=rfd;
            }
        }
            
        Assert.that(ret!=null, "Precondition to removeBest violated.");
        filesLeft.remove(ret);
        return ret;
    }

    private static boolean isPrivate(RemoteFileDesc rfd) {
        String host=rfd.getHost();
        int port=rfd.getPort();
        return (new Endpoint(host, port)).isPrivateAddress();
    }

    /** Removes all push requests made more than PUSH_INVALIDATE_TIME
     *  seconds ago from the requested list.
     *      @requires this monitor held
     *      @modifies requested */
    private void purgeOldPushRequests() {
        long expireTime=System.currentTimeMillis()-(PUSH_INVALIDATE_TIME*1000);
        Iterator iter=requested.iterator();
        while (iter.hasNext()) {
            RemoteFileDesc2 prf=(RemoteFileDesc2)iter.next();
            //Created before the expire time?
            if (prf.getCreationTime()<expireTime)
                iter.remove();
        }
    }



    /////////////////////////////   Display Variables ////////////////////////////

    /** Sets this' state, taking care of locking
     *      @requires newState one of the constants defined in Downloader
     *      @modifies this.state, this.stateTime */
    private void setState(int newState) {
        synchronized (this) {
            this.state=newState;
            this.stateTime=(new Date()).getTime();
        }
    }


    /***************************************************************************
     * Accessors that delegate to dloader. Synchronized because dloader can
     * change.
     ***************************************************************************/

    public synchronized int getState() {
        return state;
    }

    public synchronized int getRemainingStateTime() {
        long now=(new Date()).getTime();
        switch (state) {
        case CONNECTING:
            return timeDiff(now, CONNECT_TIME);
        case WAITING_FOR_RETRY:
            return timeDiff(now, calculateWaitTime());
        default:
            return Integer.MAX_VALUE;
        }
    }

    private int timeDiff(long nowTime, long stateLength) {
        long elapsed=nowTime-stateTime;
        long remaining=stateLength-elapsed;
        return (int)Math.max(remaining, 0)/1000;
    }
    
    //TODO: re-enable the folllowing fields

	public synchronized boolean chatEnabled() {
        return false;
//  		if (dloader == null)
//  			return false;
//  		else 
//  			return dloader.chatEnabled();
	}

    public synchronized String getFileName() {
//          if (dloader!=null)
//              return dloader.getFileName();
//          //If we're not actually downloading, we just pick some random value.
//          else if (allFiles.length>0)
        return allFiles[0].getFileName();
//          //The downloader is about to die, but respond anyway.
//          else
    }

    public synchronized int getContentLength() {
//          if (dloader!=null)
//              return dloader.getFileSize();
//          //If we're not actually downloading, we just pick some random value.
//          else if (allFiles.length>0)
        return allFiles[0].getSize();
//          elsereturn 0;
    }

    public synchronized int getAmountRead() {
//          if (dloader!=null)
//              return dloader.getAmountRead();
//          else
        int sum=0;
        for (Iterator iter=dloaders.iterator(); iter.hasNext(); )
            sum+=((HTTPDownloader)iter.next()).getAmountRead();
        return sum;
    }

    public synchronized String getHost() {
        return dloaders.size()+" locations";
    }

	public synchronized int getPort() {
//  		if (dloader != null)
//  			return dloader.getPort();
//  		else
			return 0;
	}

    public synchronized int getPushesWaiting() {
        return 0;
//          return pushFiles.size();
    }

    public synchronized int getRetriesWaiting() {
        return files.size();
    }
}


