package org.limewire.xmpp.client.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.Roster;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.client.FriendConnectionFactory;
import org.limewire.core.api.friend.client.FriendConnectionFactoryRegistry;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionHistogram;
import org.limewire.inspection.InspectionPoint;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.xmpp.api.client.JabberSettings;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class XMPPConnectionFactoryImpl implements Service, FriendConnectionFactory {

    private static final Log LOG = LogFactory.getLog(XMPPConnectionFactoryImpl.class);

    private final XMPPConnectionImplFactory connectionImplFactory;
    private final JabberSettings jabberSettings;
    private final ListeningExecutorService executorService;

    // Connections that are logged in or logging in
    final List<XMPPFriendConnectionImpl> connections;
    private boolean multipleConnectionsAllowed;

    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        @InspectionPoint("xmpp service")
        private final Inspectable inspectable = new Inspectable() {
            @Override
            public Object inspect() {
                Map<Object, Object> map = new HashMap<Object, Object>();
                map.put("loggedIn", isLoggedIn());
                collectFriendStatistics(map);
                return map;
            }
            
            private void collectFriendStatistics(Map<Object, Object> data) {
                int count = 0;
                FriendConnection connection = getActiveConnection();
                InspectionHistogram<Integer> presencesHistogram = new InspectionHistogram<Integer>();
                if (connection != null) {
                    for (Friend user : connection.getFriends()) {
                        Map<String, FriendPresence> presences = user.getPresences();
                        presencesHistogram.count(presences.size());
                        for (FriendPresence presence : presences.values()) {
                            if (presence.hasFeatures(LimewireFeature.ID)) {
                                count++;
                                // break from inner presence loop, count each user only once
                                break;
                            }
                        }
                    }
                }
                data.put("limewire friends", count);
                data.put("presences", presencesHistogram.inspect());
            }
        };
    }

    @Inject
    public XMPPConnectionFactoryImpl(XMPPConnectionImplFactory connectionImplFactory,
                           EventMulticaster<FriendConnectionEvent> connectionBroadcaster,
            JabberSettings jabberSettings) {
        this.connectionImplFactory = connectionImplFactory;
        this.jabberSettings = jabberSettings;

        connections = new CopyOnWriteArrayList<XMPPFriendConnectionImpl>();
        multipleConnectionsAllowed = false;
        connectionBroadcaster.addListener(new ReconnectionManager(this));
        // We'll install our own subscription listeners
        Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);
        executorService = ExecutorsHelper.newSingleThreadExecutor(ExecutorsHelper.daemonThreadFactory("XMPPServiceImpl"));
    }

    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    /**
     * Sets the connection mode to idle (extended away) after receiving activity
     * events triggered by periods of inactivity.
     */
    @Inject
    void register(ListenerSupport<XmppActivityEvent> listenerSupport) {
        listenerSupport.addListener(new EventListener<XmppActivityEvent>() {
            @Override
            public void handleEvent(XmppActivityEvent event) {
                switch(event.getSource()) {
                case Idle:
                    try {
                        setModeImpl(FriendPresence.Mode.xa);
                    } catch (FriendException e) {
                        LOG.debugf(e, "couldn't set mode based on {0}", event);
                    }
                    break;
                case Active:
                    try {
                        setModeImpl(jabberSettings.isDoNotDisturbSet() ? FriendPresence.Mode.dnd : FriendPresence.Mode.available);
                    } catch (FriendException e) {
                        LOG.debugf(e, "couldn't set mode based on {0}", event);
                    }
                }
            }
        });
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
    }

    /**
     * Logs out all existing connections.
     */
    @Asynchronous
    @Override
    public void stop() {
        logoutImpl();
    }

    @Override
    public String getServiceName() {
        return "XMPP";
    }

    @Override
    public ListeningFuture<FriendConnection> login(final FriendConnectionConfiguration configuration) {
        return executorService.submit(new Callable<FriendConnection>() {
            @Override
            public FriendConnection call() throws Exception {
                return loginImpl(configuration);
            }
        }); 
    }

    @Override
    @Inject
    public void register(FriendConnectionFactoryRegistry registry) {
        registry.register(Network.Type.XMPP, this);
    }

    FriendConnection loginImpl(FriendConnectionConfiguration configuration) throws FriendException {
        return loginImpl(configuration, false);
    }

    FriendConnection loginImpl(FriendConnectionConfiguration configuration, boolean isReconnect) throws FriendException {
        synchronized (this) {
            if(!multipleConnectionsAllowed) {
                FriendConnection activeConnection = getActiveConnection();
                if(isReconnect) {
                    if(activeConnection != null) {
                        return activeConnection;
                    }
                } else {
                    if(activeConnection != null && activeConnection.getConfiguration().equals(configuration)) {
                        return activeConnection;
                    } else {
                        logoutImpl();
                    }    
                }                
            }
            
            XMPPFriendConnectionImpl connection = connectionImplFactory.createConnection(
                    configuration, executorService);
            try {
                connections.add(connection);
                connection.loginImpl();
                //maintain the last set login state available or do not disturb
                connection.setModeImpl(jabberSettings.isDoNotDisturbSet() ? FriendPresence.Mode.dnd : FriendPresence.Mode.available);
                return connection;
            } catch(FriendException e) {
                connections.remove(connection);
                LOG.debug(e.getMessage(), e);
                throw new FriendException(e);
            }
        }
    }

    private boolean isLoggedIn() {
        for(XMPPFriendConnectionImpl connection : connections) {
            if(connection.isLoggedIn()) {
                return true;
            }
        }
        return false;
    }

    private void logoutImpl() {
        for(XMPPFriendConnectionImpl connection : connections) {
            connection.logoutImpl();
        }
        connections.clear();
    }

    private XMPPFriendConnectionImpl getActiveConnection() {
        if(connections.isEmpty()) {
            return null;
        } else {
            try {
                return connections.get(0);
            } catch(IndexOutOfBoundsException ioobe) {
                return null; // possible because connections is CoW
            }
        }
    }

    // Only for testing
    void setMultipleConnectionsAllowed(boolean allowed) {
        multipleConnectionsAllowed = allowed;
    }

    // Only for testing
    List<? extends FriendConnection> getConnections() {
        return Collections.unmodifiableList(connections);
    } 
    
    private void setModeImpl(FriendPresence.Mode mode) throws FriendException {
        for(XMPPFriendConnectionImpl connection : connections) {
            connection.setModeImpl(mode);
        }
    }

    @Override
    public ListeningFuture<String> requestLoginUrl(FriendConnectionConfiguration configuration) {
        return null;
    }

    
}
