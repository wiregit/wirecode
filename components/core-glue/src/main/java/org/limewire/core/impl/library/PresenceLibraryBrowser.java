package org.limewire.core.impl.library;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.io.Address;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PresenceLibraryBrowser {
    public static final Log LOG = LogFactory.getLog(PresenceLibraryBrowser.class);

    private final BrowseFactory browseFactory;

    @Inject
    public PresenceLibraryBrowser(BrowseFactory browseFactory) {
        this.browseFactory = browseFactory;
    }

    @Inject
    public void register(RemoteLibraryManager remoteLibraryManager) {
        remoteLibraryManager.getFriendLibraryList().addListEventListener(new ListEventListener<FriendLibrary>() {
            @Override
            public void listChanged(ListEvent<FriendLibrary> listChanges) {
                while(listChanges.next()) {
                    if(listChanges.getType() == ListEvent.INSERT) {
                        FriendLibrary friendLibrary = listChanges.getSourceList().get(listChanges.getIndex());
                        friendLibrary.getPresenceLibraryList().addListEventListener(new ListEventListener<PresenceLibrary>() {
                            @Override
                            public void listChanged(ListEvent<PresenceLibrary> listChanges) {
                                while(listChanges.next()) {
                                    if(listChanges.getType() == ListEvent.INSERT) {
                                        final PresenceLibrary presenceLibrary = listChanges.getSourceList().get(listChanges.getIndex());
                                        Address address = presenceLibrary.getPresence().getAddress();
                                        presenceLibrary.setState(LibraryState.LOADING);
                                        LOG.debugf("browsing {0} ...", presenceLibrary.getPresence().getJID());
                                        browseFactory.createBrowse(address).start(new BrowseListener() {
                                            public void handleBrowseResult(SearchResult searchResult) {
                                                LOG.debugf("browse result: {0}, {1}", searchResult.getUrn(), searchResult.getSize());
                                                RemoteFileItem file = new CoreRemoteFileItem((RemoteFileDescAdapter)searchResult);
                                                presenceLibrary.addFile(file);
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
                            }
                        });
                    }
                }
            }
        });
    }
}
