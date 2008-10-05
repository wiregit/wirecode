package org.limewire.core.impl.library;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.library.FriendLibraryEvent;
import org.limewire.core.api.library.PresenceLibraryEvent;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.io.Address;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PresenceLibraryBrowser implements RegisteringEventListener<FriendLibraryEvent> {
    public static final Log LOG = LogFactory.getLog(PresenceLibraryBrowser.class);

    private final BrowseFactory browseFactory;

    @Inject
    public PresenceLibraryBrowser(BrowseFactory browseFactory) {
        this.browseFactory = browseFactory;
    }

    @Inject
    public void register(ListenerSupport<FriendLibraryEvent> friendRemoteLibraryEventListenerSupport) {
        friendRemoteLibraryEventListenerSupport.addListener(this);
    }

    public void handleEvent(FriendLibraryEvent event) {
        switch (event.getType()) {
            case LIBRARY_ADDED:
            event.getFriendLibrary().addListener(new EventListener<PresenceLibraryEvent>() {
                public void handleEvent(final PresenceLibraryEvent event) {
                    switch (event.getType()) {
                    case LIBRARY_ADDED:
                        Address address = (event.getLibrary().getPresence()).getAddress();
                        LOG.debugf("browsing {0} ...", event.getLibrary().getPresence().getJID());
                        browseFactory.createBrowse(address).start(new BrowseListener() {
                            public void handleBrowseResult(SearchResult searchResult) {
                                LOG.debugf("browse result: {0}, {1}", searchResult.getUrn(), searchResult.getSize());
                                RemoteFileItem file = new CoreRemoteFileItem((RemoteFileDescAdapter)searchResult);
                                event.getLibrary().addFile(file);
                            }
                        });
                    }
                }
            });
        }
    }
}
