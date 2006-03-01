package com.limegroup.gnutella;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.util.ThreadFactory;

/**
 * Manages state for push upload requests.
 */
public final class PushManager {
    
    private static final Log LOG = LogFactory.getLog(PushManager.class);

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
        if (LOG.isDebugEnabled())
            LOG.debug("Accepting Push Upload from ip:" + host + " port:" + port + " FW:" + isFWTransfer);
                                    
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
        
        PushData data = new PushData(host, port, guid, forceAllow);
        
        // If the transfer is to be done using FW-FW, then immediately start a new thread
        // which will connect using FWT.  Otherwise, do a non-blocking connect and have
        // the observer spawn the thread only if it succesfully connected.
        if(isFWTransfer) {
            startPushRunner(data, null);
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("Adding push observer to host: " + host + ":" + port);
            try {
                Sockets.connect(host, port, CONNECT_TIMEOUT, new PushObserver(data));
            } catch(IOException iox) {
                UploadStat.PUSH_FAILED.incrementStat();
            }
        }
    }

    /**
     * Starts a thread that'll do the pushing using the given PushData & Socket.
     * @param data All the data about the push.
     * @param socket The possibly null socket.
     */
    private static void startPushRunner(PushData data, Socket socket) {
        ThreadFactory.startThread(new Pusher(data, socket), "PushUploadThread");
    }
    
    /** A simple collection of Push information */
    private static class PushData {
        private final String host;
        private final int port;
        private final String guid;
        private final boolean forceAllow;
        
        PushData(String host, int port, String guid, boolean forceAllow) {
            this.host = host;
            this.port = port;
            this.guid = guid;
            this.forceAllow = forceAllow;
        }
        
        public boolean isForceAllow() {
            return forceAllow;
        }
        public String getGuid() {
            return guid;
        }
        public String getHost() {
            return host;
        }
        public int getPort() {
            return port;
        }
    }
    
    /** Non-blocking observer for connect events related to pushing. */
    private static class PushObserver implements ConnectObserver {
        private final PushData data;
        
        PushObserver(PushData data) {
            this.data = data;
        }        
        
        public void handleIOException(IOException iox) {}

        /** Increments the PUSH_FAILED stat and does nothing else. */
        public void shutdown() {
            if(LOG.isDebugEnabled())
                LOG.debug("Push connect to: " + data.getHost() + ":" + data.getPort() + " failed");
            UploadStat.PUSH_FAILED.incrementStat();
        }

        /** Starts a new thread that'll do the pushing. */
        public void handleConnect(Socket socket) throws IOException {
            if(LOG.isDebugEnabled())
                LOG.debug("Push connect to: " + data.getHost() + ":" + data.getPort() + " succeeded");            
            startPushRunner(data, socket);
        }
    }    

    /** A runnable that starts a push transfer. */
    private static class Pusher implements Runnable {
        PushData data;
        private Socket socket;
        private boolean fwTransfer;
        
        Pusher(PushData data, Socket socket) {
            this.data = data;
            this.socket = socket;
        }

        public void run() {
            try {
                if (socket == null) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Creating UDP Connection to " + data.getHost() + ":" + data.getPort());
                    fwTransfer = true;
                    socket = new UDPConnection(data.getHost(), data.getPort());
                }

                OutputStream ostream = socket.getOutputStream();
                String giv = "GIV 0:" + data.getGuid() + "/file\n\n";
                ostream.write(giv.getBytes());
                ostream.flush();

                // try to read a GET or HEAD for only 30 seconds.
                socket.setSoTimeout(30 * 1000);

                // read GET or HEAD and delegate appropriately.
                String word = IOUtils.readWord(socket.getInputStream(), 4);
                if (fwTransfer)
                    UploadStat.FW_FW_SUCCESS.incrementStat();

                if (word.equals("GET")) {
                    UploadStat.PUSHED_GET.incrementStat();
                    RouterService.getUploadManager().acceptUpload(HTTPRequestMethod.GET, socket, data.isForceAllow());
                } else if (word.equals("HEAD")) {
                    UploadStat.PUSHED_HEAD.incrementStat();
                    RouterService.getUploadManager().acceptUpload(HTTPRequestMethod.HEAD, socket, data.isForceAllow());
                } else {
                    UploadStat.PUSHED_UNKNOWN.incrementStat();
                    throw new IOException();
                }
            } catch (IOException ioe) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Failed push connect/transfer to " + data.getHost() + ":" + data.getPort() + ", fwt: " + fwTransfer);
                if (fwTransfer)
                    UploadStat.FW_FW_FAILURE.incrementStat();
                UploadStat.PUSH_FAILED.incrementStat();
            } finally {
                IOUtils.close(socket);
            }
        }
    }
}
