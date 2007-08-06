package com.limegroup.gnutella.connection;

import java.net.Socket;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.MessageDispatcher;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NetworkUpdateSanityChecker;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;

@Singleton
public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory {

    private final Provider<ConnectionManager> connectionManager;
    private final NetworkManager networkManager;
    private final QueryRequestFactory queryRequestFactory;
    private final HeadersFactory headersFactory;
    private final HandshakeResponderFactory handshakeResponderFactory;
    private final QueryReplyFactory queryReplyFactory;
    private final Provider<MessageDispatcher> messageDispatcher;
    private final Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker;
    private final Provider<UDPService> udpService;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<SearchResultHandler> searchResultHandler;

    @Inject
    public ManagedConnectionFactoryImpl(
            Provider<ConnectionManager> connectionManager,
            NetworkManager networkManager,
            QueryRequestFactory queryRequestFactory,
            HeadersFactory headersFactory,
            HandshakeResponderFactory handshakeResponderFactory,
            QueryReplyFactory queryReplyFactory,
            Provider<MessageDispatcher> messageDispatcher,
            Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker,
            Provider<UDPService> udpService,
            Provider<MessageRouter> messageRouter,
            Provider<SearchResultHandler> searchResultHandler) {
        this.connectionManager = connectionManager;
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.headersFactory = headersFactory;
        this.handshakeResponderFactory = handshakeResponderFactory;
        this.queryReplyFactory = queryReplyFactory;
        this.messageDispatcher = messageDispatcher;
        this.networkUpdateSanityChecker = networkUpdateSanityChecker;
        this.udpService = udpService;
        this.messageRouter = messageRouter;
        this.searchResultHandler = searchResultHandler;
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
        return new ManagedConnection(host, port, type, connectionManager.get(),
                networkManager, queryRequestFactory, headersFactory,
                handshakeResponderFactory, queryReplyFactory, messageDispatcher
                        .get(), networkUpdateSanityChecker.get(), udpService
                        .get(), messageRouter.get(), searchResultHandler.get());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.connection.ManagedConnectionFactory#createManagedConnection(java.net.Socket)
     */
    public ManagedConnection createManagedConnection(Socket socket) {
        return new ManagedConnection(socket, connectionManager.get(),
                networkManager, queryRequestFactory, headersFactory,
                handshakeResponderFactory, queryReplyFactory,
                messageDispatcher.get(), networkUpdateSanityChecker.get(), udpService.get(),
                messageRouter.get(), searchResultHandler.get());
    }

}
