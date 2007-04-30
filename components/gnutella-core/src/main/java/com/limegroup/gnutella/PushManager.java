package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.NetworkUtils;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestWritableByteChannel;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.rudp.UDPConnection;

import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.util.Sockets;

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
     * @param lan whether or not this is a request over a local network (
     * (force the UploadManager to accept this request when it comes back)
     * @param isFWTransfer whether or not to use a UDP pipe to service this
     * upload.
	 */
	public void acceptPushUpload(final String host, 
                                 final int port, 
                                 final String guid,
                                 final boolean lan,
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
        
        PushData data = new PushData(host, port, guid, lan);
        
        // If the transfer is to be done using FW-FW, then immediately start a new thread
        // which will connect using FWT.  Otherwise, do a non-blocking connect and have
        // the observer spawn the thread only if it succesfully connected.
        if(isFWTransfer) {
            if(LOG.isDebugEnabled())
                LOG.debug("Adding push observer FW-FW to host: " + host + ":" + port);
            UDPConnection socket = new UDPConnection();
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT*2, new PushObserver(data, true));
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("Adding push observer to host: " + host + ":" + port);
            try {
                Sockets.connect(host, port, CONNECT_TIMEOUT, new PushObserver(data, false));
            } catch(IOException iox) {
                UploadStat.PUSH_FAILED.incrementStat();
            }
        }
    }
    
    /** A simple collection of Push information */
    private static class PushData {
        private final String host;
        private final int port;
        private final String guid;
        private final boolean lan;
        
        PushData(String host, int port, String guid, boolean lan) {
            this.host = host;
            this.port = port;
            this.guid = guid;
            this.lan = lan;
        }
        
        public boolean isLan() {
            return lan;
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
        private final boolean fwt;
        
        PushObserver(PushData data, boolean fwt) {
            this.data = data;
            this.fwt = fwt;
        }        
        
        public void handleIOException(IOException iox) {}

        /** Increments the PUSH_FAILED stat and does nothing else. */
        public void shutdown() {
            if(LOG.isDebugEnabled())
                LOG.debug("Push (fwt: " + fwt + ") connect to: " + data.getHost() + ":" + data.getPort() + " failed");
            if(fwt)
                UploadStat.FW_FW_FAILURE.incrementStat();
            else
                UploadStat.PUSH_FAILED.incrementStat();
        }

        /** Starts a new thread that'll do the pushing. */
        public void handleConnect(Socket socket) throws IOException {
            if(LOG.isDebugEnabled())
                LOG.debug("Push (fwt: " + fwt + ") connect to: " + data.getHost() + ":" + data.getPort() + " succeeded");
            ((NIOMultiplexor) socket).setWriteObserver(new PushConnector(socket, data));
        }
    }    

    /** Non-blocking observer for connect events related to pushing. */
    private static class PushConnector implements ChannelWriter {

        private InterestWritableByteChannel channel;

        private final ByteBuffer buffer;

        private final Socket socket;

        private final PushData data;

        public PushConnector(Socket socket, PushData data) throws IOException {
            this.data = data;
            this.socket = socket;

            socket.setSoTimeout(30 * 1000);

            String giv = "GIV 0:"  + data.getGuid() + "/file\n\n";
            this.buffer = ByteBuffer.wrap(giv.getBytes());
        }

        public boolean handleWrite() throws IOException {
            if (!buffer.hasRemaining()) {
                return false;
            }

            while (buffer.hasRemaining()) {
                int written = channel.write(buffer);
                if (written == 0) {
                    return true;
                }
            }

//                try {
//                    if (word.equals("GET")) {
//                        UploadStat.PUSHED_GET.incrementStat();
//                        RouterService.getUploadManager().acceptUpload(HTTPRequestMethod.GET, socket, data.isLan());
//                    } else if (word.equals("HEAD")) {
//                        UploadStat.PUSHED_HEAD.incrementStat();
//                        RouterService.getUploadManager().acceptUpload(HTTPRequestMethod.HEAD, socket, data.isLan());
//                    } else {
//                        UploadStat.PUSHED_UNKNOWN.incrementStat();
//                        throw new IOException();
//                    }
//                } catch (IOException ioe) {
//                    if(LOG.isDebugEnabled())
//                        LOG.debug("Failed push connect/transfer to " + data.getHost() + ":" + data.getPort() + ", fwt: " + fwTransfer);
//                    if (fwTransfer)
//                        UploadStat.FW_FW_FAILURE.incrementStat();
//                    UploadStat.PUSH_FAILED.incrementStat();
//                }

            RouterService.getUploadManager().acceptUpload(socket, data.isLan());
            return false;
        }

        public void handleIOException(IOException iox) {
            throw new RuntimeException();
        }

        public void shutdown() {
            // ignore
        }

        public InterestWritableByteChannel getWriteChannel() {
            return channel;
        }

        public void setWriteChannel(InterestWritableByteChannel newChannel) {
            this.channel = newChannel;

            if (newChannel != null) {
                newChannel.interestWrite(this, true);
            }
        }
    }    

}
