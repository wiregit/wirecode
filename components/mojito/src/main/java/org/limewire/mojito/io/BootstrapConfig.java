package org.limewire.mojito.io;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.BootstrapManager;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.BootstrapSettings;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.util.Objects;

/**
 * A config for the {@link BootstrapManager}.
 */
public class BootstrapConfig {
    
    private volatile int concurrency 
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
     * Creates a {@link BootstrapConfig} for the given address.
     */
    public BootstrapConfig(String address, int port) {
        this(new InetSocketAddress(address, port));
    }
    
    /**
     * Creates a {@link BootstrapConfig} for the given address.
     */
    public BootstrapConfig(InetAddress address, int port) {
        this(new InetSocketAddress(address, port));
    }
    
    /**
     * Creates a {@link BootstrapConfig} for the given address.
     */
    public BootstrapConfig(SocketAddress address) {
        this(address, null);
        Objects.nonNull(address, "address");
    }
    
    /**
     * Creates a {@link BootstrapConfig} for the given {@link Contact}.
     */
    public BootstrapConfig(Contact contact) {
        this(null, contact);
        Objects.nonNull(contact, "contact");
    }
    
    private BootstrapConfig(SocketAddress address, Contact contact) {
        this.address = address;
        this.contact = contact;
    }
    
    /**
     * Returns the {@link SocketAddress}
     */
    public SocketAddress getAddress() {
        return address;
    }
    
    /**
     * Returns the {@link Contact}
     */
    public Contact getContact() {
        return contact;
    }
    
    /**
     * Returns the concurrency factor
     */
    public int getConcurrency() {
        return concurrency;
    }
    
    /**
     * Sets the concurrency factor
     */
    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }
    
    /**
     * Returns the <tt>PING</tt> timeout.
     */
    public long getPingTimeout(TimeUnit unit) {
        return unit.convert(pingTimeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the <tt>PING</tt> timeout in milliseconds.
     */
    public long getPingTimeoutInMillis() {
        return getPingTimeout(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sets the <tt>PING</tt> timeout.
     */
    public void setPingTimeout(long timeout, TimeUnit unit) {
        this.pingTimeout = unit.toMillis(timeout);
    }
    
    /**
     * Returns the <tt>FIND_NODE</tt> timeout.
     */
    public long getLookupTimeout(TimeUnit unit) {
        return unit.convert(lookupTimeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the <tt>FIND_NODE</tt> timeout in milliseconds.
     */
    public long getLookupTimeoutInMillis() {
        return getLookupTimeout(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sets the <tt>FIND_NODE</tt> timeout.
     */
    public void setLookupTimeout(long timeout, TimeUnit unit) {
        this.lookupTimeout = unit.toMillis(timeout);
    }
    
    /**
     * Returns the refresh timeout.
     */
    public long getRefreshTimeout(TimeUnit unit) {
        return unit.convert(refreshTimeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the refresh timeout in milliseconds.
     */
    public long getRefreshTimeoutInMillis() {
        return getRefreshTimeout(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sets the refresh timeout.
     */
    public void setRefreshTimeout(long timeout, TimeUnit unit) {
        this.refreshTimeout = unit.toMillis(timeout);
    }
}
