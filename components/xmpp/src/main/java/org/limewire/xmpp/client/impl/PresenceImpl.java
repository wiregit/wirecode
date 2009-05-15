package org.limewire.xmpp.client.impl;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.core.api.friend.AbstractFriendPresence;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.listener.EventBroadcaster;

public class PresenceImpl extends AbstractFriendPresence implements FriendPresence {

    private final XMPPFriendImpl friend;
    private final String jid;
    
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    private Type type;
    private String status;
    private int priority;
    private Mode mode;
    

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence,
                 XMPPFriendImpl friend, EventBroadcaster<FeatureEvent> featureSupport) {
        super(featureSupport);
        this.friend = friend;
        this.jid = presence.getFrom();
        update(presence);
    }

    public void update(org.jivesoftware.smack.packet.Presence presence) {
        rwLock.writeLock().lock();
        try {
            this.type = Type.valueOf(presence.getType().toString());
            this.status = presence.getStatus();
            this.priority = presence.getPriority();
            this.mode = presence.getMode() != null ? Mode.valueOf(presence.getMode().toString()) : Mode.available;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public String getPresenceId() {
        return jid;
    }

    @Override
    public Type getType() {
        rwLock.readLock().lock();
        try {
            return type;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public String getStatus() {
        rwLock.readLock().lock();
        try {
            return status;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public int getPriority() {
        rwLock.readLock().lock();
        try {
            return priority;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Mode getMode() {
        rwLock.readLock().lock();
        try {
            return mode;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        return getPresenceId() + " for " + friend.toString();
    }

    @Override
    public XMPPFriendImpl getFriend() {
        return friend;
    }
}
