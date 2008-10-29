package org.limewire.ui.swing.friends;

import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendsPaneRosterListener implements RegisteringEventListener<RosterEvent> {
    private static final Log LOG = LogFactory.getLog(FriendsPaneRosterListener.class);

        
    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }
    
    public void handleEvent(RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            userAdded(event.getSource());
        } //else if(event.getType().equals(User.EventType.USER_REMOVED)) {
//            userDeleted(event.getSource().getId());
//        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
//            userUpdated(event.getSource());
//        }
    }

    //TODO: Add presenceChanged listeners directly (elsewhere) and avoid global publish
    public void userAdded(final User user) {
        user.addPresenceListener(new EventListener<PresenceEvent>() {
            public void handleEvent(PresenceEvent event) {
                Presence presence = event.getSource();
                LOG.debugf("presenceChanged(). Presence jid: {0} presence-type: {1}", presence.getJID(), presence.getMode());
                if(presence.getType().equals(Presence.Type.available)) {
                    //TODO: Should distinguish between Sharable/Lime and "regular" presence with 2 event types
                    new PresenceUpdateEvent(presence, event.getType()).publish();
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    new PresenceUpdateEvent(presence, event.getType()).publish();
                } else {
                    // TODO update UI
                }
            }
        });        
    }
}

