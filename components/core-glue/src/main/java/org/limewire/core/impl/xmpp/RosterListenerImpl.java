package org.limewire.core.impl.xmpp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.net.address.Address;
import org.limewire.xmpp.client.service.LimePresence;
import org.limewire.xmpp.client.service.Presence;
import org.limewire.xmpp.client.service.PresenceListener;
import org.limewire.xmpp.client.service.RosterListener;
import org.limewire.xmpp.client.service.User;

import java.util.ArrayList;
import java.util.HashMap;

@Singleton
class RosterListenerImpl implements RosterListener {

    private final BrowseHost browseHost;

    private HashMap<String, ArrayList<Presence>> roster = new HashMap<String, ArrayList<Presence>>();
    private final IncomingChatListenerImpl listener = new IncomingChatListenerImpl();

    @Inject
    public RosterListenerImpl(BrowseHost browseHost) {
        this.browseHost = browseHost;
    }

    public void userAdded(User user) {
        System.out.println("user added: " + user.getId());
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
                        System.out.println("lime presence " + presence.getJID() + " (" + name + ") available");
                        Address address = ((LimePresence)presence).getAddress();
                        browseHost.browseHost(address);
                    } else {
                        System.out.println("presence " + presence.getJID() + " (" + name + ") available");
                    }
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    if(roster.get(id) == null) {
                        roster.put(id, new ArrayList<Presence>());
                    }
                    remove(id, presence);
                    if(presence instanceof LimePresence) {
                        System.out.println("lime presence " + presence.getJID() + " (" + name + ") unavailable");
                    } else {

                        System.out.println("presence " + presence.getJID() + " (" + name + ") unavailable");
                    }
                } else {
                    System.out.println("user presence changed: " + presence.getType());
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
        System.out.println("user updated: " + user.getId());
    }

    public void userDeleted(String id) {
        System.out.println("user deleted: " +id);
    }
}

