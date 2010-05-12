package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.Context;
import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.DHTValueFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.NodeEntity;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.MessageDispatcher;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueFactoryManager;
import org.limewire.mojito2.storage.Database;
import org.limewire.mojito2.storage.Storable;
import org.limewire.mojito2.storage.StorableModelManager;
import org.limewire.mojito2.util.HostFilter;
import org.limewire.mojito2.util.NopTransport;

public class MojitoDHTStub implements MojitoDHT {

    @Override
    public DHTFuture<BootstrapEntity> bootstrap(Contact contact) {
        return createFuture();
    }

    @Override
    public DHTFuture<BootstrapEntity> bootstrap(InetAddress address, int port) {
        return createFuture();
    }

    @Override
    public DHTFuture<BootstrapEntity> bootstrap(SocketAddress addr) {
        return createFuture();
    }

    @Override
    public DHTFuture<BootstrapEntity> bootstrap(String address, int port) {
        return createFuture();
    }

    @Override
    public DHTFuture<PingEntity> collisionPing(Contact dst) {
        return createFuture();
    }

    @Override
    public DHTFuture<PingEntity> findActiveContact() {
        return createFuture();
    }

    @Override
    public DHTFuture<ValueEntity> get(EntityKey key) {
        return createFuture();
    }

    @Override
    public DHTFuture<ValueEntity[]> getAll(EntityKey key) {
        return createFuture();
    }

    @Override
    public SocketAddress getContactAddress() {
        return null;
    }

    @Override
    public Context getContext() {
        return null;
    }

    @Override
    public KUID getLocalNodeID() {
        return null;
    }

    @Override
    public Vendor getVendor() {
        return Vendor.UNKNOWN;
    }

    @Override
    public Version getVersion() {
        return Version.ZERO;
    }

    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId) {
        return createFuture();
    }

    @Override
    public DHTFuture<PingEntity> ping(InetAddress address, int port) {
        return ping(new InetSocketAddress(address, port));
    }

    @Override
    public DHTFuture<PingEntity> ping(SocketAddress addr) {
        return ping(addr, 0L, TimeUnit.MILLISECONDS);
    }

    @Override
    public DHTFuture<PingEntity> ping(String address, int port) {
        return ping(new InetSocketAddress(address, port));
    }

    @Override
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value) {
        return createFuture();
    }

    @Override
    public DHTFuture<StoreEntity> remove(KUID key) {
        return createFuture();
    }

    @Override
    public void setContactAddress(SocketAddress address) {
    }

    @Override
    public void setContactId(KUID contactId) {
    }

    @Override
    public void setExternalAddress(SocketAddress address) {
    }

    @Override
    public DHTFuture<StoreEntity> store(Storable storable) {
        return createFuture();
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void bind(Transport transport) throws IOException {
    }

    @Override
    public DHTFuture<BootstrapEntity> bootstrap(Contact dst, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public DHTFuture<BootstrapEntity> bootstrap(SocketAddress dst, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public DHTFuture<ValueEntity> get(EntityKey key, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public DHTFuture<ValueEntity> get(KUID lookupId, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public Database getDatabase() {
        return null;
    }

    @Override
    public DHTValueFactoryManager getDHTValueFactoryManager() {
        return null;
    }

    @Override
    public HostFilter getHostFilter() {
        return null;
    }

    @Override
    public Contact getLocalNode() {
        return null;
    }

    @Override
    public MessageDispatcher getMessageDispatcher() {
        return null;
    }

    @Override
    public MessageFactory getMessageFactory() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public RouteTable getRouteTable() {
        return null;
    }

    @Override
    public StorableModelManager getStorableModelManager() {
        return null;
    }

    @Override
    public boolean isBooting() {
        return false;
    }

    @Override
    public boolean isBound() {
        return false;
    }

    @Override
    public boolean isFirewalled() {
        return false;
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, Contact[] dst, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public DHTFuture<PingEntity> ping(Contact src, Contact[] dst, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public DHTFuture<PingEntity> ping(Contact dst, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public DHTFuture<PingEntity> ping(InetAddress address, int port, long timeout, TimeUnit unit) {
        return ping(new InetSocketAddress(address, port), timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(SocketAddress dst, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public DHTFuture<PingEntity> ping(String address, int port, long timeout, TimeUnit unit) {
        return ping(new InetSocketAddress(address, port), timeout, unit);
    }

    @Override
    public DHTFuture<StoreEntity> put(DHTValueEntity value, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public DHTFuture<StoreEntity> put(Storable storable, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public void setHostFilter(HostFilter hostFilter) {
    }

    @Override
    public BigInteger size() {
        return BigInteger.ZERO;
    }

    @Override
    public <T> DHTFuture<T> submit(AsyncProcess<T> process, long timeout, TimeUnit unit) {
        return createFuture();
    }

    @Override
    public Transport unbind() {
        return NopTransport.NOP;
    }
    
    private static <V> DHTFuture<V> createFuture() {
        return new DHTValueFuture<V>(new UnsupportedOperationException());
    }
}
