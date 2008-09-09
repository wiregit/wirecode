package org.limewire.core.impl.library;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.search.SearchResult;
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
                        Address address = ((LimePresence)presence).getAddress();
                        LOG.debugf("browsing {0} ...", presence.getJID());
                        browseFactory.createBrowse(address).start(new BrowseListener() {
                            public void handleBrowseResult(SearchResult searchResult) {
                                LOG.debugf("browse result: {0}, {1}", searchResult.getUrn(), searchResult.getSize());
                                if(!libraryManager.containsBuddyLibrary(user.getName())) {
                                    // TODO locking
                                    libraryManager.addBuddyLibrary(user.getName());
                                    FileList list = libraryManager.getBuddyLibrary(user.getName());
                                    //list.addFile();
                                }
                            }
                        });
                    }
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    //new PresenceUpdateEvent(user, presence).publish();
                }                
            }   
        });
        //TODO: fix this
        Thread t = new Thread(new Runnable(){
            public void run() {
//                if(!libraryManager.containsBuddy(user.getId())) {
                    libraryManager.addBuddy(user.getId());
//                }
            }
        }); t.start();

    }

    private void userUpdated(User source) {
        // TODO
    }

    private void userDeleted(String id) {
        // TODO
    }
}
