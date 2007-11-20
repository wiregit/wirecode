package com.limegroup.gnutella.privategroups;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;

public class XMPPConnectionPGRP extends XMPPConnection{


    private String host;
    private String serviceName;
    private int port;
    private Socket socket;
//    PacketWriter packetWriter;
//    PacketReader packetReader;
    
    public XMPPConnectionPGRP(ConnectionConfiguration config) {
        super(config);
    }
    
    public XMPPConnectionPGRP(String serverAddress) {
        super(serverAddress);
    }
    
    public void initConnection(){
//        
//        boolean isFirstInitialization = packetReader == null || packetWriter == null;
//        if (!isFirstInitialization) {
//            usingCompression = false;
//        }
//
//        // Set the reader and writer instance variables
//        super.initReaderAndWriter();
//
//        try {
//            if (isFirstInitialization) {
//                packetWriter = new PacketWriter(this);
//                packetReader = new PacketReader(this);
//
//                // If debugging is enabled, we should start the thread that will listen for
//                // all packets and then log them.
//                if (configuration.isDebuggerEnabled()) {
//                    packetReader.addPacketListener(debugger.getReaderListener(), null);
//                    if (debugger.getWriterListener() != null) {
//                        packetWriter.addPacketListener(debugger.getWriterListener(), null);
//                    }
//                }
//            }
//            else {
//                packetWriter.init();
//                packetReader.init();
//            }
//
//            // Start the packet writer. This will open a XMPP stream to the server
//            packetWriter.startup();
//            // Start the packet reader. The startup() method will block until we
//            // get an opening stream packet back from server.
//            packetReader.startup();
//
//            // Make note of the fact that we're now connected.
//            connected = true;
//
//            // Start keep alive process (after TLS was negotiated - if available)
//            packetWriter.startKeepAliveProcess();
//
//
//            if (isFirstInitialization) {
//                // Notify listeners that a new connection has been established
//                for (ConnectionCreationListener listener : connectionEstablishedListeners) {
//                    listener.connectionCreated(this);
//                }
//            }
//            else {
//                packetReader.notifyReconnection();
//            }
//
//        }
//        catch (XMPPException ex) {
//            // An exception occurred in setting up the connection. Make sure we shut down the
//            // readers and writers and close the socket.
//
//            if (packetWriter != null) {
//                try {
//                    packetWriter.shutdown();
//                }
//                catch (Throwable ignore) { /* ignore */ }
//                packetWriter = null;
//            }
//            if (packetReader != null) {
//                try {
//                    packetReader.shutdown();
//                }
//                catch (Throwable ignore) { /* ignore */ }
//                packetReader = null;
//            }
//            if (reader != null) {
//                try {
//                    reader.close();
//                }
//                catch (Throwable ignore) { /* ignore */ }
//                reader = null;
//            }
//            if (writer != null) {
//                try {
//                    writer.close();
//                }
//                catch (Throwable ignore) {  /* ignore */}
//                writer = null;
//            }
//            if (socket != null) {
//                try {
//                    socket.close();
//                }
//                catch (Exception e) { /* ignore */ }
//                socket = null;
//            }
//            this.setWasAuthenticated(authenticated);
//            authenticated = false;
//            connected = false;
//
//            throw ex;        // Everything stoppped. Now throw the exception.
//        }
//        
        
        

    }
    
    
    public void connect(String host, String port) throws XMPPException {

        connectUsingConfiguration();
    }
    
    private void connectUsingConfiguration() throws XMPPException {
        this.host = super.getHost();
        this.port = super.getPort();
        try {
            if (super.getConfiguration().getSocketFactory() == null) {
                this.socket = new Socket(host, port);
            }
            else {
                this.socket = super.getConfiguration().getSocketFactory().createSocket(host, port);
            }
        }
        catch (UnknownHostException uhe) {
            String errorMessage = "Could not connect to " + host + ":" + port + ".";
            throw new XMPPException(errorMessage, new XMPPError(
                    XMPPError.Condition.remote_server_timeout, errorMessage),
                    uhe);
        }
        catch (IOException ioe) {
            String errorMessage = "XMPPError connecting to " + host + ":"
                    + port + ".";
            throw new XMPPException(errorMessage, new XMPPError(
                    XMPPError.Condition.remote_server_error, errorMessage), ioe);
        }
        this.serviceName = super.getConfiguration().getServiceName();
        initConnection();
    }

}
