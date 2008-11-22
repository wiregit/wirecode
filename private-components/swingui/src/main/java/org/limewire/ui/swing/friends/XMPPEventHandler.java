package org.limewire.ui.swing.friends;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.FriendSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.setting.IntSetting;
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
    
    public void login(XMPPConnectionConfiguration configuration) {
        xmppService.login(configuration);
    }

    @EventSubscriber
    public void handleSigninEvent(XMPPConnectionEstablishedEvent event) {
        new SelfAvailabilityUpdateEvent(Mode.available).publish();
        IntSetting loginCount = FriendSettings.NUM_LOGINS;
        loginCount.setValue(loginCount.getValue() + 1);
    }
    
    @EventSubscriber
    public void handleSignoffEvent(SignoffEvent event) {
        ThreadExecutor.startThread(new Runnable() {
            public void run() {
                xmppService.logout();
            }
        }, "xmpp-logout");
    }
    
    @EventSubscriber
    public void handlePresenceChange(SelfAvailabilityUpdateEvent event) {
        final XMPPConnection connection = getLoggedInConnection();
        if (connection != null) {
            LOG.debugf("Changing presence for {0} to {1}", connection.getConfiguration().getServiceName(), event.getNewMode());
            connection.setMode(event.getNewMode());
        }
    }
    
    public XMPPConnection getLoggedInConnection() {
        return xmppService.getActiveConnection();
    }
    
    @EventSubscriber
    public void handleAddFriend(final AddFriendEvent event) {
        final XMPPConnection connection = getLoggedInConnection();
        if (connection != null) {
            LOG.debugf("Adding new friend: ID {0} - Name {1}", event.getId(), event.getName());
            ThreadExecutor.startThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        connection.addUser(event.getId(), event.getName());
                    } catch (XMPPException e) {
                        LOG.error("Could not add friend", e);
                    }
                }
            }, "add-friend");
        }
    }
}
