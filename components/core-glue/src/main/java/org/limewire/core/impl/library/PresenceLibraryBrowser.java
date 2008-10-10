package org.limewire.core.impl.library;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.io.Address;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.LibraryChangedEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

@Singleton
class PresenceLibraryBrowser implements EventListener<LibraryChangedEvent> {
    public static final Log LOG = LogFactory.getLog(PresenceLibraryBrowser.class);

    private final BrowseFactory browseFactory;
    private final RemoteLibraryManager remoteLibraryManager;

    @Inject
    public PresenceLibraryBrowser(BrowseFactory browseFactory, RemoteLibraryManager remoteLibraryManager) {
        this.browseFactory = browseFactory;
        this.remoteLibraryManager = remoteLibraryManager;
    }

    @Inject void register(ListenerSupport<LibraryChangedEvent> listenerSupport) {
        listenerSupport.addListener(this);
    }

    public void handleEvent(LibraryChangedEvent event) {
        remoteLibraryManager.removePresenceLibrary(event.getSource());
        remoteLibraryManager.addPresenceLibrary(event.getSource());
    }

    @Inject
    public void addListener(RemoteLibraryManager remoteLibraryManager) {
        remoteLibraryManager.getFriendLibraryList().addListEventListener(new ListEventListener<FriendLibrary>() {
            @Override
            public void listChanged(ListEvent<FriendLibrary> listChanges) {
                while(listChanges.next()) {
                    if(listChanges.getType() == ListEvent.INSERT) {
                        final FriendLibrary friendLibrary = listChanges.getSourceList().get(listChanges.getIndex());
                        friendLibrary.getPresenceLibraryList().addListEventListener(new ListEventListener<PresenceLibrary>() {
                            @Override
                            public void listChanged(ListEvent<PresenceLibrary> listChanges) {
                                while(listChanges.next()) {
                                    if(listChanges.getType() == ListEvent.INSERT) {
                                        final PresenceLibrary presenceLibrary = listChanges.getSourceList().get(listChanges.getIndex());
                                        final FriendPresence friendPresence = presenceLibrary.getPresence();
                                        browse(presenceLibrary, friendPresence);
                                    }
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    private void browse(final PresenceLibrary presenceLibrary, final FriendPresence friendPresence) {
        Address address = friendPresence.getPresenceAddress();
        presenceLibrary.setState(LibraryState.LOADING);
        LOG.debugf("browsing {0} ...", friendPresence.getPresenceId());
        browseFactory.createBrowse(address).start(new BrowseListener() {
            public void handleBrowseResult(SearchResult searchResult) {
                LOG.debugf("browse result: {0}, {1}", searchResult.getUrn(), searchResult.getSize());
                RemoteFileDescAdapter remoteFileDescAdapter = (RemoteFileDescAdapter)searchResult;
                if(!friendPresence.getFriend().isAnonymous()) {
                    remoteFileDescAdapter.setFriendPresence(friendPresence);
                }
                RemoteFileItem file = new CoreRemoteFileItem(remoteFileDescAdapter);
                if(file.getName() != null) {
                    presenceLibrary.addFile(file);
                }
            }
            @Override
            public void browseFinished(boolean success) {
                if(success) {
                    presenceLibrary.setState(LibraryState.LOADED);
                } else {
                    presenceLibrary.setState(LibraryState.FAILED_TO_LOAD);
                }
            }
        });
    }
}
