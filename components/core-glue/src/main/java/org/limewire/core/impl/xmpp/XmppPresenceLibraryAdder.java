package org.limewire.core.impl.xmpp;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class XmppPresenceLibraryAdder {
    public static final Log LOG = LogFactory.getLog(XmppPresenceLibraryAdder.class);

    private final RemoteLibraryManager remoteLibraryManager;

    @Inject
    public XmppPresenceLibraryAdder(RemoteLibraryManager remoteLibraryManager) {
        this.remoteLibraryManager = remoteLibraryManager;
    }

    @Inject void register(ListenerSupport<FriendPresenceEvent> presenceSupport) {
        presenceSupport.addListener(new EventListener<FriendPresenceEvent>() {
            @Override
            public void handleEvent(FriendPresenceEvent presenceEvent) {
                switch(presenceEvent.getType()) {
                case ADDED:
                    final FriendPresence presence = presenceEvent.getSource();
                    presence.getFeatureListenerSupport().addListener(new EventListener<FeatureEvent>() {                        
                        @BlockingEvent
                        public void handleEvent(FeatureEvent featureEvent) {
                            if(featureEvent.getType().equals(Feature.EventType.FEATURE_ADDED)) {
                                if(presence.hasFeatures(AddressFeature.ID, AuthTokenFeature.ID)) {
                                    remoteLibraryManager.addPresenceLibrary(presence);
                                }
                            } else if(featureEvent.getType().equals(Feature.EventType.FEATURE_REMOVED)){
                                if(!presence.hasFeatures(AddressFeature.ID, AuthTokenFeature.ID)) {
                                    remoteLibraryManager.removePresenceLibrary(presence);
                                }
                            }
                        }
                    });
                    break;
                }
            }
        });
    }
}