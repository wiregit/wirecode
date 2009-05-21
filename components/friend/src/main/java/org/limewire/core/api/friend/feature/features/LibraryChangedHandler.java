package org.limewire.core.api.friend.feature.features;

import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.client.LibraryChanged;
import org.limewire.core.api.friend.client.LibraryChangedEvent;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;
import org.limewire.xmpp.client.impl.features.LibraryChangedNotifierFeatureInitializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryChangedHandler implements  FeatureTransport.Handler<LibraryChangedNotifier> {
    private final EventBean<FriendConnectionEvent> friendConnectionEvent;
    private final EventBroadcaster<LibraryChangedEvent> libChangedBroadcaster;

    @Inject
    public LibraryChangedHandler(EventBean<FriendConnectionEvent> friendConnectionEvent,
                                 EventBroadcaster<LibraryChangedEvent> libChangedBroadcaster,
                                 FeatureRegistry featureRegistry) {
        this.friendConnectionEvent = friendConnectionEvent;
        this.libChangedBroadcaster = libChangedBroadcaster;
        new LibraryChangedNotifierFeatureInitializer().register(featureRegistry);
    }

    @Override
    public void featureReceived(String from, LibraryChangedNotifier feature) {
        FriendConnectionEvent event = friendConnectionEvent.getLastEvent();
        if(event != null) {
            FriendConnection connection = event.getSource();
            Friend friend = connection.getFriend(StringUtils.parseBareAddress(from));
            if(friend != null) {
                FriendPresence friendPresence = friend.getPresences().get(from);
                if(friendPresence != null) {
                    libChangedBroadcaster.broadcast(new LibraryChangedEvent(friendPresence, LibraryChanged.LIBRARY_CHANGED));     
                } 
            }
        }
        
    }
}
