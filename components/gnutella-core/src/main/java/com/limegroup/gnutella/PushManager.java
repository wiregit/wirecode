package com.limegroup.gnutella;

import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.udpconnect.UDPConnection;

import com.sun.java.util.collections.List;
import com.sun.java.util.collections.LinkedList;
import com.sun.java.util.collections.Iterator;
import java.util.Date;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manages state for push upload requests.
 */
public final class PushManager {
    
    private static final Log LOG =
      LogFactory.getLog(PushManager.class);

    /**
     * The timeout for the connect time while establishing the socket. Set to
     * the same value as NORMAL_CONNECT_TIME is ManagedDownloader.
     */
    private static final int CONNECT_TIMEOUT = 10000;//10 secs


	/**
	 * Accepts a new push upload.
     * NON-BLOCKING: creates a new thread to transfer the file.
	 * <p>
     * The thread connects to the other side, waits for a GET/HEAD,
     * and delegates to the UploaderManager.acceptUpload method with the
     * socket it created.
     * Essentially, this is a reverse-Acceptor.
     * <p>
     * No file and index are needed since the GET/HEAD will include that
     * information.  Just put in our first file and filename to create a
     * well-formed.
	 * @param host the ip address of the host to upload to
	 * @param port the port over which the transfer will occur
	 * @param guid the unique identifying client guid of the uploading client
     * @param forceAllow whether or not to force the UploadManager to send
     *  accept this request when it comes back.
     * @param isFWTransfer whether or not to use a UDP pipe to service this
     * upload.
	 */
	public void acceptPushUpload(final String host, 
                                 final int port, 
                                 final String guid,
                                 final boolean forceAllow,
                                 final boolean isFWTransfer) {
        if(LOG.isDebugEnabled())  {
            LOG.debug("acceptPushUp ip:"+host+" port:"+port+
              " FW:"+isFWTransfer);
        }
                                    
        if( host == null )
            throw new NullPointerException("null host");
        if( !NetworkUtils.isValidPort(port) )
            throw new IllegalArgumentException("invalid port: " + port);
        if( guid == null )
            throw new NullPointerException("null guid");
                                    

        FileManager fm = RouterService.getFileManager();
        
        // TODO: why is this check here?  it's a tiny optimization,
        // but could potentially kill any sharing of files that aren't
        // counted in the library.
        if (fm.getNumFiles() < 1 && fm.getNumIncompleteFiles() < 1)
            return;

        // We used to have code here that tested if the guy we are pushing to is
        // 1) hammering us, or 2) is actually firewalled.  1) is done above us
        // now, and 2) isn't as much an issue with the advent of connectback

        Thread runner=new ManagedThread("PushUploadThread") {
            public void managedRun() {
                Socket s = null;
                try {
        			// try to create the socket.
                    if (isFWTransfer)
                        s = new UDPConnection(host, port);
                    else 
                        s = Sockets.connect(host, port, CONNECT_TIMEOUT);
        			// open a stream for writing to the socket
        			OutputStream ostream = s.getOutputStream();        
        			String giv = "GIV 0:" + guid + "/file\n\n";
        			ostream.write(giv.getBytes());
        			ostream.flush();

                    //read GET or HEAD and delegate appropriately.
                    String word = IOUtils.readWord(s.getInputStream(), 4);
                    if(isFWTransfer)
                        UploadStat.FW_FW_SUCCESS.incrementStat();
                    
                    if (word.equals("GET")) {
                        UploadStat.PUSHED_GET.incrementStat();
                        RouterService.getUploadManager().acceptUpload(
                            HTTPRequestMethod.GET, s, forceAllow);
                    } else if (word.equals("HEAD")) {
                        UploadStat.PUSHED_HEAD.incrementStat();
                        RouterService.getUploadManager().acceptUpload(
                            HTTPRequestMethod.HEAD, s, forceAllow);
                    } else {
                        UploadStat.PUSHED_UNKNOWN.incrementStat();
                        throw new IOException();
                    }
                } catch(IOException ioe){
                    if(isFWTransfer)
                        UploadStat.FW_FW_FAILURE.incrementStat();
                    UploadStat.PUSH_FAILED.incrementStat();
                } finally {
                    if( s != null ) {
                        try {
                            s.getInputStream().close();
                        } catch(IOException ioe) {}
                        try {
                            s.getOutputStream().close();
                        } catch(IOException ioe) {}
                        try {
                            s.close();
                        } catch(IOException ioe) {}
                    }
                }
            }
        };
        runner.setDaemon(true);
        runner.start();
	}
}
