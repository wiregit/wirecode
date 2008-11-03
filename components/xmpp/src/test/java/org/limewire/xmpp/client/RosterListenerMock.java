package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.listener.EventListener;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

public class RosterListenerMock implements EventListener<RosterEvent> {
    public HashMap<String, User> users = new HashMap<String, User>();
    public HashMap<String, ArrayList<Presence>> roster = new HashMap<String, ArrayList<Presence>>();
    ArrayList<FileMetaData> files = new ArrayList<FileMetaData>();
    IncomingChatListenerMock listener = new IncomingChatListenerMock();
    FeatureEventListener featureEventListener = new FeatureEventListener();
    
    public void handleEvent(RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            userAdded(event.getSource());
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            userDeleted(event.getSource().getId());
        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
            userUpdated(event.getSource());
        }
    }

    private void userAdded(User user) {
        System.out.println("user added: " + user.getId()); 
        users.put(user.getId(), user);
        if(roster.get(user.getId()) == null) {
            roster.put(user.getId(), new ArrayList<Presence>());
        }
        final String name = user.getName();
        user.addPresenceListener(new EventListener<PresenceEvent>() {
            public void handleEvent(PresenceEvent event) {
                synchronized (RosterListenerMock.this) {
                    Presence presence = event.getSource();
                    String id = StringUtils.parseBareAddress(presence.getJID());
                    if(presence.getType().equals(Presence.Type.available)) {
                        if(roster.get(id) == null) {
                            roster.put(id, new ArrayList<Presence>());
                        }
                        if(!contains(roster.get(id), presence.getJID())) {
                            roster.get(id).add(presence);
                            presence.setIncomingChatListener(listener);
                            System.out.println("user " + presence.getJID() + " (" + name + ") available");
                        } else {
                            replace(roster.get(id), presence);
                        }
                    } else if(presence.getType().equals(Presence.Type.unavailable)) {
                        if(roster.get(id) == null) {
                            roster.put(id, new ArrayList<Presence>());
                        }
                        remove(id, presence);
                    } else {
                        System.out.println("user presence changed: " + presence.getType());
                    }
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
                roster.get(id).remove(presence);
                break;
            }
        }
    }

    private void userUpdated(User user) {
        System.out.println("user updated: " + user.getId());
    }

    private void userDeleted(String id) {
        System.out.println("user deleted: " +id);
    }

    class FeatureEventListener implements EventListener<FeatureEvent> {
        List<FeatureEvent> featureEvents = new ArrayList<FeatureEvent>();
        public void handleEvent(FeatureEvent event) {
            featureEvents.add(event);
        }
    }
}
