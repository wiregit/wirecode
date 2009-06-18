package org.limewire.core.impl.xmpp;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.collection.Periodic;
import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.api.feature.LibraryChangedNotifier;
import org.limewire.friend.impl.feature.LibraryChangedNotifierFeature;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.ManagedListStatusEvent;

/**
 * Sends library changed messages to friends when:<BR>
 * 1) File manager is finished loading<BR>
 * OR<BR>
 * 2) A friend's sharelist changes
 */
@Singleton
class FriendShareListRefresher implements RegisteringEventListener<FriendShareListEvent> {

    private static final Log LOG = LogFactory.getLog(FriendShareListRefresher.class);
    
    private final BrowseTracker tracker;
    private final EventBean<FriendConnectionEvent> connectionEventBean;
    private final ScheduledExecutorService scheduledExecutorService;

    private final Map<String, LibraryChangedSender> listeners;
    
    // Package private for testing
    final AtomicBoolean fileManagerLoaded = new AtomicBoolean(false);

    @Inject
    FriendShareListRefresher(BrowseTracker tracker,
                             EventBean<FriendConnectionEvent> connectionEventBean,
                             @Named("backgroundExecutor")ScheduledExecutorService scheduledExecutorService) {
        this.tracker = tracker;
        this.connectionEventBean = connectionEventBean;
        this.scheduledExecutorService = scheduledExecutorService;
        listeners = new ConcurrentHashMap<String, LibraryChangedSender>();
    }

    @Inject
    public void register(FileManager fileManager) {
        fileManager.getManagedFileList().addManagedListStatusListener(new FinishedLoadingListener());
    }

    @Inject
    public void register(ListenerSupport<FriendShareListEvent> friendShareListEventListenerSupport) {
        friendShareListEventListenerSupport.addListener(this);
    }

    public void handleEvent(final FriendShareListEvent event) {
        if(event.getType() == FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED) {
            LOG.debugf("friend share list added {0}", event);
            LibraryChangedSender listener = new LibraryChangedSender(event.getFriend());
            listener.scheduleSendRefreshCheck();
            listeners.put(event.getFriend().getId(), listener);
            event.getFileList().getModel().addListEventListener(listener);
        } else if(event.getType() == FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED) {
            LOG.debugf("friend share list removed {0}", event);
            event.getFileList().getModel().removeListEventListener(listeners.remove(event.getFriend().getId()));
        }
    }
    
    class FinishedLoadingListener implements EventListener<ManagedListStatusEvent> {
        @SuppressWarnings("unchecked")
        @BlockingEvent
        public void handleEvent(ManagedListStatusEvent evt) {
            if(evt.getType() == ManagedListStatusEvent.Type.LOAD_COMPLETE) {
                fileManagerLoaded.set(true);
                FriendConnectionEvent connection = connectionEventBean.getLastEvent();
                if(connection != null && connection.getType() == FriendConnectionEvent.Type.CONNECTED) {
                    for(Friend friend : connection.getSource().getFriends()) {
                        tracker.sentRefresh(friend.getId());
                        Map<String, FriendPresence> presences = friend.getPresences();
                        for(FriendPresence presence : presences.values()) {
                            Feature<LibraryChangedNotifier> notifier = presence.getFeature(LibraryChangedNotifierFeature.ID);                                
                            if(notifier != null) {
                                FeatureTransport<LibraryChangedNotifier> notifierFeatureTransport = presence.getTransport(LibraryChangedNotifierFeature.class);
                                try {
                                    notifierFeatureTransport.sendFeature(presence, notifier.getFeature());
                                } catch (FriendException e) {
                                    LOG.error("library changed notification failed", e);
                                }
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
                    LOG.debugf("send scheduled refresh to {0}", friend);
                    browseTracker.sentRefresh(friend.getId());
                    Map<String,FriendPresence> presences = friend.getPresences();
                    for(FriendPresence presence : presences.values()) {
                        Feature<LibraryChangedNotifier> notifier = presence.getFeature(LibraryChangedNotifierFeature.ID);
                        if(notifier != null) {
                            LOG.debugf("sending refresh to presence: {0}", presence);
                            FeatureTransport<LibraryChangedNotifier> notifierFeatureTransport = presence.getTransport(LibraryChangedNotifierFeature.class);
                            try {
                                notifierFeatureTransport.sendFeature(presence, notifier.getFeature());
                            } catch (FriendException e) {
                                LOG.error("library changed notification failed", e);
                            }
                        }
                    }
                }
            }
        }
    }
}
