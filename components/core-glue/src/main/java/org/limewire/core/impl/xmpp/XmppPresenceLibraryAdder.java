package org.limewire.core.impl.xmpp;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class XmppPresenceLibraryAdder implements RegisteringEventListener<RosterEvent> {
    public static final Log LOG = LogFactory.getLog(XmppPresenceLibraryAdder.class);

    private final RemoteLibraryManager remoteLibraryManager;

    @Inject
    public XmppPresenceLibraryAdder(RemoteLibraryManager remoteLibraryManager) {
        this.remoteLibraryManager = remoteLibraryManager;
    }

    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }

    public void handleEvent(RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            userAdded(event.getSource());
        }
    }    

    private void userAdded(final User user) {
        user.addPresenceListener(new PresenceListener() {            
            public void presenceChanged(final Presence presence) {
                if(presence instanceof FriendPresence) {
                    FriendPresence fPresence = (FriendPresence)presence;
                    if(presence.getType().equals(Presence.Type.available)) {
                        remoteLibraryManager.addPresenceLibrary(user, fPresence);
                    } else if(presence.getType().equals(Presence.Type.unavailable)) {
                        remoteLibraryManager.removePresenceLibrary(user, fPresence);
                    }
                }
            }
        });
    }
}