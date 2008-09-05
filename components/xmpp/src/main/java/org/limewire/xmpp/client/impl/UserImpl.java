package org.limewire.xmpp.client.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.RosterEntry;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;
import org.limewire.util.DebugRunnable;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.User;
import org.limewire.concurrent.ThreadExecutor;

public class UserImpl implements User {
    private static final Log LOG = LogFactory.getLog(UserImpl.class);

    private final String id;
    private String name;
    private final ConcurrentHashMap<String, Presence> presences;
    private final CopyOnWriteArrayList<PresenceListener> presenceListeners;

    UserImpl(String id, RosterEntry rosterEntry) {
        this.id = id;
        this.name = rosterEntry.getName();
        this.presences = new ConcurrentHashMap<String, Presence>(); 
        this.presenceListeners = new CopyOnWriteArrayList<PresenceListener>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        synchronized (this) {
            return name;
        }
    }
    
    void setRosterEntry(RosterEntry rosterEntry) {
        synchronized (this) {
            this.name = rosterEntry.getName();
        }
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

    private void firePresenceListeners(final Presence presence) {
        for(final PresenceListener listener : presenceListeners) {
            Thread t = ThreadExecutor.newManagedThread(new DebugRunnable(new Runnable() {
                public void run() {
                    listener.presenceChanged(presence);
                }                
            }), "presence-listener-thread-" + listener);
            t.start();
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

    public void updatePresence(Presence updatedPresence) {
        presences.remove(updatedPresence.getJID());
        presences.put(updatedPresence.getJID(), updatedPresence);
        firePresenceListeners(updatedPresence);
    }

    @Override
    public boolean jidBelongsTo(String jid) {
        return presences.containsKey(jid);
    }
    
    Presence getPresence(String jid) {
        return presences.get(jid);
    }
}
