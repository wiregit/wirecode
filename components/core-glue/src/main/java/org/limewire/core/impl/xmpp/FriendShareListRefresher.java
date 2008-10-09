package org.limewire.core.impl.xmpp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.limegroup.gnutella.FileEventListener;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;

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

        private static final AtomicBoolean FILE_MANAGER_LOADED = new AtomicBoolean(false);

        @Inject
        public void register(ListenerSupport<FriendShareListEvent> friendShareListEventListenerSupport) {
            friendShareListEventListenerSupport.addListener(this);
        }

        @Inject
        public void register(FileManager fileManager) {
            fileManager.addFileEventListener(new FileEventListener() {
                public void handleFileEvent(FileManagerEvent evt) {
                    if(evt.getType() == FileManagerEvent.Type.FILEMANAGER_LOAD_COMPLETE) {
                        FILE_MANAGER_LOADED.set(true);
                    }
                }
            });
        }

        public void handleEvent(final FriendShareListEvent event) {
            if(event.getType() == FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED) {
                event.getFileList().getModel().addListEventListener(new ListEventListener<LocalFileItem>() {
                    public void listChanged(ListEvent<LocalFileItem> listChanges) {
                        if(FILE_MANAGER_LOADED.get()) {
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
                    }
                });
            }
        }
    }
}
