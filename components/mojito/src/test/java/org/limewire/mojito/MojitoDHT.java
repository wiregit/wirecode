package org.limewire.mojito;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.DHT;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.NodeEntity;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.MessageDispatcher;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.LocalContact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.settings.BootstrapSettings;
import org.limewire.mojito2.settings.NetworkSettings;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.Database;
import org.limewire.mojito2.storage.Storable;
import org.limewire.mojito2.storage.StorableModelManager;
import org.limewire.mojito2.util.HostFilter;

public class MojitoDHT implements DHT {

    private final DHT dht;
    
    public MojitoDHT(DHT dht) {
        this.dht = dht;
    }
    
    @Override
    public void bind(Transport transport) throws IOException {
        dht.bind(transport);
    }

    @Override
    public DHTFuture<BootstrapEntity> bootstrap(Contact dst, 
            long timeout, TimeUnit unit) {
        return dht.bootstrap(dst, timeout, unit);
    }
    
    @Override
    public DHTFuture<BootstrapEntity> bootstrap(SocketAddress dst, 
            long timeout, TimeUnit unit) {
        return dht.bootstrap(dst, timeout, unit);
    }

    @Override
    public DHTFuture<ValueEntity> get(KUID lookupId, long timeout, TimeUnit unit) {
        return dht.get(lookupId, timeout, unit);
    }

    @Override
    public DHTFuture<ValueEntity> get(EntityKey key, long timeout, TimeUnit unit) {
        return dht.get(key, timeout, unit);
    }

    @Override
    public Database getDatabase() {
        return dht.getDatabase();
    }

    @Override
    public HostFilter getHostFilter() {
        return dht.getHostFilter();
    }

    @Override
    public Contact getLocalNode() {
        return dht.getLocalNode();
    }

    @Override
    public MessageDispatcher getMessageDispatcher() {
        return dht.getMessageDispatcher();
    }

    @Override
    public MessageFactory getMessageFactory() {
        return dht.getMessageFactory();
    }

    @Override
    public String getName() {
        return dht.getName();
    }

    @Override
    public RouteTable getRouteTable() {
        return dht.getRouteTable();
    }

    @Override
    public StorableModelManager getStorableModelManager() {
        return dht.getStorableModelManager();
    }

    @Override
    public boolean isBound() {
        return dht.isBound();
    }

    @Override
    public boolean isFirewalled() {
        return dht.isFirewalled();
    }
    
    public KUID getLocalNodeID() {
        return dht.getLocalNode().getNodeID();
    }
    
    public SocketAddress getContactAddress() {
        return dht.getLocalNode().getContactAddress();
    }
    
    public void setContactId(KUID contactId) {
        ((LocalContact)dht.getLocalNode()).setNodeID(contactId);
    }

    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            long timeout, TimeUnit unit) {
        return dht.lookup(lookupId, timeout, unit);
    }

    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            Contact[] dst, long timeout, TimeUnit unit) {
        return dht.lookup(lookupId, dst, timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(String address, int port, 
            long timeout, TimeUnit unit) {
        return dht.ping(address, port, timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(InetAddress address, 
            int port, long timeout, TimeUnit unit) {
        return dht.ping(address, port, timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(SocketAddress dst, 
            long timeout, TimeUnit unit) {
        return dht.ping(dst, timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(Contact dst, 
            long timeout, TimeUnit unit) {
        return dht.ping(dst, timeout, unit);
    }

    @Override
    public DHTFuture<StoreEntity> put(Storable storable, 
            long timeout, TimeUnit unit) {
        return dht.put(storable, timeout, unit);
    }

    @Override
    public DHTFuture<StoreEntity> put(DHTValueEntity value, 
            long timeout, TimeUnit unit) {
        return dht.put(value, timeout, unit);
    }

    @Override
    public void setHostFilter(HostFilter hostFilter) {
        dht.setHostFilter(hostFilter);
    }

    @Override
    public BigInteger size() {
        return dht.size();
    }

    @Override
    public <T> DHTFuture<T> submit(AsyncProcess<T> process, 
            long timeout, TimeUnit unit) {
        return dht.submit(process, timeout, unit);
    }

    @Override
    public Transport unbind() {
        return dht.unbind();
    }

    @Override
    public void close() throws IOException {
        dht.close();
    }
    
    public DHTFuture<PingEntity> ping(String address, int port) {
        return ping(new InetSocketAddress(address, port));
    }
    
    public DHTFuture<PingEntity> ping(InetAddress address, int port) {
        return ping(new InetSocketAddress(address, port));
    }
    
    public DHTFuture<PingEntity> ping(SocketAddress addr) {
        long timeout = NetworkSettings.DEFAULT_TIMEOUT.getValue();
        return ping(addr, timeout, TimeUnit.MILLISECONDS);
    }
    
    public DHTFuture<BootstrapEntity> bootstrap(String address, int port) {
        return bootstrap(new InetSocketAddress(address, port));
    }
    
    public DHTFuture<BootstrapEntity> bootstrap(InetAddress address, int port) {
        return bootstrap(new InetSocketAddress(address, port));
    }
    
    public DHTFuture<BootstrapEntity> bootstrap(SocketAddress addr) {
        long timeout = BootstrapSettings.BOOTSTRAP_TIMEOUT.getValue();
        return bootstrap(addr, timeout, TimeUnit.MILLISECONDS);
    }
}
