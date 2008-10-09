package org.limewire.core.impl.xmpp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class FriendShareListRefresher {

    private static final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<String, User>();

    @Singleton
    static class RosterEventListenerImpl implements RegisteringEventListener<RosterEvent> {

        @Inject
        public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
            rosterEventListenerSupport.addListener(this);
        }

        public void handleEvent(RosterEvent event) {
            if(event.getType() == User.EventType.USER_ADDED) {
                users.put(event.getSource().getId(), event.getSource());
            }
        }
    }

    @Singleton
    static class FriendShareListEventImpl implements RegisteringEventListener<FriendShareListEvent> {

        @Inject
        public void register(ListenerSupport<FriendShareListEvent> friendShareListEventListenerSupport) {
            friendShareListEventListenerSupport.addListener(this);
        }

        public void handleEvent(final FriendShareListEvent event) {
            if(event.getType() == FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED) {
                event.getFileList().getModel().addListEventListener(new ListEventListener<LocalFileItem>() {
                    public void listChanged(ListEvent<LocalFileItem> listChanges) {
                        User user = users.get(event.getFriend().getId());
                        if(user != null) {
                            Map<String,Presence> presences = user.getPresences();
                            for(Presence presence : presences.values()) {
                                if(presence instanceof LimePresence) {
                                    ((LimePresence)presence).sendLibraryRefresh();
                                }
                            }
                        }
                    }
                });
            }
        }
    }
}
