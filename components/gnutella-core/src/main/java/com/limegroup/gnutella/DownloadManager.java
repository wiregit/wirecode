package com.limegroup.gnutella;

import com.limegroup.gnutella.downloader.ManagedDownloader;
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
 * in to the constructor in case there are circular dependencies.
 */
public class DownloadManager {
    /** The callback for notifying the GUI of major changes. */
    private ActivityCallback callback;
    /** The message router to use for pushes. */
    private MessageRouter router;
    /** Used for get addresses in pushes. */
    private Acceptor acceptor;

    /** The list of all ManagedDownloader's attempting to download.
     *  INVARIANT: active.size()<=slots() && active contains no duplicates */
    private List /* of ManagedDownloader */ active=new LinkedList();
    /** The list of all queued ManagedDownloader. 
     *  INVARIANT: waiting contains no duplicates */
    private List /* of ManagedDownloader */ waiting=new LinkedList();


    //////////////////////// Main Public Interface /////////////////////////

    /** 
     * Initializes this manager. <b>This method must be called before any other
     * methods are used.</b> 
     *     @param callback the UI callback to notify of download changes
     *     @param router the message router to use for sending push requests
     *     @param acceptor used to get my IP address and port for pushes
     */
    public void initialize(ActivityCallback callback,
                           MessageRouter router,
                           Acceptor acceptor) {
        this.callback=callback;
        this.router=router;
        this.acceptor=acceptor;
    }
                
    /** 
     * Tries to download the given file.  Returns a Downloader that allows you
     * to stop and resume this download.  The ActivityCallback will also be
     * notified of this download, so the return value can usually be ignored.
     * The download begins immediately, unless it is queued.  
     *     @modifies this, disk 
     */
    public synchronized Downloader getFile(RemoteFileDesc file) {
        return getFiles(new RemoteFileDesc[] { file });
    }

    /** 
     * Tries to "smart download" any of the given files.  Returns a Downloader
     * that allows you to stop and resume this download.  The ActivityCallback 
     * will also be notified of this download, so the return value can usually
     * be ignored.The download begins immediately, unless it is queued.  It
     * stops after any of the files succeeds.
     *      @modifies this, disk 
     */
    public synchronized Downloader getFiles(RemoteFileDesc[] files) {
        //Start download asynchronously.  This automatically moves downloader to
        //active if it can.
        ManagedDownloader downloader=new ManagedDownloader(this, files);
        waiting.add(downloader);
        callback.addDownload(downloader);
        return downloader;
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
        active.remove(downloader);
        waiting.add(downloader);
        notify();
    }

    /**
     * Removes downloader entirely from the list of current downloads.
     * Notifies callback of the change in status.
     *     @requires downloader active or queued
     *     @modifies this, callback
     */
    public synchronized void remove(ManagedDownloader downloader, boolean success) {
        boolean activated=active.remove(downloader);
        if (! activated)  //minor optimization.  Always safe to execute both paths.
            waiting.remove(downloader);
        else
            notify();
        callback.removeDownload(downloader);
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
