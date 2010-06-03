package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.io.IpPort;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.ValueKey;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.storage.Value;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

public class DHTManagerStub implements DHTManager {

    @Override
    public void addActiveNode(SocketAddress address) {
    }

    @Override
    public void addEventListener(DHTEventListener listener) {
    }

    @Override
    public void addPassiveNode(SocketAddress address) {
    }

    @Override
    public void addressChanged() {
    }

    @Override
    public DHTFuture<ValueEntity> get(ValueKey key) {
        return null;
    }
    
    @Override
    public DHTFuture<ValueEntity[]> getAll(ValueKey key) {
        return null;
    }

    @Override
    public Contact[] getActiveContacts(int max) {
        return new Contact[0];
    }

    @Override
    public IpPort[] getActiveIpPort(int max) {
        return new IpPort[0];
    }

    @Override
    public Controller getController() {
        return null;
    }
    
    @Override
    public MojitoDHT getMojitoDHT() {
        return null;
    }

    @Override
    public DHTMode getMode() {
        return DHTMode.ACTIVE;
    }

    @Override
    public Vendor getVendor() {
        return VENDOR;
    }

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public void handleContactsMessage(DHTContactsMessage msg) {
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isMode(DHTMode mode) {
        return getMode() == mode;
    }

    @Override
    public boolean isReady() {
        return isRunning();
    }
    
    @Override
    public boolean isBooting() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return !isMode(DHTMode.INACTIVE);
    }

    @Override
    public DHTFuture<StoreEntity> put(KUID key, Value value) {
        return null;
    }
    
    @Override
    public DHTFuture<StoreEntity> enqueue(KUID key, Value value) {
        return null;
    }

    @Override
    public void removeEventListener(DHTEventListener listener) {
    }

    @Override
    public void setEnabled(boolean enabled) {
    }

    @Override
    public boolean start(DHTMode mode) {
        return false;
    }

    @Override
    public void stop() {
    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void dispatchEvent(DHTEvent event) {
    }
}
