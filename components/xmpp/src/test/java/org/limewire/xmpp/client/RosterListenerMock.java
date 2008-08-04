package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.util.HashMap;

import org.limewire.xmpp.client.service.RosterListener;
import org.limewire.xmpp.client.service.Presence;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.User;
import org.limewire.xmpp.client.service.PresenceListener;
import org.limewire.xmpp.client.service.LimePresence;
import org.limewire.xmpp.client.service.XMPPService;
import org.jivesoftware.smack.util.StringUtils;

import com.google.inject.Inject;

public class RosterListenerMock implements RosterListener {
    public HashMap<String, ArrayList<Presence>> roster = new HashMap<String, ArrayList<Presence>>();
    ArrayList<FileMetaData> files = new ArrayList<FileMetaData>();
    IncomingChatListenerMock listener = new IncomingChatListenerMock();

    @Inject
    public void register(XMPPService xmppService) {
        //xmppService.register(this);
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
                        System.out.println("lime user " + presence.getJID() + " (" + name + ") available");
                        // TODO browse host
                    } else {                            
                        System.out.println("user " + presence.getJID() + " (" + name + ") available");
                    }
                } else if(presence.getType().equals(Presence.Type.unavailable)) {
                    if(roster.get(id) == null) {
                        roster.put(id, new ArrayList<Presence>());
                    }
                    remove(id, presence);
                    if(presence instanceof LimePresence) {
                        System.out.println("lime user " + presence.getJID() + " (" + name + ") unavailable");
                    } else {
                        
                        System.out.println("user " + presence.getJID() + " (" + name + ") unavailable");
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
