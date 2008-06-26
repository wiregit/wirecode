package org.jivesoftware.smackx.jingle.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.packet.file.FileDescription;
import org.jivesoftware.smackx.packet.StreamInitiation;

public class FileMediaSession extends JingleMediaSession {
    
    private static final Log LOG = LogFactory.getLog(FileMediaSession.class);
    
    private FileDescription.FileContainer file;
    protected Socket socket;
    protected ServerSocket serverSocket;
    protected String localIP;
    protected int localPort;
    protected String remoteIP;
    protected int remotePort;
    private final boolean initiator;
    protected FileLocator fileLocator;
    private final FileTransferProgressListener progressListener;

    public FileMediaSession(final FileDescription.FileContainer file, boolean isInitiator,
            final TransportCandidate remote, final TransportCandidate local,
            JingleSession jingleSession, FileLocator fileLocator, FileTransferProgressListener progressListener) {
        super(remote, local, jingleSession);
        this.file = file;
        initiator = isInitiator;
        this.fileLocator = fileLocator;
        this.progressListener = progressListener;
        initialize();
    }

    protected void initialize(String ip, String localIp, int localPort, int remotePort) {
        // TODO delegate to transport negotiator?
        // TODO push up?
        this.localIP = localIp;
        this.localPort = localPort;
        this.remoteIP = ip;
        this.remotePort = remotePort;
        try {
            if(isSending()) {
                serverSocket = new ServerSocket(localPort, 50, InetAddress.getByName(localIp));
            }
        }
        catch (IOException e) {
            // TODO throw
            e.printStackTrace();
        }
    }

    private boolean isSending() {
        return (file instanceof FileDescription.Offer && initiator) || (file instanceof FileDescription.Request && !initiator);
    }

    private void pipe(InputStream in, OutputStream out, StreamInitiation.File beingTransferred, long size) throws IOException {
        progressListener.started(beingTransferred);
        byte [] buffer = new byte[1024];
        int read;
        long readSoFar = 0;
        while((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            readSoFar += read;
            if(size != 0) {
                progressListener.updated(beingTransferred, (int)(readSoFar / size)); // TODO in another Thread?
            }
        }
        progressListener.completed(beingTransferred);
        in.close();
        out.close();
    }

    /**
     * Starts transmission and for NAT Traversal reasons start receiving also.
     */
    public void startTrasmit() {
        LOG.info("transmitting...");
        if(isSending()) {
            Thread sender = new Thread(new Runnable() {
                public void run() {                    
                    try {
                        LOG.debug("accepting on " + serverSocket.getLocalPort());
                        Socket client = serverSocket.accept();
                        InputStream in = fileLocator.readFile(file.getFile());
                        OutputStream out = new BufferedOutputStream(client.getOutputStream());
                        pipe(in, out, file.getFile(), file.getFile().getSize());
                    } catch (Exception e) {
                        // TODO throw
                        e.printStackTrace();
                    }
                }
            });
            sender.start();
        }     
    }

    /**
     * Set transmit activity. If the active is true, the instance should trasmit.
     * If it is set to false, the instance should pause transmit.
     *
     * @param active active state
     */
    public void setTrasmit(boolean active) {
        
    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    public void startReceive() {      
        LOG.info("receiving...");
        if(!isSending()) {
            Thread receiver = new Thread(new Runnable() {
                public void run() {                    
                    try {
                        Thread.sleep(2000);
                        //socket = new Socket(ip, remotePort, InetAddress.getByName(localIp), localPort);
                        LOG.debug("connecting to " + remotePort);
                        socket = new Socket(remoteIP, remotePort, InetAddress.getByName(localIP), localPort);
                        OutputStream out = fileLocator.writeFile(file.getFile());
                        InputStream in = new BufferedInputStream(socket.getInputStream());
                        pipe(in, out, file.getFile(), file.getFile().getSize());
                    } catch (Exception e) {
                        // TODO throw
                        e.printStackTrace();
                    }
                }
            });
            receiver.start();
        }        
    }

    /**
     * Stops transmission and for NAT Traversal reasons stop receiving also.
     */
    public void stopTrasmit() {

    }

    /**
     * For NAT Reasons this method does nothing. Use startTransmit() to start transmit and receive jmf
     */
    public void stopReceive() {
        // Do nothing
    }

}
