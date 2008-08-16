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
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.FileOfferHandler;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.api.client.RosterEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class XMPPServiceImpl implements Service, XMPPService, EventListener<AddressEvent> {

    private static final Log LOG = LogFactory.getLog(XMPPServiceImpl.class);
    
    public static final String LW_SERVICE_NS = "http://www.limewire.org/";
    
    private final CopyOnWriteArrayList<XMPPConnectionImpl> connections;
    private final Provider<List<XMPPConnectionConfiguration>> configurations;
    private FileOfferHandler fileOfferHandler;
    private final Provider<EventListener<RosterEvent>> rosterListener;
    private final AddressFactory addressFactory;
    private XMPPErrorListener errorListener;

    @Inject
    XMPPServiceImpl(Provider<List<XMPPConnectionConfiguration>> configurations,
                    Provider<EventListener<RosterEvent>> rosterListener, 
                    AddressFactory addressFactory) {
        this.configurations = configurations;
        this.rosterListener = rosterListener;
        this.addressFactory = addressFactory;
        this.connections = new CopyOnWriteArrayList<XMPPConnectionImpl>();
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Inject
    void register(ListenerSupport<AddressEvent> registry) {
        registry.addListener(this);
    }

//    public void register(RosterListener rosterListener) {
//        this.rosterListener = rosterListener;
//        for(XMPPConnectionImpl connection : connections) {
//            connection.addRosterListener(rosterListener);
//        }
//    }

    public void setXmppErrorListener(XMPPErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public void setFileOfferHandler(FileOfferHandler offerHandler) {
        this.fileOfferHandler = offerHandler;
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
            // TODO connection.getConfiguration().getErrorListener()
            errorListener.error(e);
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
        XMPPConnectionImpl connection = new XMPPConnectionImpl(configuration, rosterListener.get(), fileOfferHandler, addressFactory);
        connection.initialize();
        connections.add(connection);
    }

    public void handleEvent(AddressEvent event) {
        for(XMPPConnectionImpl connection : connections) {
            connection.handleEvent(event);
        }
    }
}
