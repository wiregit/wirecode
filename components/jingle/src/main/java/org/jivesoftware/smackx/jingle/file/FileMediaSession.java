package org.jivesoftware.smackx.jingle.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.packet.StreamInitiation;

public class FileMediaSession extends JingleMediaSession {
    private StreamInitiation.File file;
    private final boolean sending;
    private OutputStream out;
    private InputStream in;
    protected Socket socket;
    protected ServerSocket serverSocket;

    private String remoteIP;
    private String localIP;
    private int remotePort;
    private int localPort;

    public FileMediaSession(final StreamInitiation.File file, final boolean sending,
            final TransportCandidate remote, final TransportCandidate local,
            JingleSession jingleSession) {
        super(remote, local, jingleSession);
        this.file = file;
        this.sending = sending;
        initialize();
    }

    protected void initialize(String ip, String localIp, int localPort, int remotePort) {
        // TODO delegate to transport negotiator?
        // TODO push up?
        this.remoteIP = ip;
        this.remotePort = remotePort;
        this.localIP = localIp;
        this.localPort = localPort;
        try {
            if(sending) {
                serverSocket = new ServerSocket(localPort, 50, InetAddress.getByName(localIp));
            }
        }
        catch (IOException e) {
            // TODO throw
            e.printStackTrace();
        }
    }

    private void pipe(InputStream in, OutputStream out) throws IOException {
        byte [] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);    
        }
        in.close();
        out.close();
    }

    /**
     * Starts transmission and for NAT Traversal reasons start receiving also.
     */
    public void startTrasmit() {
        try {
            if(sending) {
                serverSocket = new ServerSocket(localPort, 50, InetAddress.getByName(localIP));
                Socket client = serverSocket.accept();
                File toSend = getLocalFile(file);
                in = new BufferedInputStream(new FileInputStream(toSend));
                out = new BufferedOutputStream(client.getOutputStream());
                pipe(in, out);
            }
        }
        catch (Exception e) {
            // TODO throw
            e.printStackTrace();
        }        
    }

    private File getLocalFile(StreamInitiation.File file) {
        return null; // TODO
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
        try {
            if(!sending) {
                socket = new Socket(remoteIP, remotePort, InetAddress.getByName(localIP), localPort);
                File toSave = getNewFileToSave(file);
                in = new BufferedInputStream(socket.getInputStream());
                out = new BufferedOutputStream(new FileOutputStream(toSave));
                pipe(in, out);
            }
        }
        catch (Exception e) {
            // TODO throw
            e.printStackTrace();
        }
    }

    private File getNewFileToSave(StreamInitiation.File file) {
        return null; // TODO
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
