package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.core.impl.xmpp.XMPPRemoteFileDescDeserializer;
import org.limewire.io.Address;
import org.limewire.io.IpPortSet;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectivityChangeEvent;
import org.limewire.net.SocketsManager;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.XMPPAddress;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

@Singleton
class PresenceLibraryBrowser implements EventListener<LibraryChangedEvent> {
   
    private static final Log LOG = LogFactory.getLog(PresenceLibraryBrowser.class);

    private final BrowseFactory browseFactory;
    private final RemoteLibraryManager remoteLibraryManager;

    private final SocketsManager socketsManager;
    
    /**
     * Keeps track of libraries that could not be browsed yet, because the local peer didn't have
     * enough connection capabilities.
     */
    private final List<PresenceLibrary> librariesToBrowse = Collections.synchronizedList(new ArrayList<PresenceLibrary>());

    private final XMPPRemoteFileDescDeserializer remoteFileDescDeserializer;

    @Inject
    public PresenceLibraryBrowser(BrowseFactory browseFactory, RemoteLibraryManager remoteLibraryManager,
            SocketsManager socketsManager, XMPPRemoteFileDescDeserializer remoteFileDescDeserializer) {
        this.browseFactory = browseFactory;
        this.remoteLibraryManager = remoteLibraryManager;
        this.socketsManager = socketsManager;
        this.remoteFileDescDeserializer = remoteFileDescDeserializer;
        socketsManager.addListener(new ConnectivityChangeListener());
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
                        
                        new AbstractListEventListener<PresenceLibrary>() {
                            protected void itemAdded(PresenceLibrary presenceLibrary) {
                                FriendPresence friendPresence = presenceLibrary.getPresence();
                                AddressFeature addressFeature = (AddressFeature)friendPresence.getFeature(AddressFeature.ID);
                                if(addressFeature != null) {
                                    Address address = addressFeature.getFeature();
                                    synchronized (librariesToBrowse) {
                                        if (socketsManager.canConnect(address) || socketsManager.canResolve(address)) {
                                            browse(presenceLibrary, friendPresence);
                                        } else {
                                            presenceLibrary.setState(LibraryState.LOADING);
                                            librariesToBrowse.add(presenceLibrary);
                                        }
                                    }
                                }
                            }

                            protected void itemRemoved(PresenceLibrary item) {
                                librariesToBrowse.remove(item);
                            }

                            @Override
                            protected void itemUpdated(PresenceLibrary item) {
                            }
                        }.install(friendLibrary.getPresenceLibraryList());
                    }
                }
            }
        });
    }

    private void browse(final PresenceLibrary presenceLibrary, final FriendPresence friendPresence) {
        presenceLibrary.setState(LibraryState.LOADING);
        LOG.debugf("browsing {0} ...", friendPresence.getPresenceId());
        final Browse browse = browseFactory.createBrowse(friendPresence);
        browse.start(new BrowseListener() {
            public void handleBrowseResult(SearchResult searchResult) {
                LOG.debugf("browse result: {0}, {1}", searchResult.getUrn(), searchResult.getSize());
                RemoteFileDescAdapter remoteFileDescAdapter = (RemoteFileDescAdapter)searchResult;
                if(!friendPresence.getFriend().isAnonymous()) {
                    // copy construct to add injectables and change address to xmpp address 
                    remoteFileDescAdapter = new RemoteFileDescAdapter(remoteFileDescDeserializer.createClone(remoteFileDescAdapter.getRfd(), new XMPPAddress(friendPresence.getPresenceId())), new IpPortSet(remoteFileDescAdapter.getAlts()));
                    remoteFileDescAdapter.setFriendPresence(friendPresence);
                }
                RemoteFileItem file = new CoreRemoteFileItem(remoteFileDescAdapter);
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
    
    /**
     * Is notified of better connection capabilities and iterates over the list of unbrowsable
     * presence libraries to see if they can be browsed now.
     */
    private class ConnectivityChangeListener implements EventListener<ConnectivityChangeEvent> {

        @Override
        public void handleEvent(ConnectivityChangeEvent event) {
            synchronized (librariesToBrowse) {
                // calls in here need to be non-blocking 
                for (Iterator<PresenceLibrary> i = librariesToBrowse.iterator(); i.hasNext();) {
                    PresenceLibrary presenceLibrary = i.next();
                    FriendPresence friendPresence = presenceLibrary.getPresence();
                    AddressFeature addressFeature = (AddressFeature)friendPresence.getFeature(AddressFeature.ID);
                    if(addressFeature != null) {
                        Address address = addressFeature.getFeature();
                        if (socketsManager.canConnect(address) || socketsManager.canResolve(address)) {
                            i.remove();
                            browse(presenceLibrary, friendPresence);
                        }
                    }
                }
            }
        }
    }
}