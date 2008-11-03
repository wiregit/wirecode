package org.limewire.xmpp.client.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.jivesoftware.smack.RosterEntry;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.DebugRunnable;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.User;

public class UserImpl implements User {
    private static final Log LOG = LogFactory.getLog(UserImpl.class);

    private final String id;
    private AtomicReference<RosterEntry> rosterEntry;
    private final ConcurrentHashMap<String, Presence> presences;
    private final EventListenerList<PresenceEvent> presenceListeners;
    private final Network network;

    UserImpl(String id, RosterEntry rosterEntry, Network network) {
        this.id = id;
        this.network = network;
        this.rosterEntry = new AtomicReference<RosterEntry>(rosterEntry);
        this.presences = new ConcurrentHashMap<String, Presence>();
        this.presenceListeners = new EventListenerList<PresenceEvent>();
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
        presenceListeners.broadcast(new PresenceEvent(presence, Presence.EventType.PRESENCE_NEW));
    }

    void removePresense(Presence presence) {
        if(LOG.isDebugEnabled()) {
            LOG.debugf("removing presence {0}", presence.getJID());
        }
        Collection<Feature> features = presence.getFeatures();
        for(Feature feature : features) {
            presence.removeFeature(feature.getID());
        }
        presences.remove(presence.getJID());
        presenceListeners.broadcast(new PresenceEvent(presence, Presence.EventType.PRESENCE_UPDATE));
    }

    public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
        presenceListeners.addListener(presenceListener);
        for(Presence presence : presences.values()) {
            presenceListener.handleEvent(new PresenceEvent(presence, Presence.EventType.PRESENCE_UPDATE));
        }
    }

    public String toString() {
        return StringUtils.toString(this, id, rosterEntry.get().getName());
    }

    void updatePresence(Presence updatedPresence) {
        if(LOG.isDebugEnabled()) {
            LOG.debugf("updating presence {0}", updatedPresence.getJID());
        }
        presences.put(updatedPresence.getJID(), updatedPresence);
        presenceListeners.broadcast(new PresenceEvent(updatedPresence, Presence.EventType.PRESENCE_UPDATE));
    }

    Presence getPresence(String jid) {
        return presences.get(jid);
    }

    public Network getNetwork() {
        return network;
    }
}
