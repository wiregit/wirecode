package com.limegroup.gnutella;

import com.limegroup.gnutella.downloader.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;

/** 
 * The list of all downloads in progress.  DownloadManager has a fixed number of
 * download slots given by the MAX_SIM_DOWNLOADS property.  It is responsible
 * for starting downloads and scheduling and queing them as needed.  This
 * class is thread safe.<p>
 *
 * As with other classes in this package, a DownloadManager instance may not be
 * used until initialize(..) is called.  The arguments to this are not passed
 * in to the constructor in case there are circular dependencies.<p>
 *
 * DownloadManager provides ways to serialize download state to disk.  Reads are
 * initiated by RouterService, since we have to wait until the GUI is initiated.
 * Writes are initiated by this, since we need to be notified of completed
 * downloads.  Downloads in the COULDNT_DOWNLOAD state are not serialized.  
 */
public class DownloadManager {
    /** The callback for notifying the GUI of major changes. */
    private ActivityCallback callback;
    /** The message router to use for pushes. */
    private MessageRouter router;
    /** Used for get addresses in pushes. */
    private Acceptor acceptor;
    /** Used to check if the file exists. */
    private FileManager fileManager;

    /** The list of all ManagedDownloader's attempting to download.
     *  INVARIANT: active.size()<=slots() && active contains no duplicates */
    private List /* of ManagedDownloader */ active=new LinkedList();
    /** The list of all queued ManagedDownloader. 
     *  INVARIANT: waiting contains no duplicates */
    private List /* of ManagedDownloader */ waiting=new LinkedList();


    //////////////////////// Creation and Saving /////////////////////////

    /** 
     * Initializes this manager. <b>This method must be called before any other
     * methods are used.</b> 
     *     @param callback the UI callback to notify of download changes
     *     @param router the message router to use for sending push requests
     *     @param acceptor used to get my IP address and port for pushes
     *     @param fileManager used to check if files exist
     */
    public void initialize(ActivityCallback callback,
                           MessageRouter router,
                           Acceptor acceptor,
                           FileManager fileManager) {
        this.callback=callback;
        this.router=router;
        this.acceptor=acceptor;
        this.fileManager=fileManager;
    }

