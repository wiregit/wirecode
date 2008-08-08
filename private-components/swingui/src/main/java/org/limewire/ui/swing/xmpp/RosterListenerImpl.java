package org.limewire.ui.swing.xmpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.net.address.Address;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.RosterListener;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RosterListenerImpl implements RosterListener {

    private final BrowseFactory browseFactory;
    //private final StatusPanel.BrowseAction browseAction;

    private Map<String, ArrayList<Presence>> roster = new HashMap<String, ArrayList<Presence>>();
    private final IncomingChatListenerImpl listener = new IncomingChatListenerImpl();

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

    public void userAdded(User user) {
        if(roster.get(user.getId()) == null) {
            roster.put(user.getId(), new ArrayList<Presence>());
        }
      //  final String name = user.getName();
        user.addPresenceListener(new PresenceListener() {
            public void presenceChanged(Presence presence) {
                String id = parseBareAddress(presence.getJID());
                if(presence.getType().equals(Presence.Type.available)) {
                    if(roster.get(id) == null) {
                        roster.put(id, new ArrayList<Presence>());
                    }
                    roster.get(id).add(presence);
                    presence.setIncomingChatListener(listener);
                    if(presence instanceof LimePresence) {
                        // TODO update UI
                        Address address = ((LimePresence)presence).getAddress();
                        //browseAction.setAddress(address);
                        browseFactory.createBrowse(address).start(new BrowseListener() {
                            public void handleBrowseResult(SearchResult searchResult) {
                                //System.out.println(searchResult.getDescription() + ": " + searchResult.getUrn());
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
    
    /**
     * Returns the XMPP address with any resource information removed. For example,
     * for the address "matt@jivesoftware.com/Smack", "matt@jivesoftware.com" would
     * be returned.
     *
     * @param xmppAddress the XMPP address.
     * @return the bare XMPP address without resource information.
     */
    private static String parseBareAddress(String xmppAddress) {
        if (xmppAddress == null) {
            return null;
        }
        int slashIndex = xmppAddress.indexOf("/");
        if (slashIndex < 0) {
            return xmppAddress;
        }
        else if (slashIndex == 0) {
            return "";
        }
        else {
            return xmppAddress.substring(0, slashIndex);
        }
    }
}

