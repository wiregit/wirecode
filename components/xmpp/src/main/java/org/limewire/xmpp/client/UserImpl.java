package org.limewire.xmpp.client;

import org.limewire.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class UserImpl implements User {
    private final String id;
    private final String name;
    private final ConcurrentHashMap<String, Presence> presences;
    private final CopyOnWriteArrayList<PresenceListener> presenceListeners;

    UserImpl(String id, String name) {
        this.id = id;
        this.name = name;
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
        synchronized (presences) {
            presences.put(presence.getJID(), presence);
            firePresenceListeners(presence);        
        }
    }

    private void firePresenceListeners(Presence presence) {
        for(PresenceListener listener : presenceListeners) {
            listener.presenceChanged(presence);
        }
    }

    void removePresense(Presence presence) {
        synchronized (presences) {
            presences.remove(presence.getJID());
            firePresenceListeners(presence);
        }
    }
    
    public void addPresenceListener(PresenceListener presenceListener) {
        synchronized (presences) {
            presenceListeners.add(presenceListener);
            for(Presence presence : presences.values()) {
                presenceListener.presenceChanged(presence);    
            }
        }
    }

    public String toString() {
        return StringUtils.toString(this, id, name);
    }
}
