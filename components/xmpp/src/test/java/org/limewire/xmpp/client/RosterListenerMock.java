package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.util.HashMap;

import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.RosterListener;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPService;
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
                    if(!contains(roster.get(id), presence.getJID())) {
                        roster.get(id).add(presence);
                        presence.setIncomingChatListener(listener);
                        if(presence instanceof LimePresence) {
                            System.out.println("lime user " + presence.getJID() + " (" + name + ") available");
                            // TODO browse host
                        } else {                            
                            System.out.println("user " + presence.getJID() + " (" + name + ") available");
                        }
                    } else {
                        replace(roster.get(id), presence);
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

    private void replace(ArrayList<Presence> presences, Presence presence) {
        for(Presence p : presences) {
            if(p.getJID().equals(presence.getJID())) {
                presences.remove(p);
                presences.add(presence);
            }
        }
    }

    private boolean contains(ArrayList<Presence> presences, String jid) {
        for(Presence presence : presences) {
            if(presence.getJID().equals(jid)) {
                return true;
            }
        }
        return false;
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
