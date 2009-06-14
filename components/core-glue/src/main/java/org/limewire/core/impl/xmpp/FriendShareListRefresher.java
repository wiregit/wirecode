package org.limewire.core.impl.xmpp;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.collection.Periodic;
import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifierFeature;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPFriend;
import org.limewire.xmpp.api.client.XMPPService;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.library.LibraryStatusEvent;

/**
 * Sends library changed messages to friends when:<BR>
 * 1) File manager is finished loading<BR>
 * OR<BR>
 * 2) A friend's sharelist changes
 */

@Singleton
class FriendShareListRefresher implements RegisteringEventListener<FriendShareListEvent> {

    private final BrowseTracker tracker;
    private final XMPPService xmppService;
    private final ScheduledExecutorService scheduledExecutorService;

    private final Map<String, LibraryChangedSender> listeners;
    
    // Package private for testing
    final AtomicBoolean fileManagerLoaded = new AtomicBoolean(false);

    @Inject
    FriendShareListRefresher(BrowseTracker tracker,
                             XMPPService xmppService,
                             @Named("backgroundExecutor")ScheduledExecutorService scheduledExecutorService) {
        this.tracker = tracker;
        this.xmppService = xmppService;
        this.scheduledExecutorService = scheduledExecutorService;
        listeners = new ConcurrentHashMap<String, LibraryChangedSender>();
    }

    @Inject
    public void register(Library library) {
        library.addManagedListStatusListener(new FinishedLoadingListener());
    }

    @Inject
    public void register(ListenerSupport<FriendShareListEvent> friendShareListEventListenerSupport) {
        friendShareListEventListenerSupport.addListener(this);
    }

    public void handleEvent(final FriendShareListEvent event) {
        if(event.getType() == FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED) {
            LibraryChangedSender listener = new LibraryChangedSender(event.getFriend());
            listener.scheduleSendRefreshCheck();
            listeners.put(event.getFriend().getId(), listener);
            event.getFileList().getModel().addListEventListener(listener);
        } else if(event.getType() == FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED) {
            event.getFileList().getModel().removeListEventListener(listeners.remove(event.getFriend().getId()));
        }
    }
    
    class FinishedLoadingListener implements EventListener<LibraryStatusEvent> {
        @SuppressWarnings("unchecked")
        @BlockingEvent
        public void handleEvent(LibraryStatusEvent evt) {
            if(evt.getType() == LibraryStatusEvent.Type.LOAD_COMPLETE) {
                fileManagerLoaded.set(true);  
                XMPPConnection connection = xmppService.getActiveConnection();
                if(connection != null) {
                    Collection<XMPPFriend> friends = connection.getFriends();
                    for(Friend friend : friends) {
                        tracker.sentRefresh(friend.getId());
                        Map<String, FriendPresence> presences = friend.getFriendPresences();
                        for(FriendPresence presence : presences.values()) {
                            Feature<LibraryChangedNotifier> notifier = presence.getFeature(LibraryChangedNotifierFeature.ID);                                
                            if(notifier != null) {
                                notifier.getFeature().sendLibraryRefresh();
                            }
                        }
                    }
                }
            }
        }            
    }
    
    class LibraryChangedSender implements ListEventListener<LocalFileItem> {
        
        private final Friend friend;
        
        private final Periodic libraryRefreshPeriodic;
        
        LibraryChangedSender(Friend friend){
            this.friend = friend;
            this.libraryRefreshPeriodic = new Periodic(new ScheduledLibraryRefreshSender(), scheduledExecutorService);
        }        
        
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            if(fileManagerLoaded.get()) {
                libraryRefreshPeriodic.rescheduleIfLater(5000);                                
            }
        }        
        
        /**
         * Schedules an immediate check if a library refresh should be sent
         * to the friend.
         */
        private void scheduleSendRefreshCheck() {
            libraryRefreshPeriodic.rescheduleIfLater(0);
        }
    
        class ScheduledLibraryRefreshSender implements Runnable {
    
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                BrowseTracker browseTracker = FriendShareListRefresher.this.tracker;
                Date lastBrowseTime = browseTracker.lastBrowseTime(friend.getId());
                Date lastRefreshTime = browseTracker.lastRefreshTime(friend.getId());
                if(lastBrowseTime != null && (lastRefreshTime == null || lastBrowseTime.after(lastRefreshTime))) {
                    browseTracker.sentRefresh(friend.getId());
                    Map<String,FriendPresence> presences = friend.getFriendPresences();
                    for(FriendPresence presence : presences.values()) {
                        Feature<LibraryChangedNotifier> notifier = presence.getFeature(LibraryChangedNotifierFeature.ID);
                        if(notifier != null) {
                            notifier.getFeature().sendLibraryRefresh();
                        }
                    }
                }
            }
        }
    }
}
