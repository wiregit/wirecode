package org.limewire.ui.swing.friends;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteFileList;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.library.BuddyLibrary;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.Navigator.NavCategory;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendsPaneRosterListener implements RegisteringEventListener<RosterEvent> {
    private static final Log LOG = LogFactory.getLog(FriendsPaneRosterListener.class);

    private final Navigator navigator;
    private final LibraryManager libraryManager;
    
    @Inject
    public FriendsPaneRosterListener(Navigator navigator, LibraryManager libraryManager) {
        this.navigator = navigator;
        this.libraryManager = libraryManager;
    }
    
    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }
    
    public void handleEvent(RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            userAdded(event.getSource());
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            userDeleted(event.getSource().getId());
        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
            userUpdated(event.getSource());
        }
    }

    public void userAdded(final User user) {
        user.addPresenceListener(new PresenceListener() {
            public void presenceChanged(final Presence presence) {
                LOG.debugf("presenceChanged(). Presence jid: {0} presence-type: {1}", presence.getJID(), presence.getMode());
                if(presence.getType().equals(Presence.Type.available)) {
                    //TODO: Should distinguish between Sharable/Lime and "regular" presence with 2 event types
                    new PresenceUpdateEvent(user, presence).publish();
                    if(presence instanceof LimePresence) {
                        addBuddyLibrary(user);
                    }
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    new PresenceUpdateEvent(user, presence).publish();
                    if(presence instanceof LimePresence) {
                        removeBuddyLibrary(user);
                    }
                } else {
                    // TODO update UI
                }
            }
        });        
    }

    private void removeBuddyLibrary(User user) {
        String name = user.getName();
        if(name == null) {
            name = user.getId();
        }
        RemoteFileList libraryList = libraryManager.getBuddyLibrary(name);
        BuddyLibrary library = new BuddyLibrary<RemoteFileItem>(name, libraryList);
        navigator.removeNavigablePanel(NavCategory.LIBRARY, library.getName());
        libraryManager.removeBuddyLibrary(name);
    }

    private void addBuddyLibrary(User user) {
        String name = user.getName();
        if(name == null) {
            name = user.getId();
        }
        synchronized (libraryManager) {
            if(!libraryManager.containsBuddyLibrary(name)) {
                libraryManager.addBuddyLibrary(name);  
            }
        }
        BuddyLibrary library = new BuddyLibrary<RemoteFileItem>(name, libraryManager.getBuddyLibrary(name));
        navigator.addNavigablePanel(NavCategory.LIBRARY, library.getName(), library, false);
    }

    public void userUpdated(User user) {
        // TODO fire UserUpdateEvent    
    }

    public void userDeleted(String id) {
        
    }
}

