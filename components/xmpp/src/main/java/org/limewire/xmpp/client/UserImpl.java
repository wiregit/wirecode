package org.limewire.xmpp.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.XMPPConnection;

public class UserImpl implements User {
    protected String id;
    protected String name;
    protected ConcurrentHashMap<String, Presence> presences;
    protected CopyOnWriteArrayList<PresenceListener> presenceListeners; 
    protected XMPPConnection connection;

    public UserImpl(String id, String name, XMPPConnection connection) {
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

    ConcurrentHashMap<String, Presence> getPresences() {
        return presences;
    }
    
    void addPresense(Presence presence) {
        System.out.println("adding presence " + presence.getJID() + " for " + getId());
        presences.put(presence.getJID(), presence);
        firePresenceListeners(presence);        
    }

    private void firePresenceListeners(Presence presence) {
        for(PresenceListener listener : presenceListeners) {
            System.out.println("firing listener: " + listener);
            listener.presenceChanged(presence);
        }
    }

    void removePresense(Presence presence) {
        System.out.println("removing presence " + presence.getJID() + " for " + getId());
        presences.remove(presence);
        firePresenceListeners(presence);
    }
    
    public void addPresenceListener(PresenceListener presenceListener) {
        presenceListeners.add(presenceListener);
        // TODO fire existing presences
    }
}
