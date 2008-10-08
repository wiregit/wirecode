package org.limewire.core.impl.library;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.io.Address;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PresenceLibraryBrowser implements RegisteringEventListener<RosterEvent> {
    public static final Log LOG = LogFactory.getLog(PresenceLibraryBrowser.class);

    private final RemoteLibraryManager remoteLibraryManager;
    private final BrowseFactory browseFactory;

    @Inject
    public PresenceLibraryBrowser(BrowseFactory browseFactory, RemoteLibraryManager remoteLibraryManager) {
        this.browseFactory = browseFactory;
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

    private void userAdded(final User user) {
        user.addPresenceListener(new PresenceListener() {            
            public void presenceChanged(final Presence presence) {
                if(presence instanceof FriendPresence) {
                    FriendPresence fPresence = (FriendPresence)presence;
                    if(presence.getType().equals(Presence.Type.available)) {
                        PresenceLibrary library = remoteLibraryManager.addPresenceLibrary(user, fPresence);
                        browse(fPresence, library);
                    } else if(presence.getType().equals(Presence.Type.unavailable)) {
                        remoteLibraryManager.removePresenceLibrary(user, fPresence);
                    }
                }
            }
        });
    }
    
    private void browse(final FriendPresence presence, final PresenceLibrary library) {
        Address address = presence.getPresenceAddress();
        library.setState(LibraryState.LOADING);
        LOG.debugf("browsing {0} ...", presence.getPresenceId());
        browseFactory.createBrowse(address).start(new BrowseListener() {
            public void handleBrowseResult(SearchResult searchResult) {
                LOG.debugf("browse result: {0}, {1}", searchResult.getUrn(), searchResult.getSize());
                RemoteFileItem file = new CoreRemoteFileItem((RemoteFileDescAdapter)searchResult);
                library.addFile(file);
            }
            @Override
            public void browseFinished(boolean success) {
                if(success) {
                    library.setState(LibraryState.LOADED);
                } else {
                    library.setState(LibraryState.FAILED_TO_LOAD);
                }
            }
        });
    }
}
