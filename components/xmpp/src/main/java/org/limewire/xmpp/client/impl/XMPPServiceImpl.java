package org.limewire.xmpp.client.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import com.limegroup.gnutella.NetworkManagerEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.client.service.FileTransferProgressListener;
import org.limewire.xmpp.client.service.IncomingFileAcceptor;
import org.limewire.xmpp.client.service.LibraryProvider;
import org.limewire.xmpp.client.service.XMPPConnection;
import org.limewire.xmpp.client.service.XMPPConnectionConfiguration;
import org.limewire.xmpp.client.service.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class XMPPServiceImpl implements Service, XMPPService, EventListener<NetworkManagerEvent> {

    private static final Log LOG = LogFactory.getLog(XMPPServiceImpl.class);
    
    public static final String LW_SERVICE_NS = "http://www.limewire.org/";
    
    private CopyOnWriteArrayList<XMPPConnectionImpl> connections;
    private final Provider<List<XMPPConnectionConfiguration>> configurations;
    private final LibraryProvider libraryProvider;
    private final IncomingFileAcceptor incomingFileAcceptor;
    private final FileTransferProgressListener progressListener;
    //private final NetworkManager networkManager;
    private final AddressFactory addressFactory;

    @Inject
    XMPPServiceImpl(Provider<List<XMPPConnectionConfiguration>> configurations,
                    LibraryProvider libraryProvider,
                    IncomingFileAcceptor incomingFileAcceptor,
                    FileTransferProgressListener progressListener,
                    /*NetworkManager networkManager,*/ AddressFactory addressFactory) {
        this.configurations = configurations;
        this.libraryProvider = libraryProvider;
        this.incomingFileAcceptor = incomingFileAcceptor;
        this.progressListener = progressListener;
        //this.networkManager = networkManager;
        this.addressFactory = addressFactory;
        this.connections = new CopyOnWriteArrayList<XMPPConnectionImpl>();
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Inject
    void register(ListenerSupport<NetworkManagerEvent> registry) {
        registry.addListener(this);
    }

    public void initialize() {
        for(XMPPConnectionConfiguration configuration : configurations.get()) {
            addConnectionConfiguration(configuration);
        }
    }

    /**
     * Logs into the xmpp service specified in the <code>XMPPConfiguration</code>
     */
    @Asynchronous 
    public void start() {
        for(XMPPConnection connection : connections) {
            if(connection.getConfiguration().isAutoLogin()) {
                login(connection);
            }
        }        
    }

    private void login(XMPPConnection connection) {
        try {
            // TODO async
            connection.login();
        } catch (XMPPException e) {
            LOG.error(e.getMessage(), e);
            connection.getConfiguration().getErrorListener().error(e);
        }
    }

    /**
     * Disconnects from the xmpp server
     */
    @Asynchronous
    public void stop() {
        for(XMPPConnection connection : connections) {
            // TODO async
            connection.logout();
        }
    }

    public String getServiceName() {
        return "XMPP";
    }

    public List<XMPPConnection> getConnections() {
        List<XMPPConnection> copy = new ArrayList<XMPPConnection>();
        copy.addAll(connections);
        return Collections.unmodifiableList(copy);
    }

    public void addConnectionConfiguration(XMPPConnectionConfiguration configuration) {
        XMPPConnectionImpl connection = new XMPPConnectionImpl(configuration, libraryProvider, incomingFileAcceptor, progressListener,  null/*networkManager*/, addressFactory);
        connection.initialize();
        connections.add(connection);
    }

    public void handleEvent(NetworkManagerEvent event) {
        for(XMPPConnectionImpl connection : connections) {
            connection.handleEvent(event);
        }
    }
}
