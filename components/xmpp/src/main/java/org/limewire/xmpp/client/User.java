package org.limewire.xmpp.client;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.XMPPConnection;

public class User {
    protected String id;
    protected String name;
    protected ConcurrentHashMap<String, Presence> presences;
    protected CopyOnWriteArrayList<PresenceListener> presenceListeners; 
    protected XMPPConnection connection;

    public User(String id, String name, XMPPConnection connection) {
        this.id = id;
        this.name = name;
        this.connection = connection;
        this.presences = new ConcurrentHashMap<String, Presence>(); 
        this.presenceListeners = new CopyOnWriteArrayList<PresenceListener>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ConcurrentHashMap<String, Presence> getPresences() {
        return presences;
    }
    
    public void addPresense(Presence presence) {
        presences.put(presence.getJID(), presence);
        firePresenceListeners(presence);        
    }

    private void firePresenceListeners(Presence presence) {
        for(PresenceListener listener : presenceListeners) {
            listener.presenceChanged(presence);
        }
    }

    public void removePresense(Presence presence) {
        presences.remove(presence);
        firePresenceListeners(presence);
    }
    
    public void addPresenceListener(PresenceListener presenceListener) {
        presenceListeners.add(presenceListener);
        // TODO fire existing presences
    }
}
