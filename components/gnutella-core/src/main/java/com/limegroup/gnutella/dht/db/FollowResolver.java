package com.limegroup.gnutella.dht.db;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.impl.friend.GnutellaPresence.GnutellaPresenceWithGuid;
import org.limewire.core.settings.SearchSettings;
import org.limewire.inject.EagerSingleton;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.search.FriendPresenceActions;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;


@EagerSingleton
public class FollowResolver {
    
    private static final Log LOG = LogFactory.getLog(FollowResolver.class);

    private final DHTManager dhtManager;
    private final AddressFinder addressFinder;
    private final FriendPresenceActions friendPresenceActions;

    private final SearchServices searchServices;
    
    private final Set<GUID> successful = Collections.synchronizedSet(new HashSet<GUID>());

    private final RemoteLibraryManager remoteLibraryManager;
    
    @Inject
    public FollowResolver(DHTManager dhtManager, @DHTAddressFinder AddressFinder addressFinder,
            FriendPresenceActions friendPresenceActions, SearchServices searchServices,
            RemoteLibraryManager remoteLibraryManager) {
        this.dhtManager = dhtManager;
        this.addressFinder = addressFinder;
        this.friendPresenceActions = friendPresenceActions;
        this.searchServices = searchServices;
        this.remoteLibraryManager = remoteLibraryManager;
        this.dhtManager.addEventListener(new FollowDHTEventListener());
        SearchSettings.FOLLOWEES.addSettingListener(new FollowSettingsListener());
    }
    
    @Inject
    void register(ServiceScheduler scheduler, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        scheduler.scheduleWithFixedDelay("follow", new Runnable() {
            @Override
            public void run() {
                if (dhtManager.isMemberOfDHT()) {
                    lookupFollowees();
                }
            }
        }, 30, 30, TimeUnit.SECONDS, backgroundExecutor);
    }
    
    private void lookupFollowees() {
        Queue<GUID> followees = new LinkedList<GUID>();
        for (String guidString : SearchSettings.FOLLOWEES.get()) {
            GUID guid = new GUID(guidString);
            if (!successful.contains(guid)) {
                followees.add(guid);
            }
        }
        LOG.debugf("lookup followees: {0}", followees);
        lookup(followees);
    }
    
    private void lookup(final Queue<GUID> followees) {
        if (followees.isEmpty()) {
            return;
        }
        final GUID guid = followees.remove();
        LOG.debugf("lookup: {0}", guid);
        addressFinder.search(guid).addFutureListener(new EventListener<FutureEvent<Address>>() {
            @Override
            public void handleEvent(FutureEvent<Address> event) {
                LOG.debugf("address event: {0}", event);
                switch (event.getType()) {
                case SUCCESS:
                    Address address = event.getResult();
                    LOG.debugf("browsing: {0}", address);
                    tryToBrowse(address, guid);
                    lookup(followees);
                    break;
                case CANCELLED:
                case EXCEPTION:
                    if (dhtManager.isMemberOfDHT()) {
                        lookup(followees);
                    } else {
                        LOG.debug("dht down");
                    }
                }
            }
        });
    }
    
    private void tryToBrowse(Address address, final GUID guid) {
        final GnutellaPresenceWithGuid presence = new GnutellaPresenceWithGuid(address, guid.bytes());
        remoteLibraryManager.addPresenceLibrary(presence);
//        searchServices.doAsynchronousBrowseHost(presence, new GUID(), new BrowseListener() {
//            @Override
//            public void handleBrowseResult(SearchResult searchResult) {
//            }
//            @Override
//            public void browseFinished(boolean success) {
//                LOG.debugf("browse succeeded {0} for {1}", success, presence);
//                if (success) {
//                    successful.add(guid);
//                    friendPresenceActions.viewLibrariesOf(Collections.<FriendPresence>singleton(presence));
//                }
//            }
//        });
    }
    
    private class FollowSettingsListener implements SettingListener {
        @Override
        public void settingChanged(SettingEvent evt) {
            // nothing for now
        }
    }
    
    private class FollowDHTEventListener implements DHTEventListener {

        @Override
        public void handleDHTEvent(DHTEvent evt) {
            LOG.debugf("dht event: {0}", evt);
            switch (evt.getType()) {
            case CONNECTED:
                lookupFollowees();
                break;
            case STOPPED:
                break;
            }
        }
        
    }
    
}
