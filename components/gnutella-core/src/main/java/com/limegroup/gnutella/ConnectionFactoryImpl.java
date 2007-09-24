package com.limegroup.gnutella;

import java.net.Socket;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.util.SocketsManager;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

public class ConnectionFactoryImpl implements ConnectionFactory {

    private final CapabilitiesVMFactory capabilitiesVMFactory;
    private final Provider<SocketsManager> socketsManager;
    private final Provider<Acceptor> acceptor;
    private final MessagesSupportedVendorMessage supportedVendorMessage;
    private final MessageFactory messageFactory;
    private final NetworkManager networkManager;
    

    @Inject
    public ConnectionFactoryImpl(CapabilitiesVMFactory capabilitiesVMFactory,
            Provider<SocketsManager> socketsManager,
            Provider<Acceptor> acceptor,
            MessagesSupportedVendorMessage supportedVendorMessage,
            MessageFactory messageFactory, NetworkManager networkManager) {
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.socketsManager = socketsManager;
        this.acceptor = acceptor;
        this.supportedVendorMessage = supportedVendorMessage;
        this.messageFactory = messageFactory;
        this.networkManager = networkManager;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionFactory#createConnection(java.net.Socket)
     */
    public Connection createConnection(Socket socket) {
        return new Connection(socket, capabilitiesVMFactory, acceptor.get(),
                supportedVendorMessage, messageFactory, networkManager);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionFactory#createConnection(java.lang.String, int)
     */
    public Connection createConnection(String host, int port) {
        return createConnection(host, port, ConnectType.PLAIN);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionFactory#createConnection(java.lang.String, int, com.limegroup.gnutella.util.SocketsManager.ConnectType)
     */
    public Connection createConnection(String host, int port,
            ConnectType connectType) {
        return new Connection(host, port, connectType, capabilitiesVMFactory,
                socketsManager.get(), acceptor.get(), supportedVendorMessage,
                messageFactory, networkManager);
    }

}
