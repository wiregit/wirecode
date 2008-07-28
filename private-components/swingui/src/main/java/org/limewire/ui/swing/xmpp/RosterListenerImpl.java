package org.limewire.ui.swing.xmpp;

import java.util.ArrayList;
import java.util.HashMap;

import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.net.address.Address;
import org.limewire.xmpp.client.service.LimePresence;
import org.limewire.xmpp.client.service.Presence;
import org.limewire.xmpp.client.service.PresenceListener;
import org.limewire.xmpp.client.service.RosterListener;
import org.limewire.xmpp.client.service.User;
import org.limewire.xmpp.client.service.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class RosterListenerImpl implements RosterListener {

    private final BrowseFactory browseFactory;
    
    private HashMap<String, ArrayList<Presence>> roster = new HashMap<String, ArrayList<Presence>>();
    private final IncomingChatListenerImpl listener = new IncomingChatListenerImpl();

    @Inject
    public RosterListenerImpl(BrowseFactory browseFactory) {
        this.browseFactory = browseFactory;
    }
    
    @Inject
    public void register(XMPPService xmppService) {
        xmppService.register(this);
    }

    public void userAdded(User user) {
        if(roster.get(user.getId()) == null) {
            roster.put(user.getId(), new ArrayList<Presence>());
        }
        final String name = user.getName();
        user.addPresenceListener(new PresenceListener() {
            public void presenceChanged(Presence presence) {
                String id = StringUtils.parseBareAddress(presence.getJID());
                if(presence.getType().equals(Presence.Type.available)) {
                    if(roster.get(id) == null) {
                        roster.put(id, new ArrayList<Presence>());
                    }
                    roster.get(id).add(presence);
                    presence.setIncomingChatListener(listener);
                    if(presence instanceof LimePresence) {
                        // TODO update UI
                        Address address = ((LimePresence)presence).getAddress();
                        browseFactory.createBrowse(address).start(new BrowseListener() {
                            public void handleBrowseResult(SearchResult searchResult) {
                                // TODO update UI
                            }
                        });
                    } else {
                        // TODO update UI
                    }
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    if(roster.get(id) == null) {
                        roster.put(id, new ArrayList<Presence>());
                    }
                    remove(id, presence);
                    if(presence instanceof LimePresence) {
                        // TODO update UI
                    } else {
                        // TODO update UI
                    }
                } else {
                    // TODO update UI
                }
            }
        });
    }

    private void remove(String id, Presence p) {
        for(Presence presence : roster.get(id)) {
            if(presence.getJID().equals(p.getJID())) {
                roster.remove(presence);
            }
        }
    }

    public void userUpdated(User user) {
        
    }

    public void userDeleted(String id) {
        
    }
}

