package com.limegroup.gnutella.connection;

import java.net.Socket;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory {

    private final ConnectionManager connectionManager;
    private final NetworkManager networkManager;
    private final QueryRequestFactory queryRequestFactory;
    private final HeadersFactory headersFactory;
    private final HandshakeResponderFactory handshakeResponderFactory;

    public ManagedConnectionFactoryImpl(ConnectionManager connectionManager,
            NetworkManager networkManager,
            QueryRequestFactory queryRequestFactory,
            HeadersFactory headersFactory,
            HandshakeResponderFactory handshakeResponderFactory) {
        this.connectionManager = connectionManager;
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.headersFactory = headersFactory;
        this.handshakeResponderFactory = handshakeResponderFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.connection.ManagedConnectionFactory#createManagedConnection(java.lang.String,
     *      int)
     */
    public ManagedConnection createManagedConnection(String host, int port) {
        return createManagedConnection(host, port, ConnectType.PLAIN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.connection.ManagedConnectionFactory#createManagedConnection(java.lang.String,
     *      int, com.limegroup.gnutella.util.SocketsManager.ConnectType)
     */
    public ManagedConnection createManagedConnection(String host, int port,
            ConnectType type) {
        return new ManagedConnection(host, port, type, connectionManager,
                networkManager, queryRequestFactory, headersFactory,
                handshakeResponderFactory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.connection.ManagedConnectionFactory#createManagedConnection(java.net.Socket)
     */
    public ManagedConnection createManagedConnection(Socket socket) {
        return new ManagedConnection(socket, connectionManager, networkManager,
                queryRequestFactory, headersFactory, handshakeResponderFactory);
    }
}
