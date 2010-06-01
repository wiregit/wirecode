package org.limewire.mojito.io;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.BootstrapSettings;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.util.Objects;

/**
 * 
 */
public class BootstrapConfig {
    
    private volatile int alpha 
        = BootstrapSettings.BOOTSTRAP_WORKERS.getValue();
    
    private volatile long pingTimeout 
        = NetworkSettings.DEFAULT_TIMEOUT.getTimeInMillis();
    
    private volatile long lookupTimeout 
        = LookupSettings.FIND_NODE_LOOKUP_TIMEOUT.getTimeInMillis();
    
    private volatile long refreshTimeout 
        = BootstrapSettings.BOOTSTRAP_TIMEOUT.getTimeInMillis();
    
    private final SocketAddress address;
    
    private final Contact contact;
    
    /**
     * 
     */
    public BootstrapConfig(String address, int port) {
        this(new InetSocketAddress(address, port));
    }
    
    /**
     * 
     */
    public BootstrapConfig(InetAddress address, int port) {
        this(new InetSocketAddress(address, port));
    }
    
    /**
     * 
     */
    public BootstrapConfig(SocketAddress address) {
        this(address, null);
        Objects.nonNull(address, "address");
    }
    
    /**
     * 
     */
    public BootstrapConfig(Contact contact) {
        this(null, contact);
        Objects.nonNull(contact, "contact");
    }
    
    /**
     * 
     */
    private BootstrapConfig(SocketAddress address, Contact contact) {
        this.address = address;
        this.contact = contact;
    }
    
    /**
     * 
     */
    public SocketAddress getAddress() {
        return address;
    }
    
    /**
     * 
     */
    public Contact getContact() {
        return contact;
    }
    
    /**
     * 
     */
    public int getAlpha() {
        return alpha;
    }
    
    /**
     * 
     */
    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }
    
    /**
     * 
     */
    public long getPingTimeout(TimeUnit unit) {
        return unit.convert(pingTimeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public long getPingTimeoutInMillis() {
        return getPingTimeout(TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public void setPingTimeout(long timeout, TimeUnit unit) {
        this.pingTimeout = unit.toMillis(timeout);
    }
    
    /**
     * 
     */
    public long getLookupTimeout(TimeUnit unit) {
        return unit.convert(lookupTimeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public long getLookupTimeoutInMillis() {
        return getLookupTimeout(TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public void setLookupTimeout(long timeout, TimeUnit unit) {
        this.lookupTimeout = unit.toMillis(timeout);
    }
    
    /**
     * 
     */
    public long getRefreshTimeout(TimeUnit unit) {
        return unit.convert(refreshTimeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public long getRefreshTimeoutInMillis() {
        return getRefreshTimeout(TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    public void setRefreshTimeout(long timeout, TimeUnit unit) {
        this.refreshTimeout = unit.toMillis(timeout);
    }
}