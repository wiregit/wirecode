package org.limewire.xmpp.client.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.listener.EventListener;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;

public class RosterListenerMock implements EventListener<RosterEvent> {
    private HashMap<String, Friend> users = new HashMap<String, Friend>();
    private HashMap<String, ArrayList<FriendPresence>> roster = new HashMap<String, ArrayList<FriendPresence>>();
    IncomingChatListenerMock listener = new IncomingChatListenerMock();
    
    public void handleEvent(RosterEvent event) {
        if(event.getType().equals(RosterEvent.Type.USER_ADDED)) {
            userAdded(event.getData());
        } else if(event.getType().equals(RosterEvent.Type.USER_DELETED)) {
            userDeleted(event.getData().getId());
        } else if(event.getType().equals(RosterEvent.Type.USER_UPDATED)) {
            userUpdated(event.getData());
        }
    }
    
    public synchronized int getRosterSize() {
        return roster.size();
    }
    
    public synchronized String getFirstRosterEntry() {
        return roster.keySet().iterator().next();
    }
    
    public synchronized int countPresences(String username) {
        ArrayList<FriendPresence> presences = roster.get(username);
        return presences == null ? 0 : presences.size();
    }
    
    public synchronized FriendPresence getFirstPresence(String username) {
        ArrayList<FriendPresence> presences = roster.get(username);
        return (presences == null || presences.isEmpty()) ? null : presences.get(0);
    }
    
    public synchronized Friend getUser(String username) {
        return users.get(username);
    }
    
    private synchronized void userAdded(Friend user) {
        System.out.println("user added: " + user.getId()); 
        users.put(user.getId(), user);
        if(roster.get(user.getId()) == null) {
            roster.put(user.getId(), new ArrayList<FriendPresence>());
        }
        final String name = user.getName();
        user.addPresenceListener(new EventListener<PresenceEvent>() {
            public void handleEvent(PresenceEvent event) {
                synchronized (RosterListenerMock.this) {
                    FriendPresence presence = event.getData();
                    String id = StringUtils.parseBareAddress(presence.getPresenceId());
                    if(presence.getType().equals(FriendPresence.Type.available)) {
                        if(roster.get(id) == null) {
                            roster.put(id, new ArrayList<FriendPresence>());
                        }
                        if(!contains(roster.get(id), presence.getPresenceId())) {
                            roster.get(id).add(presence);
                            presence.getFriend().setChatListenerIfNecessary(listener);
                            System.out.println("user " + presence.getPresenceId() + " (" + name + ") available");
                        } else {
                            replace(roster.get(id), presence);
                        }
                    } else if(presence.getType().equals(FriendPresence.Type.unavailable)) {
                        if(roster.get(id) == null) {
                            roster.put(id, new ArrayList<FriendPresence>());
                        }
                        remove(id, presence);
                    } else {
                        System.out.println("user presence changed: " + presence.getType());
                    }
                }
            }
        });
    }

    private synchronized void replace(ArrayList<FriendPresence> presences, FriendPresence presence) {
        for(FriendPresence p : presences) {
            if(p.getPresenceId().equals(presence.getPresenceId())) {
                presences.remove(p);
                presences.add(presence);
            }
        }
    }

    private synchronized boolean contains(ArrayList<FriendPresence> presences, String jid) {
        for(FriendPresence presence : presences) {
            if(presence.getPresenceId().equals(jid)) {
                return true;
            }
        }
        return false;
    }

    private synchronized void remove(String id, FriendPresence p) {
        for(FriendPresence presence : roster.get(id)) {
            if(presence.getPresenceId().equals(p.getPresenceId())) {
                roster.get(id).remove(presence);
                if(roster.get(id).size() == 0) {
                    roster.remove(id);
                }
                break;
            }
        }
    }

    private void userUpdated(Friend user) {
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
