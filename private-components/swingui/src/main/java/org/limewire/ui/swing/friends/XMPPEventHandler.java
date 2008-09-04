package org.limewire.ui.swing.friends;

import java.util.List;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.api.client.Presence.Mode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class XMPPEventHandler {
    private static final Log LOG = LogFactory.getLog(XMPPEventHandler.class);
    private final XMPPService xmppService;

    @Inject
    public XMPPEventHandler(XMPPService service) {
        this.xmppService = service;
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    public void login(String serviceName, String username, String password, boolean autologin) throws XMPPException {
        List<XMPPConnection> connections = xmppService.getConnections();
        for(XMPPConnection connection : connections) {
            XMPPConnectionConfiguration configuration = connection.getConfiguration();
            if(configuration.getServiceName().equals(serviceName)) {
                if(!connection.isLoggedIn()) {
                    configuration.setUsername(username);
                    configuration.setPassword(password);
                    configuration.setAutoLogin(autologin);
                    connection.login();
                }
            }
        }
    }
    
    @EventSubscriber
    public void handleSigninEvent(XMPPConnectionEstablishedEvent event) {
        new PresenceChangeEvent(Mode.available).publish();
    }
    
    @EventSubscriber
    public void handleSignoffEvent(SignoffEvent event) {
        ThreadExecutor.startThread(new Runnable() {
            public void run() {
                logout();
            }
        }, "xmpp-logout");
    }
    
    private void logout() {
        final XMPPConnection connection = getLoggedInConnection();
        if (connection != null) {
            connection.logout();
        }
    }
    
    public XMPPConnectionConfiguration getConfig(String serviceName) {
        XMPPConnectionConfiguration config = null;
        List<XMPPConnection> connections = xmppService.getConnections();
        for(XMPPConnection connection : connections) {
            XMPPConnectionConfiguration configuration = connection.getConfiguration();
            if(configuration.getServiceName().equals(serviceName)) {
                config = configuration;
            }
        }
        return config;
    }
    
    @EventSubscriber
    public void handlePresenceChange(PresenceChangeEvent event) {
        final XMPPConnection connection = getLoggedInConnection();
        if (connection != null) {
            LOG.debugf("Changing presence for {0} to {1}", connection.getConfiguration().getServiceName(), event.getNewMode());
            connection.setMode(event.getNewMode());
        }
    }
    
    private XMPPConnection getLoggedInConnection() {
        List<XMPPConnection> connections = xmppService.getConnections();
        for(XMPPConnection connection : connections) {
            if (connection.isLoggedIn()) {
                return connection;
            }
        }
        return null;
    }
    
    @EventSubscriber
    public void handleAddBuddy(final AddBuddyEvent event) {
        final XMPPConnection connection = getLoggedInConnection();
        if (connection != null) {
            LOG.debugf("Adding new buddy: ID {0} - Name {1}", event.getId(), event.getName());
            ThreadExecutor.startThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        connection.addUser(event.getId(), event.getName());
                    } catch (XMPPException e) {
                        LOG.error("Could not add buddy", e);
                    }
                }
            }, "add-buddy");
        }
    }
    
    @EventSubscriber
    public void handleRemoveBuddy(final RemoveBuddyEvent event) {
        final XMPPConnection connection = getLoggedInConnection();
        if (connection != null) {
            LOG.debugf("Removing buddy: ID {0} - Name {1}", event.getFriend().getID(), event.getFriend().getName());
            ThreadExecutor.startThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        connection.removeUser(event.getFriend().getID());
                    } catch (XMPPException e) {
                        LOG.error("Could not remove buddy", e);
                    }
                }
            }, "remove-buddy");
        }
    }
}
