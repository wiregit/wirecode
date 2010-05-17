package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.CollisionException;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.storage.DHTValue;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.Controller;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

public class ControllerStub implements Controller {
    
    private final DHTMode mode;
    
    private final MojitoDHT dht;
    
    public ControllerStub(DHTMode mode, MojitoDHT dht) {
        this.mode = mode;
        this.dht = dht;
    }
    
    @Override
    public void addActiveNode(SocketAddress address) {
    }

    @Override
    public void addPassiveNode(SocketAddress address) {
    }

    @Override
    public void addressChanged() {
    }

    @Override
    public Contact[] getActiveContacts(int max) {
        return new Contact[0];
    }

    @Override
    public DHTMode getMode() {
        return mode;
    }

    @Override
    public void handleCollision(CollisionException ex) {
    }

    @Override
    public void handleContactsMessage(DHTContactsMessage msg) {
    }

    @Override
    public boolean isMode(DHTMode other) {
        return getMode() == other;
    }

    @Override
    public DHTFuture<ValueEntity> get(EntityKey key) {
        return null;
    }

    @Override
    public MojitoDHT getMojitoDHT() {
        return dht;
    }

    @Override
    public boolean isReady() {
        return dht.isReady();
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value) {
        return null;
    }

    @Override
    public void start() throws IOException {
    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
    }

    @Override
    public void close() throws IOException {
    }
}