    /** Writes a snapshot of all downloaders in this to the file named
     *  DOWNLOAD_SNAPSHOT_FILE.  Returns true iff the file was successfully
     *  written. */
    private synchronized boolean writeSnapshot() {
        List buf=new ArrayList();
        buf.addAll(active);
        buf.addAll(waiting);

        try {
            ObjectOutputStream out=new ObjectOutputStream(
                new FileOutputStream(
                    SettingsManager.instance().getDownloadSnapshotFile()));
            out.writeObject(buf);
            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Reads the downloaders serialized in DOWNLOAD_SNAPSHOT_FILE and adds them
     *  to this, queued.  The queued downloads will restart immediately if slots
     *  are available.  Returns false iff the file could not be read for any
     *  reason. */
    public synchronized boolean readSnapshot() {
        //Read downloaders from disk.
        List buf=null;
        try {
            ObjectInputStream in=new ObjectInputStream(
                new FileInputStream(
                    SettingsManager.instance().getDownloadSnapshotFile()));
            buf=(List)in.readObject();
        } catch (IOException e) {
            return false;
        } catch (ClassCastException e) {
            return false;
        } catch (ClassNotFoundException e) {
            return false;
        }

        //Initialize and start downloaders.  Must catch ClassCastException since
        //the data could be corrupt.  This code is a little tricky.  It is
        //important that instruction (3) follow (1) and (2), because we must not
        //pass an uninitialized Downloader to the GUI.  (The call to getFileName
        //will throw NullPointerException.)  I believe the relative order of (1)
        //and (2) does not matter since this' monitor is held.  (The download
        //thread must obtain the monitor to acquire a queue slot.)
        try {
            for (Iterator iter=buf.iterator(); iter.hasNext(); ) {
                ManagedDownloader downloader=(ManagedDownloader)iter.next();
                waiting.add(downloader);             //1
                downloader.initialize(this);         //2
                callback.addDownload(downloader);    //3
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

     
    ////////////////////////// Main Public Interface ///////////////////////
           
    /** 
     * Tries to "smart download" any of the given files.<p>  
     *
     * If any of the files already being downloaded (or queued for downloaded)
     * has the same temporary name as any of the files in 'files', throws
     * AlreadyDownloadingException.  Note, however, that this doesn't guarantee
     * that a successfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==false, then if any of the files already exists in the
     * download directory, FileExistsException is thrown and no files are
     * modified.  If overwrite==true, the files may be overwritten.<p>
     * 
     * Otherwise returns a Downloader that allows you to stop and resume this
     * download.  The ActivityCallback will also be notified of this download,
     * so the return value can usually be ignored.  The download begins
     * immediately, unless it is queued.  It stops after any of the files
     * succeeds.
     *
     *     @modifies this, disk */
    public synchronized Downloader getFiles(RemoteFileDesc[] files,
                                            boolean overwrite) 
            throws FileExistsException, AlreadyDownloadingException {
        //Check if file would conflict with any other downloads in progress.
        //TODO3: if only a few of many files conflicts, we could just ignore
        //them.
        String conflict=conflicts(files, null);
        if (conflict!=null)
            throw new AlreadyDownloadingException(conflict);


        //Check if file exists.  TODO3: ideally we'd pass ALL conflicting files
        //to the GUI, so they know what they're overwriting.
        if (! overwrite) {
            File downloadDir = SettingsManager.instance().getSaveDirectory();
            for (int i=0; i<files.length; i++) {
                String filename=files[i].getFileName();
                File completeFile = new File(downloadDir, filename);  
                if ( completeFile.exists() ) 
                    throw new FileExistsException(filename);            
            }
        }

        //Start download asynchronously.  This automatically moves downloader to
        //active if it can.
        ManagedDownloader downloader=new ManagedDownloader(this, files);
        waiting.add(downloader);
        callback.addDownload(downloader);
        //Save this' state to disk for crash recovery.
        writeSnapshot();
        return downloader;
    }   
    
    /**
     * Returns the name of any of the files in 'files' conflict with any of the
     * downloads in this except for dloader, which may be null.  Returns null if
     * there are no conflicts.  This is used before starting and resuming
     * downloads.  
     */
    public synchronized String conflicts(RemoteFileDesc[] files,
                                         ManagedDownloader dloader) {
        for (int i=0; i<files.length; i++) {
            //Active downloads...
            for (Iterator iter=active.iterator(); iter.hasNext(); ) {
                ManagedDownloader md=(ManagedDownloader)iter.next();
                if (dloader!=null && md==dloader)
                    continue;
                if (md.conflicts(files[i]))                   
                    return files[i].getFileName();
            }
            //Queued downloads...
            for (Iterator iter=waiting.iterator(); iter.hasNext(); ) {
                ManagedDownloader md=(ManagedDownloader)iter.next();
                if (dloader!=null && md==dloader)
                    continue;
                if (md.conflicts(files[i]))
                    return files[i].getFileName();
            }
        }
        return null;
    }

    /**
     * Accepts the given socket for a push download to this host.
     * If the GIV is for a file that was never requested or has already
     * been downloaded, this will deal with it appropriately.  In any case
     * this eventually closes the socket.  Non-blocking.
     *     @modifies this
     *     @requires "GIV " was just read from s
     */
    public void acceptDownload(Socket socket) {
        try {
            //1. Read GIV line BEFORE acquiring lock, since this may block.
            GIVLine line=parseGIV(socket);
            String file=line.file;
            int index=line.index;
            byte[] clientGUID=line.clientGUID;

            //2. Attempt to give to an existing downloader.
            synchronized (this) {
                for (Iterator iter=active.iterator(); iter.hasNext();) {
                    ManagedDownloader md=(ManagedDownloader)iter.next();
                    if (md.acceptDownload(file, socket, index, clientGUID))
                        return;
                }
                for (Iterator iter=waiting.iterator(); iter.hasNext();) {
                    ManagedDownloader md=(ManagedDownloader)iter.next();
                    if (md.acceptDownload(file, socket, index, clientGUID))
                        return;
                }
            }
        } catch (IOException e) {
        }            

        //3. We never requested the file or already got it.  Kill it.
        try {
            socket.close();
        } catch (IOException e) { }
    }


    ////////////// Callback Methods for ManagedDownloaders ///////////////////

    private boolean hasFreeSlot() {
        SettingsManager settings=SettingsManager.instance();
        return active.size() < settings.getMaxSimDownload();
    }

    /** 
     * Blocks until a download slot has been assigned to downloader.  Throws
     * InterruptedException if the current thread is interrupted while waiting.
     * If InterruptedException is thrown, this is not modified.
     *     @requires downloader queued
     *     @modifies this 
     */
    public synchronized void waitForSlot(ManagedDownloader downloader) 
            throws InterruptedException {
        while (! hasFreeSlot()) 
            wait();
        waiting.remove(downloader);
        active.add(downloader);
    }

    /**
     * Relinquishes downloader's slot.  This is idempotent and non-blocking.
     *     @modifies this
     */
    public synchronized void yieldSlot(ManagedDownloader downloader) {
        Assert.that(downloader!=null, "Null downloader");
        Assert.that(active!=null, "Null active");
        Assert.that(waiting!=null, "Null waiting");
        active.remove(downloader);
        waiting.add(downloader);
        notify();
    }

    /**
     * Removes downloader entirely from the list of current downloads.
     * Notifies callback of the change in status.
     *     @modifies this, callback
     */
    public synchronized void remove(ManagedDownloader downloader,
                                    boolean success) {
        //As a minor optimization, only waiting.remove(..) or notify(..)
        //is needed.  But we do both just to be safe.
        active.remove(downloader);
        waiting.remove(downloader);
        notify();  
        callback.removeDownload(downloader);
        //Save this' state to disk for crash recovery.  Note that a downloader
        //in the GAVE_UP state is not serialized here even if still displayed in
        //the GUI.  Maybe this callback model needs a little tweaking.
        writeSnapshot();
    }

    /**
     * Sends a push request for the given file.  Returns false iff no push could
     * be sent, i.e., because no routing entry exists. That generally means you
     * shouldn't send any more pushes for this file.
     *     @modifies router 
     */
    public boolean sendPush(RemoteFileDesc file) {
        PushRequest pr=new PushRequest(GUID.makeGuid(),
                                       SettingsManager.instance().getTTL(),
                                       file.getClientGUID(),
                                       file.getIndex(),
                                       acceptor.getAddress(),
                                       acceptor.getPort());
        try {
            router.sendPushRequest(pr);
        } catch (IOException e) {
            return false;
        }
        return true;
    }


    /////////////////// Internal Method to Parse GIV String ///////////////////

    private static class GIVLine {
        String file;
        int index;
        byte[] clientGUID;
        GIVLine(String file, int index, byte[] clientGUID) {
            this.clientGUID=clientGUID;
            this.file=file;
            this.index=index;
            this.clientGUID=clientGUID;
        }
    }

    /** 
     * Returns the file, index, and client GUID from the GIV request from s.
     * The input stream of s is positioned just after the GIV request,
     * immediately before any HTTP.  If s is closed or the line couldn't
     * be parsed, throws IOException.
     *     @requires "GIV " just read from s
     *     @modifies s's input stream.
     */
    private static GIVLine parseGIV(Socket s) throws IOException {
        //1. Read  "GIV 0:BC1F6870696111D4A74D0001031AE043/sample.txt\n\n"
        String command;
        try {
            //We set the timeout now so we don't block reading
            //connection strings.  We reset it before actually downloading.
            s.setSoTimeout(SettingsManager.instance().getTimeout());
            //The try-catch below is a work-around for JDK bug 4091706.
            InputStream istream=null;
            try {
                istream = s.getInputStream();
            } catch (Exception e) {
                throw new IOException();
            }
            ByteReader br = new ByteReader(istream);
            command = br.readLine();      // read in the first line
            if (command==null)
                throw new IOException();
            String next=br.readLine();    // read in empty line
            if (next==null || (! next.equals(""))) {
                throw new IOException();
            }
        } catch (IOException e) {        
            throw e;                   
        }   

        //2. Parse and return the fields.
        try {            
            //a) Extract file index.  IndexOutOfBoundsException
            //   or NumberFormatExceptions will be thrown here if there's
            //   a problem.  They're caught below.
            int i=command.indexOf(":");
            int index=Integer.parseInt(command.substring(0,i));
            //b) Extract clientID.  This can throw
            //   IndexOutOfBoundsException or
            //   IllegalArgumentException, which is caught below.
            int j=command.indexOf("/", i);
            byte[] guid=GUID.fromHexString(command.substring(i+1,j));
            //c). Extract file name.
            String filename=command.substring(j+1);    

            return new GIVLine(filename, index, guid);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        } catch (NumberFormatException e) {
            throw new IOException();
        } catch (IllegalArgumentException e) {
            throw new IOException();
        }          
    }   
}
