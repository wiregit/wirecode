package com.limegroup.gnutella.connection;

import java.net.Socket;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.GuidMapManager;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.MessageDispatcher;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NetworkUpdateSanityChecker;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.util.SocketsManager;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;
import com.limegroup.gnutella.version.UpdateHandler;

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

    private final Provider<SearchResultHandler> searchResultHandler;

    private final CapabilitiesVMFactory capabilitiesVMFactory;

    private final Provider<SocketsManager> socketsManager;

    private final Provider<Acceptor> acceptor;

    private final MessagesSupportedVendorMessage supportedVendorMessage;

    private final Provider<SimppManager> simppManager;

    private final Provider<UpdateHandler> updateHandler;

    private final Provider<ConnectionServices> connectionServices;

    private final GuidMapManager guidMapManager;

    private final SpamFilterFactory spamFilterFactory;

    private final MessageFactory messageFactory;

    private final MessageReaderFactory messageReaderFactory;

    private final ApplicationServices applicationServices;

    @Inject
    public ManagedConnectionFactoryImpl(Provider<ConnectionManager> connectionManager,
            NetworkManager networkManager, QueryRequestFactory queryRequestFactory,
            HeadersFactory headersFactory, HandshakeResponderFactory handshakeResponderFactory,
            QueryReplyFactory queryReplyFactory, Provider<MessageDispatcher> messageDispatcher,
            Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker,
            Provider<SearchResultHandler> searchResultHandler,
            CapabilitiesVMFactory capabilitiesVMFactory, Provider<SocketsManager> socketsManager,
            Provider<Acceptor> acceptor, MessagesSupportedVendorMessage supportedVendorMessage,
            Provider<SimppManager> simppManager, Provider<UpdateHandler> updateHandler,
            Provider<ConnectionServices> connectionServices, GuidMapManager guidMapManager,
            SpamFilterFactory spamFilterFactory, MessageFactory messageFactory,
            MessageReaderFactory messageReaderFactory, ApplicationServices applicationServices) {
        this.connectionManager = connectionManager;
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.headersFactory = headersFactory;
        this.handshakeResponderFactory = handshakeResponderFactory;
        this.queryReplyFactory = queryReplyFactory;
        this.messageDispatcher = messageDispatcher;
        this.networkUpdateSanityChecker = networkUpdateSanityChecker;
        this.applicationServices = applicationServices;
        this.searchResultHandler = searchResultHandler;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.socketsManager = socketsManager;
        this.acceptor = acceptor;
        this.supportedVendorMessage = supportedVendorMessage;
        this.simppManager = simppManager;
        this.updateHandler = updateHandler;
        this.connectionServices = connectionServices;
        this.guidMapManager = guidMapManager;
        this.spamFilterFactory = spamFilterFactory;
        this.messageFactory = messageFactory;
        this.messageReaderFactory = messageReaderFactory;
    }

    public ManagedConnection createManagedConnection(String host, int port) {
        return createManagedConnection(host, port, ConnectType.PLAIN);
    }

    public ManagedConnection createManagedConnection(String host, int port, ConnectType type) {
        return new ManagedConnection(host, port, type, connectionManager.get(), networkManager,
                queryRequestFactory, headersFactory, handshakeResponderFactory, queryReplyFactory,
                messageDispatcher.get(), networkUpdateSanityChecker.get(), searchResultHandler
                        .get(), capabilitiesVMFactory, socketsManager.get(), acceptor.get(),
                supportedVendorMessage, simppManager, updateHandler, connectionServices,
                guidMapManager, spamFilterFactory, messageReaderFactory, messageFactory,
                applicationServices);
    }

    public ManagedConnection createManagedConnection(Socket socket) {
        return new ManagedConnection(socket, connectionManager.get(), networkManager,
                queryRequestFactory, headersFactory, handshakeResponderFactory, queryReplyFactory,
                messageDispatcher.get(), networkUpdateSanityChecker.get(), searchResultHandler
                        .get(), capabilitiesVMFactory, acceptor.get(), supportedVendorMessage,
                simppManager, updateHandler, connectionServices, guidMapManager, spamFilterFactory,
                messageReaderFactory, messageFactory, applicationServices);
    }

}
