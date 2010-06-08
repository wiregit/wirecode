package org.limewire.mojito;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.Value;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

import com.limegroup.gnutella.RouteTable;

/**
 * The {@link MojitoDHT} interface provides simplified versions of some
 * methods and 
 */
public interface MojitoDHT extends DHT {
    
    /**
     * Returns the {@link Vendor}.
     */
    public Vendor getVendor();
    
    /**
     * Returns the {@link Version}.
     */
    public Version getVersion();
    
    /**
     * Returns the localhost's {@link KUID}.
     */
    public KUID getContactId();
    
    /**
     * Sets the localhost's {@link KUID}.
     */
    public void setContactId(KUID contactId);
    
    /**
     * Returns the localhost's {@link SocketAddress}.
     */
    public SocketAddress getContactAddress();
    
    /**
     * Sets the localhost's {@link SocketAddress}.
     */
    public void setContactAddress(SocketAddress address);
    
    /**
     * Sets the localhost's external {@link SocketAddress}.
     */
    public void setExternalAddress(SocketAddress address);
    
    /**
     * Returns the first {@link Contact} from the {@link RouteTable} that
     * responds to a PING.
     */
    public DHTFuture<PingEntity> findActiveContact();
    
    /**
     * @see #ping(String, int, long, TimeUnit)
     */
    public DHTFuture<PingEntity> ping(String address, int port);
    
    /**
     * @see #ping(InetAddress, int, long, TimeUnit)
     */
    public DHTFuture<PingEntity> ping(InetAddress address, int port);
    
    /**
     * @see #ping(SocketAddress, long, TimeUnit)
     */
    public DHTFuture<PingEntity> ping(SocketAddress addr);
    
    /**
     * @see #ping(Contact, Contact[], long, TimeUnit)
     */
    public DHTFuture<PingEntity> ping(Contact src, Contact[] dst);
    
    /**
     * Sends a special PING to the given {@link Contact} that checks
     * whether or not 
     */
    public DHTFuture<PingEntity> collisionPing(Contact dst);
    
    /**
     * @see #bootstrap(SocketAddress, long, TimeUnit)
     */
    public DHTFuture<BootstrapEntity> bootstrap(String address, int port);
    
    /**
     * @see #bootstrap(SocketAddress, long, TimeUnit)
     */
    public DHTFuture<BootstrapEntity> bootstrap(InetAddress address, int port);
    
    /**
     * @see #bootstrap(SocketAddress, long, TimeUnit)
     */
    public DHTFuture<BootstrapEntity> bootstrap(SocketAddress addr);
    
    /**
     * @see #bootstrap(Contact, long, TimeUnit)
     */
    public DHTFuture<BootstrapEntity> bootstrap(Contact contact);
    
    /**
     * @see #put(KUID, Value, long, TimeUnit)
     */
    public DHTFuture<StoreEntity> put(KUID key, Value value);
    
    /**
     * @see #enqueue(KUID, Value, long, TimeUnit)
     */
    public DHTFuture<StoreEntity> enqueue(KUID key, Value value);
 
    /**
     * Removes the given {@link KUID} from the DHT.
     */
    public DHTFuture<StoreEntity> remove(KUID key);
    
    /**
     * @see #lookup(KUID, long, TimeUnit)
     */
    public DHTFuture<NodeEntity> lookup(KUID lookupId);
    
    /**
     * @see #get(ValueKey, long, TimeUnit)
     */
    public DHTFuture<ValueEntity> get(ValueKey key);
    
    /**
     * Retrieves all values for the given {@link ValueKey} from the DHT.
     */
    public DHTFuture<ValueEntity[]> getAll(ValueKey key);
}
