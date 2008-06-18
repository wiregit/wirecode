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
    
    private FileMediaNegotiator.JingleFile file;
    private final boolean sending;
    private OutputStream out;
    private InputStream in;
    protected Socket socket;
    protected ServerSocket serverSocket;
    protected String localIP;
    protected int localPort;
    protected String remoteIP;
    protected int remotePort;
    protected File saveDir;
    private final FileTransferProgressListener progressListener;

    public FileMediaSession(final FileMediaNegotiator.JingleFile file, final boolean sending,
            final TransportCandidate remote, final TransportCandidate local,
            JingleSession jingleSession, File saveDir, FileTransferProgressListener progressListener) {
        super(remote, local, jingleSession);
        this.file = file;
        this.sending = sending;
        this.saveDir = saveDir;
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
            if(sending) {
                serverSocket = new ServerSocket(localPort, 50, InetAddress.getByName(localIp));
            }
        }
        catch (IOException e) {
            // TODO throw
            e.printStackTrace();
        }
    }

    private void pipe(InputStream in, OutputStream out, FileMediaNegotiator.JingleFile beingTransferred, long size) throws IOException {
        progressListener.started(beingTransferred);
        byte [] buffer = new byte[1024];
        int read;
        long readSoFar = 0;
        while((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            readSoFar += read;
            progressListener.updated(beingTransferred, (int)(readSoFar / size)); // TODO in another Thread?
        }
        progressListener.completed(beingTransferred);
        in.close();
        out.close();
    }

    /**
     * Starts transmission and for NAT Traversal reasons start receiving also.
     */
    public void startTrasmit() {
        System.out.println("transmitting...");
        if(sending) {
            Thread sender = new Thread(new Runnable() {
                public void run() {                    
                    try {
                        System.out.println("accepting on " + serverSocket.getLocalPort());
                        Socket client = serverSocket.accept();
                        File toSend = file.getLocalFile();
                        in = new BufferedInputStream(new FileInputStream(toSend));
                        out = new BufferedOutputStream(client.getOutputStream());
                        pipe(in, out, file, file.getSize());
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
        System.out.println("receiving...");
        if(!sending) {
            Thread receiver = new Thread(new Runnable() {
                public void run() {                    
                    try {
                        Thread.sleep(2000);
                        //socket = new Socket(ip, remotePort, InetAddress.getByName(localIp), localPort);
                        System.out.println("connecting to " + remotePort);
                        socket = new Socket(remoteIP, remotePort, InetAddress.getByName(localIP), localPort);
                        File toSave = getNewFileToSave(file);
                        in = new BufferedInputStream(socket.getInputStream());
                        out = new BufferedOutputStream(new FileOutputStream(toSave));
                        pipe(in, out, file, file.getSize());
                    } catch (Exception e) {
                        // TODO throw
                        e.printStackTrace();
                    }
                }
            });
            receiver.start();
        }        
    }

    private File getNewFileToSave(StreamInitiation.File file) throws IOException {        
        File toSave = new File(saveDir, file.getName());
        toSave.getParentFile().mkdirs();         
        int i = 1;
        while(!toSave.createNewFile()) {
            toSave = new File(saveDir, toSave.getName() + i++);   
        }
        return toSave;
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
