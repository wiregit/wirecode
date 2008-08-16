package org.limewire.ui.swing.friends;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.address.Address;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RosterListenerImpl implements RegisteringEventListener<RosterEvent> {

    private final BrowseFactory browseFactory;
    //private final StatusPanel.BrowseAction browseAction;

    @Inject
    public RosterListenerImpl(BrowseFactory browseFactory/*, StatusPanel.BrowseAction browseAction*/) {
        this.browseFactory = browseFactory;
        //this.browseAction = browseAction;
    }
    
    @Inject
    public void register(XMPPService xmppService) {
        for(XMPPConnection connection : xmppService.getConnections()) {
            connection.addRosterListener(this);
        }
    }

    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }
    
    public void handleEvent(RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            userAdded(event.getSource());
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            userDeleted(event.getSource().getId());
        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
            userUpdated(event.getSource());
        }
    }

    public void userAdded(final User user) {
        user.addPresenceListener(new PresenceListener() {
            public void presenceChanged(final Presence presence) {
                if(presence.getType().equals(Presence.Type.available)) {
                    if(presence instanceof LimePresence) {
                        // TODO update UI
                        Address address = ((LimePresence)presence).getAddress();
                        //browseAction.setAddress(address);
                        browseFactory.createBrowse(address).start(new BrowseListener() {
                            public void handleBrowseResult(SearchResult searchResult) {
                                ((LimePresence)presence).addFile(new FileMetaDataAdapter(searchResult));
                            }
                        });
                    } else {
                        // TODO update UI
                    }
                    //TODO: Should distinguish between Sharable/Lime and "regular" presence with 2 event types
                    new PresenceUpdateEvent(user, presence).publish();
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    if(presence instanceof LimePresence) {
                        // TODO update UI
                    } else {
                        // TODO update UI
                    }
                    new PresenceUpdateEvent(user, presence).publish();
                } else {
                    // TODO update UI
                }
            }
        });
    }

    public void userUpdated(User user) {
        
    }

    public void userDeleted(String id) {
        
    }

    private class FileMetaDataAdapter implements FileMetaData {
        private SearchResult searchResult;
        public FileMetaDataAdapter(SearchResult searchResult) {
            this.searchResult = searchResult;
        }

        public String getId() {
            return searchResult.getUrn();
        }

        public String getName() {
            return ""; // TODO
        }

        public long getSize() {
            return searchResult.getSize();
        }

        public String getDescription() {
            return searchResult.getDescription();
        }

        public long getIndex() {
            return -1; // TODO
        }

        public Map<String, String> getMetaData() {
            return null;  // TODO
        }

        public Set<URI> getURIs() throws URISyntaxException {
            return null; // TODO
        }

        public Date getCreateTime() {
            return new Date(); // TODO
        }

        public String toXML() {
            return null; // TODO
        }
    }
}

