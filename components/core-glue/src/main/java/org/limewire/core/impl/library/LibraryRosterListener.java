package org.limewire.core.impl.library;

import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteFileList;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.Address;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryRosterListener implements RegisteringEventListener<RosterEvent> {
    public static final Log LOG = LogFactory.getLog(LibraryRosterListener.class);
    
    private final LibraryManager libraryManager;
    private final BrowseFactory browseFactory;

    @Inject
    public LibraryRosterListener(BrowseFactory browseFactory, LibraryManager libraryManager) {
        this.browseFactory = browseFactory;
        this.libraryManager = libraryManager;
    }

    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }

    public void handleEvent(RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            userAdded(event.getSource());
        } // else if(event.getType().equals(User.EventType.USER_REMOVED)) {
//            userDeleted(event.getSource().getId());
//        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
//            userUpdated(event.getSource());
//        }
    }
    
    public void userAdded(final User user) {
        user.addPresenceListener(new PresenceListener() {
            private AtomicBoolean libraryExists = new AtomicBoolean(false);
            public void presenceChanged(final Presence presence) {
                if(presence.getType().equals(Presence.Type.available)) {                    
                    if(presence instanceof LimePresence) {
                        if(!libraryExists.getAndSet(true)) {
                            final RemoteFileList list = libraryManager.getOrCreateFriendLibrary(user);
                            Address address = ((LimePresence)presence).getAddress();
                            LOG.debugf("browsing {0} ...", presence.getJID());
                            browseFactory.createBrowse(address).start(new BrowseListener() {
                                public void handleBrowseResult(SearchResult searchResult) {
                                    LOG.debugf("browse result: {0}, {1}, libraryExists {2}", searchResult.getUrn(), searchResult.getSize(), libraryExists);
                                    if(libraryExists.get()) {
                                        RemoteFileItem file = new CoreRemoteFileItem((RemoteFileDescAdapter)searchResult);
                                        list.addFile(file);
                                    }
                                }
                            });
                        }
                    }
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    if(libraryExists.getAndSet(false)) {
                        libraryManager.removeFriendLibrary(user);
                    }
                }                
            }   
        });
    }
}
