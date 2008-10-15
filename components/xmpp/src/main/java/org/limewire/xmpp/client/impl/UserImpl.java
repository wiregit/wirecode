package org.limewire.xmpp.client.impl;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.jivesoftware.smack.RosterEntry;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.DebugRunnable;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceListener;
import org.limewire.xmpp.api.client.User;
import org.limewire.core.api.friend.Network;

public class UserImpl implements User {
    private static final Log LOG = LogFactory.getLog(UserImpl.class);

    private final String id;
    private AtomicReference<RosterEntry> rosterEntry;
    private final ConcurrentHashMap<String, Presence> presences;
    private final CopyOnWriteArrayList<PresenceListener> presenceListeners;
    private final Network network;

    UserImpl(String id, RosterEntry rosterEntry, Network network) {
        this.id = id;
        this.network = network;
        this.rosterEntry = new AtomicReference<RosterEntry>(rosterEntry);
        this.presences = new ConcurrentHashMap<String, Presence>(); 
        this.presenceListeners = new CopyOnWriteArrayList<PresenceListener>();
    }
    
    @Override
    public boolean isAnonymous() {
        return false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return rosterEntry.get().getName();
    }
    
    @Override
    public String getRenderName() {
        String visualName = rosterEntry.get().getName();
        if(visualName == null) {
            return id;
        } else {
            return visualName;
        }
    }
    
    void setRosterEntry(RosterEntry rosterEntry) {
        this.rosterEntry.set(rosterEntry);
    }
    
    public void setName(final String name) {
        Thread t = ThreadExecutor.newManagedThread(new DebugRunnable(new Runnable() {
            public void run() {
                UserImpl.this.rosterEntry.get().setName(name);    
            }
        }), "set-name-thread-" + toString());
        t.start();
    }

    public Map<String, Presence> getPresences() {
        return Collections.unmodifiableMap(presences);
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
        if(LOG.isDebugEnabled()) {
            LOG.debugf("removing presence {0}", presence.getJID());
        }
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
        return StringUtils.toString(this, id, rosterEntry.get().getName());
    }

    void updatePresence(Presence updatedPresence) {
        if(LOG.isDebugEnabled()) {
            LOG.debugf("updating presence {0}", updatedPresence.getJID());
        }
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

    public Network getNetwork() {
        return network;
    }
}
