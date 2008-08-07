package org.limewire.xmpp.client.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.User;

public class UserImpl implements User {
    private static final Log LOG = LogFactory.getLog(UserImpl.class);

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

    public ConcurrentMap<String, Presence> getPresences() {
        return presences;
    }
    
    void addPresense(Presence presence) {
        if(LOG.isDebugEnabled()) {
            LOG.debugf("adding presence {0}", presence.getJID());
        }
        presences.put(presence.getJID(), presence);
        firePresenceListeners(presence);      
    }

    private void firePresenceListeners(Presence presence) {
        for(PresenceListener listener : presenceListeners) {
            listener.presenceChanged(presence);
        }
    }

    void removePresense(Presence presence) {
        presences.remove(presence.getJID());
        firePresenceListeners(presence);

    }
    
    public void addPresenceListener(PresenceListener presenceListener) {
        presenceListeners.add(presenceListener);
        for(Presence presence : presences.values()) {
            presenceListener.presenceChanged(presence);    
        }
    }

    public String toString() {
        return StringUtils.toString(this, id, name);
    }
}
