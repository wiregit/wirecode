package org.limewire.core.impl.xmpp;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifierFeature;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.xmpp.api.client.Presence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.ManagedListStatusEvent;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class FriendShareListRefresher {

    @Singleton
    static class FriendShareListEventImpl implements RegisteringEventListener<FriendShareListEvent> {

        private final AtomicBoolean FILE_MANAGER_LOADED = new AtomicBoolean(false);

        @Inject
        public void register(ListenerSupport<FriendShareListEvent> friendShareListEventListenerSupport) {
            friendShareListEventListenerSupport.addListener(this);
        }

        @Inject
        public void register(FileManager fileManager) {
            fileManager.getManagedFileList().addManagedListStatusListener(new EventListener<ManagedListStatusEvent>() {
                public void handleEvent(ManagedListStatusEvent evt) {
                    if(evt.getType() == ManagedListStatusEvent.Type.LOAD_COMPLETE) {
                        FILE_MANAGER_LOADED.set(true);
                    }
                }
            });
        }

        // TODO: Sharelists are going to change a lot, in rapid succession --
        //       We need to queue up the changes to avoid sending a zillion
        //       library refresh packets.
        public void handleEvent(final FriendShareListEvent event) {
            if(event.getType() == FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED) {
                event.getFileList().getModel().addListEventListener(new ListEventListener<LocalFileItem>() {
                    @SuppressWarnings("unchecked")
                    public void listChanged(ListEvent<LocalFileItem> listChanges) {
                        if(FILE_MANAGER_LOADED.get()) {
                            Map<String,Presence> presences = event.getFriend().getPresences();
                            for(Presence presence : presences.values()) {
                                Feature<LibraryChangedNotifier> notifier = presence.getFeature(LibraryChangedNotifierFeature.ID);
                                if(notifier != null) {
                                    notifier.getFeature().sendLibraryRefresh();
                                }
                            }
                        }
                    }
                });
            }
        }
    }
}
