package org.limewire.core.impl.xmpp;

import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.listener.BlockingEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class XmppPresenceLibraryAdder implements RegisteringEventListener<RosterEvent> {
    public static final Log LOG = LogFactory.getLog(XmppPresenceLibraryAdder.class);

    private final RemoteLibraryManager remoteLibraryManager;

    @Inject
    public XmppPresenceLibraryAdder(RemoteLibraryManager remoteLibraryManager) {
        this.remoteLibraryManager = remoteLibraryManager;
    }

    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }

    public void handleEvent(RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            userAdded(event.getSource());
        }
    }    

    private void userAdded(User user) {
        user.addPresenceListener(new EventListener<PresenceEvent>() {
            
            public void handleEvent(final PresenceEvent presenceEvent) {
                final Presence presence = presenceEvent.getSource();
                if(presence.getType().equals(Presence.Type.available)) {
                    presence.getFeatureListenerSupport().addListener(new EventListener<FeatureEvent>() {
                        
                        @BlockingEvent
                        public void handleEvent(FeatureEvent event) {
                            if(event.getType().equals(Feature.EventType.FEATURE_ADDED)) {
                                if(presence.hasFeatures(AddressFeature.ID, AuthTokenFeature.ID)) {
                                    remoteLibraryManager.addPresenceLibrary(presence);
                                }
                            } else if(event.getType().equals(Feature.EventType.FEATURE_REMOVED)){
                                if(!presence.hasFeatures(AddressFeature.ID, AuthTokenFeature.ID)) {
                                    remoteLibraryManager.removePresenceLibrary(presence);
                                }
                            }
                        }
                    });
                }
            }
        });
    }
}