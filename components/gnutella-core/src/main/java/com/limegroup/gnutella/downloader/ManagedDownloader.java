package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.xml.*;
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
 * as allowed by the Downloader specification.  This implementation provides
 * swarmed downloads, the ability to download copies of the same file from
 * multiple hosts.  See the accompanying white paper for details.<p>
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
    /** The place to share completed downloads (and their metadata) */
    private FileManager fileManager;
    /** The repository of incomplete files. */
    private IncompleteFileManager incompleteFileManager;
    /** The complete list of files passed to the constructor. */
    private RemoteFileDesc[] allFiles;

    ///////////////////////// Policy Controls ///////////////////////////
    /** The number of tries to make */
    private static final int TRIES=300;
    /** The max number of push tries to make, per host. */
    private static final int PUSH_TRIES=2;
    /** The time to wait trying to establish each connection, in milliseconds.*/
    private static final int CONNECT_TIME=4000;  //8 seconds
    /** The maximum time, in SECONDS, allowed between a push request and an
     *  incoming push connection. */
    private static final int PUSH_INVALIDATE_TIME=5*60;  //5 minutes
    /** The smallest interval that can be split for parallel download */
    private static final int MIN_SPLIT_SIZE=100000;      //100 KB        
    /** The minimum quality considered likely to work.  Value of two corresponds
     *  to a THREE-star result. */
    private static final int DECENT_QUALITY=2;


    ////////////////////////// Core Variables /////////////////////////////
    /** If started, the thread trying to coordinate all downloads.  
     *  Otherwise null. */
    private Thread dloaderManagerThread;
    /** The connections we're using for the current attempts. */    
    private List /* of HTTPDownloader */ dloaders;
    /** True iff this has been forcibly stopped. */
    private boolean stopped;
    
    /** The lock for pushes (see below).  Used intead of THIS to prevent missing
     *  notify's.  See readObject for note on serialization. */
    private Object pushLock=new Object();
    /** The name of the push file we are waiting for, or NULL if none */
    private String pushFile;        
    /** The client GUID of the push we are waiting for, if any */
    private byte[] pushClientGUID=null;
    /** The index of the push file we are waiting for, if any */
    private long pushIndex;    
    /** The socket for the pending push download.  Used to communicate between
     *  the Acceptor thread and the manager thread. */
    private Socket pushSocket;

    /** For implementing the BandwidthTracker interface. */
    private int _oldAmountRead = 0;

    ///////////////////////// Variables for GUI Display  /////////////////
    /** The current state.  One of Downloader.CONNECTING, Downloader.ERROR,
      *  etc.   Should be modified only through setState. */
    private int state;
    /** The time as returned by Date.getTime() that this entered the current
        state.  Should be modified only through setState. */
    private long stateTime;
    /** If in the wait state, the number of retries we're waiting for.
     *  Otherwise undefined. */
    private int retriesWaiting;
    /** The name of the last file being attempted.  Needed only to support
     *  launch(). */
    private String currentFileName;
    /** The size of the last file being attempted.  Needed only to support
     *  launch(). */
    private int currentFileSize;
    /** The name of the last location we tried to connect to. (We may be
     *  downloading from multiple other locations. */
    private String currentLocation;

    /**
     * Creates a new ManagedDownload to download the given files.  The download
     * attempts to begin immediately; there is no need to call initialize.
     * Non-blocking.
     *     @param manager the delegate for queueing purposes.  Also the callback
     *      for changes in state.
     *     @param files the list of files to get.  This stops after ANY of the
     *      files is downloaded.
     *     @param incompleteFileManager the repository of incomplete files for
     *      resuming
     */
    public ManagedDownloader(DownloadManager manager,
                             RemoteFileDesc[] files,
                             FileManager fileManager,
                             IncompleteFileManager incompleteFileManager) {
        this.allFiles=files;
        this.fileManager=fileManager;
        this.incompleteFileManager=incompleteFileManager;
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
        //The following is needed to prevent NullPointerException when reading
        //serialized object from disk.  This can't be done in the constructor or
        //initializer statements, as they're not executed.  Nor should it be
        //done in initialize, as that could cause problems in resume().
        pushLock=new Object(); 
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
        dloaders=new LinkedList();
        stopped=false;
        setState(QUEUED);
            
        this.dloaderManagerThread=new Thread() {
            public void run() {
                tryAllDownloads();
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
            //TODO3: this is stricter than necessary.  What if a location has
            //been removed?  Tricky without global variables.  At the least we
            //should return false if in COULDNT_DOWNLOAD state.
            for (int i=0; i<allFiles.length; i++) {
                RemoteFileDesc rfd=(RemoteFileDesc)allFiles[i];
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
        synchronized (pushLock) {
            if (pushFile==null
                   || !pushFile.equals(file)
                   || pushIndex!=index
                   || !Arrays.equals(pushClientGUID, clientGUID))
                //Not intended for me.
                return false;
            else {
                //Intended for me.
                this.pushSocket=socket;
                pushLock.notify();
                return true;
            }
        }
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
        if (! (state==WAITING_FOR_RETRY || state==GAVE_UP || state==ABORTED))
            return false;
        //Sometimes a resume can cause a conflict.  So we check.
        String conflict=this.manager.conflicts(allFiles, this);
        if (conflict!=null)
            throw new AlreadyDownloadingException(conflict);

        if (state==GAVE_UP || state==ABORTED) {
            //This stopped because all hosts were tried.  (Note that this
            //couldn't have been user aborted.)  Therefore no threads are
            //running in this and it may be safely resumed.
            initialize(this.manager);
        } else if (state==WAITING_FOR_RETRY) {
            //Interrupt any waits.
            if (dloaderManagerThread!=null)
                dloaderManagerThread.interrupt();
        }
        return true;
    }

    public File getDownloadFragment() {
        //We haven't started yet.
        if (currentFileName==null)
            return null;

        //a) If the file is being downloaded, create *copy* of first
        //block of incomplete file.  The copy is needed because some
        //programs, notably Windows Media Player, attempt to grab
        //exclusive file locks.  If the download hasn't started, the
        //incomplete file may not even exist--not a problem.
        if (state!=COMPLETE) {
            File incomplete=incompleteFileManager.
                               getFile(currentFileName, currentFileSize); 
            File file=new File(incomplete.getParent(),
                               IncompleteFileManager.PREVIEW_PREFIX
                                   +incomplete.getName());
            //Get the size of the first block of the file.  (Remember
            //that swarmed downloads don't always write in order.)
            int size=amountForPreview(incomplete);
            if (size<=0)
                return null;
            //Copy first block, returning if nothing was copied.
            if (CommonUtils.copy(incomplete, size, file)<=0) 
                return null;
            return file;
        }
        //b) Otherwise, choose completed file.
        else {
            File saveDir = null;
            try {
                saveDir = SettingsManager.instance().getSaveDirectory();
            } catch(java.io.FileNotFoundException fnfe) {
                // simply return if we could not get the save directory.
                return null;
            }
            return new File(saveDir,currentFileName);     
        }
    }


    /** 
     * Returns the amount of the file written on disk that can be safely
     * previewed. 
     * 
     * @param incompleteFile the file to examine, which MUST correspond to
     *  the current download.
     */
    private synchronized int amountForPreview(File incompleteFile) {
        int last=0;
        //This is tricky. First we search if a previous downloader wrote a block
        //starting at byte zero.     
        for (Iterator iter=incompleteFileManager.getBlocks(incompleteFile); 
                iter.hasNext() ; ) {
            Interval interval=(Interval)iter.next();
            if (interval.low==0) {
                last=interval.high;
                break;
            }
        }

        //Now we search for a downloader starting at the ending place of the
        //block we found above (which may be zero).
        for (Iterator iter=dloaders.iterator(); iter.hasNext(); ) {
            HTTPDownloader dloader=(HTTPDownloader)iter.next();
            if (dloader.getInitialReadingPoint()==last)
                return last+dloader.getAmountRead();
        }
        return last;
    }


    ///////////////////////////// Core Downloading Logic ///////////////////////

    /** 
     * Actually does the download, finding duplicate files, trying all
     * locations, resuming, waiting, and retrying as necessary.  Also takes care
     * of moving file from incomplete directory to save directory and adding
     * file to the library.  Called from dloadManagerThread.  
     */
    private void tryAllDownloads() {     
        List[] /* of File */ buckets=bucket(allFiles, incompleteFileManager);
        //System.out.println("Buckets: "+Arrays.asList(buckets).toString());

        //While not success and still busy...
        while (true) {
            try {
                //Try each group, returning on success.  The children of
                //tryAllDownloads call setState(CONNECTING) and
                //setState(DOWNLOADING) as appropriate.
                setState(QUEUED);
                manager.waitForSlot(this);
                boolean waitForRetry=false;
                for (int i=0; i<buckets.length; i++) {    
                    if (buckets[i].size() <= 0)
                        continue;
                    synchronized (this) {
                        RemoteFileDesc rfd=(RemoteFileDesc)buckets[i].get(0);
                        currentFileName=rfd.getFileName();
                        currentFileSize=rfd.getSize();
                    }
                    int status=tryAllDownloads2(buckets[i]);
                    if (status==COMPLETE) {
                        //Success! 
                        setState(COMPLETE);
                        manager.remove(this, true);
                        return;
                    } else if (status==COULDNT_MOVE_TO_LIBRARY) {
                        setState(COULDNT_MOVE_TO_LIBRARY);
                        manager.remove(this, false);
                        return;
                    } else if (status==WAITING_FOR_RETRY) {
                        waitForRetry=true;
                    } else {
                        Assert.that(status==GAVE_UP,
                                    "Bad status from tad2: "+status);
                    }                    
                }
                manager.yieldSlot(this);

                //Wait or abort.
                if (waitForRetry) {
                    synchronized (this) {
                        retriesWaiting=0;
                        for (int i=0; i<buckets.length; i++)
                            retriesWaiting+=buckets[i].size();
                    }
                    setState(WAITING_FOR_RETRY);
                    Thread.sleep(calculateWaitTime());
                } else {
                    setState(GAVE_UP);
                    manager.remove(this, false);
                    return;
                }
            } catch (InterruptedException e) {
                if (stopped) {
                    setState(ABORTED);
                    manager.remove(this, false);
                    return;
                }
            }
        }                 
    }

    /** Returns the amount of time to wait in milliseconds before retrying,
     *  based on tries.  This is also the time to wait for * incoming pushes to
     *  arrive, so it must not be too small.  A value of * tries==0 represents
     *  the first try.  */
    private long calculateWaitTime() {
        //60 seconds: same as BearShare.
        return 60*1000;
    }


    /**
     * Tries one round of downloading of the given files.  Downloads from all
     * locations until all locations fail or some locations succeed.  Moves
     * incomplete file to the library on success.
     * 
     * @param files a list of files to pick from, all of which MUST be
     *  "identical" instances of RemoteFileDesc.  Unreachable locations
     *  are removed from files.
     * @return COMPLETE if a file was successfully downloaded
     *         COULDNT_MOVE_TO_LIBRARY the download completed but the
     *             temporary file couldn't be moved to the library
     *         WAITING_FOR_RETRY if no file was downloaded, but it makes sense 
     *             to try again later because some hosts reported busy.
     *             The caller should usually wait before retrying.
     *         GAVE_UP the download attempt failed, and there are 
     *             no more locations to try.
     * @exception InterruptedException if the user stop()'ed this download. 
     *  (Calls to resume() do not result in InterruptedException.)
     */
    private int tryAllDownloads2(final List /* of RemoteFileDesc */ files)
            throws InterruptedException {
        if (files.size()==0)
            return GAVE_UP;

        //1. Verify it's safe to download.  Filename must not have "..", "/",
        //   etc.  We check this by looking where the downloaded file will
        //   end up.
        RemoteFileDesc rfd=(RemoteFileDesc)files.get(0);
        int fileSize=rfd.getSize(); 
        String filename=rfd.getFileName(); 
        File incompleteFile=incompleteFileManager.getFile(rfd);
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
            return COULDNT_MOVE_TO_LIBRARY;
        }           

        //2. Do the download
        int status=tryAllDownloads3(files);
        if (status!=COMPLETE)
            return status;
                
        //3. Move to library.  
        //System.out.println("MANAGER: completed");
        //Delete target.  If target doesn't exist, this will fail silently.
        completeFile.delete();
        //Try moving file.  If we couldn't move the file, i.e., because
        //someone is previewing it or it's on a different volume, try copy
        //instead.  If that failed, notify user.
        if (!incompleteFile.renameTo(completeFile))
            if (! CommonUtils.copy(incompleteFile, completeFile))
                return COULDNT_MOVE_TO_LIBRARY;
        //Add file to library.
        fileManager.addFileIfShared(completeFile, getXMLDocuments());  
        return COMPLETE;
    }   


    /** Like tryDownloads2, but does not deal with the library and hence
     *  cannot return COULDNT_MOVE_TO_LIBRARY.  Also requires that
     *  files.size()>0. */
    private int tryAllDownloads3(final List /* of RemoteFileDesc */ files) 
            throws InterruptedException {
        //The parts of the file we still need to download.
        //INVARIANT: all intervals are disjoint and non-empty
        List /* of Interval */ needed=new ArrayList(); {
            RemoteFileDesc rfd=(RemoteFileDesc)files.get(0);
            File incompleteFile=incompleteFileManager.getFile(rfd);
            Iterator iter=incompleteFileManager.
                 getFreeBlocks(incompleteFile, rfd.getSize());
            while (iter.hasNext()) 
                needed.add((Interval)iter.next());
        }

        //The locations that were busy, for trying later.
        List /* of RemoteFileDesc2 */ busy=new LinkedList();
        //The downloaders that finished, either normally or abnormally.
        List /* of HTTPDownloader */ terminated=new LinkedList();

        //While there is still an unfinished region of the file...
        while (true) {
            synchronized (this) {
                if (dloaders.size()==0 && needed.size()==0) {
                    //Finished.
                    return COMPLETE;
                } else if (dloaders.size()==0 
                        && files.size()==0 
                        && terminated.size()==0) {
                    //No downloaders worth living for.
                    if (busy.size()>0) {
                        files.addAll(busy);
                        return WAITING_FOR_RETRY;
                    } else {
                        return GAVE_UP;
                    }
                }
            }                        

            //1. Try dividing remaining work among PARALLEL_DOWNLOAD-|dloaders|
            //locations, so that that desired parallelism will be reached.
            while (true) {
                if (! allowAnotherDownload())
                    break;
                try {
                    startBestDownload(files, needed,
                                      busy, terminated);
                } catch (NoSuchElementException e) {
                    break;
                }
            }
        
            //2. Wait for them to finish.
            //System.out.println("MANAGER: waiting for complete");
            synchronized (this) {
                if (stopped) throw new InterruptedException();
                while (terminated.size()==0 && dloaders.size()!=0) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        if (stopped) throw e;
                    }
                }
                for (Iterator iter=terminated.iterator(); iter.hasNext(); ) {
                    HTTPDownloader dloader=(HTTPDownloader)iter.next();
                    int init=dloader.getInitialReadingPoint();
                    Interval interval=new Interval(
                        init+dloader.getAmountRead(),
                        init+dloader.getAmountToRead());
                    if ((interval.high-interval.low) > 0)
                        needed.add(interval);
                }
                terminated.clear();
                if (stopped) throw new InterruptedException();
            }
        }
    }

    /** Returns true if another downloader is allowed. */
    private synchronized boolean allowAnotherDownload() {
        //TODO1: this should really be done dynamically by observing capacity
        //and load, but that's hard to do.
        int downloads=dloaders.size();
        int capacity=SettingsManager.instance().getConnectionSpeed();
        if (capacity<=SpeedConstants.MODEM_SPEED_INT)
            //Modems can't swarm.
            return downloads<1;
        else if (capacity<=SpeedConstants.T1_SPEED_INT)
            //DSL, Cable, and "T1" can swarm from up to 4 locations.
            return downloads<4;
        else 
            return downloads<6;
    }

    /** 
     * Increases parallelism by assigning a block (possibly from another
     * downloader) to a new downloader.
     * 
     * @param files a list of files to pick from, all of which MUST be
     *  "identical" instances of RemoteFileDesc.  Elements are removed
     *  from this as they are tried.
     * @param needed a list of Intervals needed, possibly empty.  Used
     *  to construct range headers.
     * @param busy a list where busy hosts should be added during 
     *  connection attempts
     * @param terminated a list where a host should be added when a 
     *  download completes.  
     * @exception NoSuchElementException no locations could connect
     * @exception InterruptedException this thread was interrupted while
     *  waiting to connect
     */
    private void startBestDownload(final List /* of RemoteFileDesc */ files,
                                   final List /* of Interval */ needed,
                                   final List /* of RemoteFileDesc */ busy,
                                   final List /* of HTTPDownloader*/ terminated)
            throws NoSuchElementException, InterruptedException {        
        //1. Create downloader according to region to download.  Note that the
        //downloader requests until the end of the file (second arg to
        //findConnectable) though it secretly plans on terminating sooner
        //(stopAt(..)).
        HTTPDownloader dloader;
        if (needed.size()>0) {
            //Assign "white" (unclaimed) interval to new downloader.
            //TODO2: choose biggest, earliest, etc.
            //TODO2: assign to existing downloader if possible, without
            //      increasing parallelism
            Interval interval=(Interval)needed.remove(0);
            try {
                dloader=findConnectable(files, 
                                        interval.low, interval.high,
                                        busy);
            } catch (NoSuchElementException e) {
                //Need to re-add the interval.  If there is an existing
                //downloader, it will be reassigned to this later.
                needed.add(interval);
                throw e;
            }
            dloader.stopAt(interval.high);
            //System.out.println("MANAGER: assigning white "
            //                   +interval+" to "+dloader);
        }
        else {
            //Split largest "gray" interval, i.e., steal part of another
            //downloader's region for a new downloader.  
            //TODO3: split interval into P-|dloaders|, etc., not just half
            //TODO3: account for speed
            //TODO3: there is a minor race condition where biggest and 
            //      dloader could write to the same region of the file
            //      I think it's ok, though it could result in >100% in the GUI
            HTTPDownloader biggest=null;
            synchronized (this) {
                for (Iterator iter=dloaders.iterator(); iter.hasNext();) {
                    HTTPDownloader h=(HTTPDownloader)iter.next();
                    if (biggest==null 
                           || h.getAmountToRead()>biggest.getAmountToRead())
                        biggest=h;
                }                
            }
            if (biggest==null)
                throw new NoSuchElementException();
            //Note that getAmountToRead() and getInitialReadingPoint() are
            //constant.  getAmountRead() is not, so we "capture" it into a
            //variable.
            int amountRead=biggest.getAmountRead();
            int left=biggest.getAmountToRead()-amountRead;;
            if (left < MIN_SPLIT_SIZE)
                throw new NoSuchElementException();
            int start=biggest.getInitialReadingPoint()+amountRead+left/2;
            int stop=biggest.getInitialReadingPoint()+biggest.getAmountToRead();
            dloader=findConnectable(files, start, stop, busy);
            dloader.stopAt(stop);
            biggest.stopAt(start);
            //System.out.println("MANAGER: assigning grey "+start
            //                    +"-"+stop+" to "+dloader);
        }
                
        //2) Asynchronously do download
        //System.out.println("MANAGER: downloading from "
        //     +dloader.getInitialReadingPoint()+" to "+
        //     +(dloader.getInitialReadingPoint()+dloader.getAmountToRead()));
        setState(DOWNLOADING);
        synchronized (this) {
            if (stopped)
                throw new InterruptedException();
            dloaders.add(dloader);
        }
        final HTTPDownloader dloaderAlias=dloader;
        Thread worker=new Thread() {
                public void run() {
                    tryOneDownload(dloaderAlias, files, terminated);
                }
            };
        worker.setDaemon(true);
        worker.start();
    }

    /** 
     * Returns an initialized connectable downloader from the given list
     * of locations.
     *
     * @param files a list of files to pick from, all of which MUST be
     *  "identical" instances of RemoteFileDesc.  Elements are removed
     *  from this as they are tried.
     * @param start the starting byte offset of the download
     * @param stop the ending byte offset of the download plus one
     * @param busy a list where busy hosts should be added during 
     *  connection attempts
     * @exception NoSuchElementException no locations could connect
     * @exception InterruptedException this thread was interrupted while
     *  waiting to connect
     */
    private HTTPDownloader findConnectable(List /* of RemoteFileDesc */ files,
                                           int start, int stop,
                                           List busy) 
            throws NoSuchElementException, InterruptedException {        
        while (true) {
            if (files.size()==0)
                throw new NoSuchElementException();
            if (stopped)
                throw new InterruptedException();

            RemoteFileDesc rfd=removeBest(files);      
            File incompleteFile=incompleteFileManager.getFile(rfd);
            HTTPDownloader ret;
            synchronized (this) {
                currentLocation=rfd.getHost();
                //If we're just increasing parallelism, stay in DOWNLOADING
                //state.  Otherwise the following call is needed to restart
                //the timer.
                if (dloaders.size()==0)
                    setState(CONNECTING);
            }
            if (needsPush(rfd)) {
                //System.out.println("MANAGER: trying push to "+rfd);
                //Send push message, wait for response with timeout.
                synchronized (pushLock) {
                    manager.sendPush(rfd);
                    pushFile=rfd.getFileName();
                    pushIndex=rfd.getIndex();
                    pushClientGUID=rfd.getClientGUID();
                    
                    //No loop is actually needed here, assuming spurious
                    //notify()'s don't occur.  (They are not allowed by the Java
                    //Language Specifications.)  Look at acceptDownload for
                    //details.
                    pushLock.wait(CONNECT_TIME);  
                    if (pushSocket==null)
                        continue;

                    pushFile=null;   //Won't accept the push after timeout.
                    pushIndex=0;
                    pushClientGUID=null;
                    ret=new HTTPDownloader(pushSocket, rfd, incompleteFile,
                                           start, stop);
                    pushSocket=null;
                }
            } else {             
                //Establish normal downloader.
                ret=new HTTPDownloader(rfd, incompleteFile, start, stop);
                //System.out.println("MANAGER: trying connect to "+rfd);
            }

            try {
                ret.connect(CONNECT_TIME);
                return ret;
            } catch (TryAgainLaterException e) {
                //Add this for retry later
                busy.add(rfd);
            } catch (CantConnectException e) {
                //Schedule for pushing
                files.add(new RemoteFileDesc2(rfd, true));
            } catch (IOException e) {
                //Miscellaneous error: never revisit.
            }
        }
    }

    /**
     * Attempts to run downloader.doDownload, notifying manager of termination
     * via downloaders.notify().  Typically tryOneDownload is invoked in
     * parallel for a number of locations.
     * 
     * @param downloader the normal or push downloader to use for the transfer,
     *  which MUST be initialized (i.e., downloader.connect() has been called)
     * @param files the list of files where downloader's RemoteFileDesc should
     *  be added when the download is terminated, so that this location can
     *  be reused if needed
     * @param terminated the list to which downloader should be added on
     *  termination
     */
    private void tryOneDownload(HTTPDownloader downloader, 
                                List /* of RemoteFileDesc */ files,
                                List /* of HTTPDownloader */ terminated) {
        try {
            downloader.doDownload();
        } catch (IOException e) {
        } finally {
            //int stop=downloader.getInitialReadingPoint()
            //            +downloader.getAmountRead();
            //System.out.println("    WORKER: terminating from "+downloader
            //                   +" at "+stop);
            //In order to reuse this location again, we need to know the
            //RemoteFileDesc.  TODO2: use measured speed if possible.
            RemoteFileDesc rfd=downloader.getRemoteFileDesc();
            synchronized (this) {
                dloaders.remove(downloader);
                terminated.add(downloader);
                files.add(rfd);
                int init=downloader.getInitialReadingPoint();
                incompleteFileManager.addBlock(
                    incompleteFileManager.getFile(rfd),
                    init,
                    init+downloader.getAmountRead());
                this.notifyAll();
            }
        }
    }

    /** 
     * Groups the elements of allFiles into buckets of similar files. 
     *
     * @return an array of List of File.  For all i, for all elements
     *  f1 and f2 of returned[i], f1 and f2 are the "same" file (with
     *  high probability).  This means that 
     *     incompleteFileManager.getFile(f1)==incompleteFileManager.get(f2).
     *  Furthermore the elements of the returned array are sorted by estimated 
     *  download time, though the elements within each list are not.
     */
    private static List[] /* of File */ bucket(
            RemoteFileDesc[] rfds,
            IncompleteFileManager incompleteFileManager) {
        //Bucket the requested files.  TODO3: a TreeMap would use less memory.
        Map /* File -> List<RemoteFileDesc> */ buckets=new HashMap();         
        for (int i=0; i<rfds.length; i++) {
            RemoteFileDesc rfd=new RemoteFileDesc2(rfds[i], false);
            File incompleteFile=incompleteFileManager.getFile(rfd);
            List siblings=(List)buckets.get(incompleteFile);
            if (siblings==null) {
                siblings=new ArrayList();
                buckets.put(incompleteFile, siblings);
            }
            siblings.add(rfd);
        }
        //Now for each file, estimate remaining download time.  This assumes
        //that we'll be able to download a file from all (only) three and
        //four-star locations in parallel at exactly the advertised speed.  Fat
        //chance that will happen, but it's probably a good enough heuristic.
        //Still, we may want to preference buckets with more quality loctions
        //even if the total bandwidth is lower.
        FilePair[] pairs=new FilePair[buckets.keySet().size()];
        int i=0;
        for (Iterator iter=buckets.keySet().iterator(); iter.hasNext(); i++) {
            File incompleteFile=(File)iter.next();
            List /* of RemoteFileDesc */ files=
                (List)buckets.get(incompleteFile);
            int size=((RemoteFileDesc)files.get(0)).getSize()
                        - incompleteFileManager.getBlockSize(incompleteFile);
            int bandwidth=1; //prevent divide by zero
            for (Iterator iter2=files.iterator(); iter2.hasNext(); ) {
                RemoteFileDesc2 rfd2=(RemoteFileDesc2)iter2.next();
                if (rfd2.getQuality()>=DECENT_QUALITY)
                    bandwidth+=rfd2.getSpeed();
            }
            float time=(float)size/(float)bandwidth;
            pairs[i]=new FilePair(incompleteFile, time);
        }
        //Sort by download time and copy corresponding lists of files to new
        //array.
        Arrays.sort(pairs);
        List[] /* of File */ ret=new List[pairs.length];
        for (i=0; i<pairs.length; i++)
            ret[i]=(List)buckets.get(pairs[i].file);
        return ret;
    }

    private static class FilePair
            implements com.sun.java.util.collections.Comparable {
        float time;     //TODO: does this work with 1.1.8?
        File file;

        public FilePair(File file, float time) {
            this.time=time;
            this.file=file;
        }

        public int compareTo(Object o) {
            float diff=this.time-((FilePair)o).time;
            if (diff<0)
                return -1;
            else if (diff>0)
                return 1;
            else
                return 0;
        }
    }

    /** 
     * Removes and returns the RemoteFileDesc with the highest quality in
     * filesLeft.  If two or more entries have the same quality, returns the
     * entry with the highest speed.  
     *
     * @param filesLeft the list of file/locations to choose from, which MUST
     *  have length of at least one.  Each entry MUST be an instance of
     *  RemoteFileDesc.  The assumption is that all are "same", though this
     *  isn't strictly needed.
     * @return the best file/endpoint location 
     */
    public static RemoteFileDesc removeBest(List filesLeft) {
        Iterator iter=filesLeft.iterator();
        //The best rfd found so far
        RemoteFileDesc ret=(RemoteFileDesc)iter.next();
        
        //Find max of each (remaining) ret...
        while (iter.hasNext()) {
            RemoteFileDesc rfd=(RemoteFileDesc)iter.next();
            if (rfd.getQuality() > ret.getQuality())
                ret=rfd;
            else if (rfd.getQuality() == ret.getQuality()) {
                if (rfd.getSpeed() > ret.getSpeed())
                    ret=rfd;
            }            
        }
            
        filesLeft.remove(ret);
        return ret;
    }

    /** Returns true iff rfd should be attempted by push download, either 
     *  because it is a private address or was unreachable in the past. */
    private static boolean needsPush(RemoteFileDesc rfd) {
        String host=rfd.getHost();
        int port=rfd.getPort();
        //Return true if rfd is private or unreachable
        if ((new Endpoint(host, port)).isPrivateAddress())
            return true;
        else if (rfd instanceof RemoteFileDesc2)
            return ((RemoteFileDesc2)rfd).isUnreachable();
        else
            return false;
    }


    /**
     * Returns the union of all XML metadata documents from all hosts.
     */
    private LimeXMLDocument[] getXMLDocuments() {
        //TODO: we don't actually union here.  Also, should we only consider
        //those locations that we download from?  How about only those in this
        //bucket?
        LimeXMLDocument[] retArray = null;
        ArrayList allDocs = new ArrayList();

        // get all docs possible
        for (int i = 0; i < this.allFiles.length; i++) {
            if (this.allFiles[i] != null) {
                retArray = this.allFiles[i].getXMLDocs();
                for (int j = 0;
                     (retArray != null) && (j < retArray.length);
                     j++)
                    allDocs.add(retArray[j]);
            }
        }

        if (allDocs.size() > 0) {
            retArray = new LimeXMLDocument[allDocs.size()];
            for (int i = 0; i < retArray.length; i++)
                retArray[i] = (LimeXMLDocument) allDocs.get(i);
        }
        else
            retArray = null;

        return retArray;
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


    /*************************************************************************
     * Accessors that delegate to dloader. Synchronized because dloader can
     * change.
     *************************************************************************/

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
    
    public synchronized String getFileName() {        
        //If we're not actually downloading, we just pick some random value.
        if (dloaders.size()==0)
            return allFiles[0].getFileName();
        else 
            //Could also use currentFileName, but this works.
            return ((HTTPDownloader)dloaders.get(0))
                      .getRemoteFileDesc().getFileName();
    }

    public synchronized int getContentLength() {
        //If we're not actually downloading, we just pick some random value.
        //TODO: this can also mean we've FINISHED the download.  Luckily it
        //doesn't really matter.
        if (dloaders.size()==0)
            return allFiles[0].getSize();
        else 
            //Could also use currentFileSize, but this works.
            return ((HTTPDownloader)dloaders.get(0))
                      .getRemoteFileDesc().getSize();
    }

    public synchronized int getAmountRead() {
        //If we're not actually downloading, we just pick some random value.
        //TODO: this can also mean we've FINISHED the download.  Luckily it
        //doesn't really matter.
        if (dloaders.size()==0)
            return 0;
        else {
            RemoteFileDesc rfd=((HTTPDownloader)dloaders.get(0))
                                    .getRemoteFileDesc();
            File incompleteFile=incompleteFileManager.getFile(rfd);
            //Add up all stuff already on disk...
            int sum=incompleteFileManager.getBlockSize(incompleteFile);
            //...and all downloads in progress.
            for (Iterator iter=dloaders.iterator(); iter.hasNext(); )
                sum+=((HTTPDownloader)iter.next()).getAmountRead();
            return sum;
        }
    }
     
    public String getAddress() {
        return currentLocation;
    }
                                 
    public synchronized Iterator /* of Endpoint */ getHosts() {
        return getHosts(false);
    }
   
    public synchronized Iterator /* of Endpoint */ getChattableHosts() {
        return getHosts(true);
    }

    private final Iterator getHosts(boolean chattableOnly) {
        List /* of Endpoint */ buf=new LinkedList();
        for (Iterator iter=dloaders.iterator(); iter.hasNext(); ) {
            HTTPDownloader dloader=(HTTPDownloader)iter.next();            
            if (chattableOnly ? dloader.chatEnabled() : true) {                
                buf.add(new Endpoint(dloader.getInetAddress().getHostAddress(),
                                     dloader.getPort()));
            }
        }
        return buf.iterator();
    }

    public synchronized int getRetriesWaiting() {
        return retriesWaiting;
    }

    /**
     * Implements the <tt>BandwidthTracker</tt> interface.
     * Returns the number of bytes read since the last time this method
     * was called.
     *
     * @return the number of new bytes read since the last time this method
     *         was called
     */
    public synchronized int getNewBytesTransferred() {
        int newAmountRead = getAmountRead();
        int newBytesTransferred = newAmountRead - _oldAmountRead;
        _oldAmountRead = newAmountRead;
        return newBytesTransferred;
    }
    
    /*
    public static void main(String args[]) {
        //Test bucketing.  Note that the 1-star result is ignored.
        System.out.println("Unit test");
        IncompleteFileManager ifm=new IncompleteFileManager();
        RemoteFileDesc rf1=new RemoteFileDesc(
            "1.2.3.4", 6346, 0, "some file.txt", 1000, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3);
        RemoteFileDesc rf2=new RemoteFileDesc(
            "1.2.3.5", 6346, 0, "some file.txt", 1000, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3);
        RemoteFileDesc rf3=new RemoteFileDesc(
            "1.2.3.6", 6346, 0, "some file.txt", 1010, 
            new byte[16], SpeedConstants.T1_SPEED_INT, false, 3);
        RemoteFileDesc rf4=new RemoteFileDesc(
            "1.2.3.6", 6346, 0, "some file.txt", 1010, 
            new byte[16], SpeedConstants.T3_SPEED_INT, false, 0);
        

        //Simple case
        RemoteFileDesc[] allFiles={rf3, rf2, rf1, rf4};
        List[] files=bucket(allFiles, ifm);
        Assert.that(files.length==2);
        List list=files[0];
        Assert.that(list.size()==2);
        Assert.that(list.contains(rf1));
        Assert.that(list.contains(rf2));
        list=files[1];
        Assert.that(list.size()==2);
        Assert.that(list.contains(rf3));
        Assert.that(list.contains(rf4));

        //Large part written on disk
        ifm.addBlock(ifm.getFile(rf3), 0, 1009);
        files=bucket(allFiles, ifm);
        Assert.that(files.length==2);
        list=files[0];
        Assert.that(list.size()==2);
        Assert.that(list.contains(rf3));
        Assert.that(list.contains(rf4));
        list=files[1];
        Assert.that(list.size()==2);
        Assert.that(list.contains(rf1));
        Assert.that(list.contains(rf2));

        //Test removeBest
        RemoteFileDesc rf5=new RemoteFileDesc(
            "1.2.3.6", 6346, 0, "some file.txt", 1010, 
            new byte[16], SpeedConstants.T3_SPEED_INT+1, false, 0);
        list=new LinkedList();
        list.add(rf4);
        list.add(rf1);
        list.add(rf5);
        Assert.that(removeBest(list)==rf1);  //quality over speed
        Assert.that(list.size()==2);
        Assert.that(list.contains(rf4));
        Assert.that(list.contains(rf5));
        Assert.that(removeBest(list)==rf5);  
        Assert.that(list.size()==1);
        Assert.that(list.contains(rf4));
        Assert.that(removeBest(list)==rf4);  
        Assert.that(list.size()==0);
    }
    */
}
