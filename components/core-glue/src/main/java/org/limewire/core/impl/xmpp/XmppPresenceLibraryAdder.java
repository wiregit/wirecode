package org.limewire.core.impl.xmpp;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.Presence;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Listens for new presences and for them to have features so they become
 * browsable and can be added to {@link RemoteLibraryManager}.
 */
@Singleton
class XmppPresenceLibraryAdder {
    public static final Log LOG = LogFactory.getLog(XmppPresenceLibraryAdder.class);

    private final RemoteLibraryManager remoteLibraryManager;

    private final LibraryAdderFeatureListener libraryAdderFeatureListener = new LibraryAdderFeatureListener();
    
    @Inject
    public XmppPresenceLibraryAdder(RemoteLibraryManager remoteLibraryManager) {
        this.remoteLibraryManager = remoteLibraryManager;
    }

    @Inject void register(ListenerSupport<FriendPresenceEvent> presenceSupport) {
        presenceSupport.addListener(new EventListener<FriendPresenceEvent>() {
            @Override
            public void handleEvent(FriendPresenceEvent presenceEvent) {
                FriendPresence presence = presenceEvent.getSource();
                switch(presenceEvent.getType()) {
                case ADDED:
                    presence.getFeatureListenerSupport().addListener(libraryAdderFeatureListener);
                    break;
                case REMOVED:
                    presence.getFeatureListenerSupport().removeListener(libraryAdderFeatureListener);
                    break;
                }
            }
        });
    }
    
    private class LibraryAdderFeatureListener implements EventListener<FeatureEvent> {                        
        @BlockingEvent
        public void handleEvent(FeatureEvent featureEvent) {
            Presence presence = featureEvent.getSource();
            if(featureEvent.getType() == FeatureEvent.Type.ADDED) {
                if(presence.hasFeatures(AddressFeature.ID, AuthTokenFeature.ID)) {
                    remoteLibraryManager.addPresenceLibrary(presence);
                }
            } else if(featureEvent.getType() == FeatureEvent.Type.REMOVED){
                if(!presence.hasFeatures(AddressFeature.ID, AuthTokenFeature.ID)) {
                    remoteLibraryManager.removePresenceLibrary(presence);
                }
            }
        }
    }
}