package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.entity.CollisionException;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.impl.LocalContact;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

/**
 * An abstract implementation of {@link Controller}.
 */
abstract class AbstractController implements Controller {
    
    private static final Log LOG 
        = LogFactory.getLog(AbstractController.class);
    
    protected final DHTMode mode;
    
    protected final Transport transport;
    
    protected final NetworkManager networkManager;
    
    public AbstractController(DHTMode mode, 
            Transport transport,
            NetworkManager networkManager) {
        
        this.mode = mode;
        
        this.transport = transport;
        this.networkManager = networkManager;
    }
    
    @Override
    public void start() throws IOException {
        MojitoDHT dht = getMojitoDHT();
        dht.bind(transport);
    }
    
    @Override
    public void close() throws IOException {
        IOUtils.close(getMojitoDHT());
    }
    
    @Override
    public DHTMode getMode() {
        return mode;
    }
    
    @Override
    public boolean isMode(DHTMode other) {
        return getMode() == other;
    }
    
    @Override
    public void addActiveNode(SocketAddress address) {
        
    }
    
    @Override
    public void addPassiveNode(SocketAddress address) {
        
    }
    
    @Override
    public Contact[] getActiveContacts(int max) {
        return new Contact[0];
    }
    
    @Override
    public void handleCollision(CollisionException ex) {
        
    }

    @Override
    public void handleContactsMessage(DHTContactsMessage msg) {
    }
    
    /**
     * Returns true if the given {@link Contact} is the localhost
     */
    protected boolean isLocalhost(Contact contact) {
        MojitoDHT dht = getMojitoDHT();
        if (dht == null) {
            return false;
        }
        
        Contact localhost = dht.getLocalhost();
        return contact.equals(localhost);
    }
    
    @Override
    public void addressChanged() {
        MojitoDHT dht = getMojitoDHT();
        if (dht != null) {
            try {
                Contact contact = dht.getLocalhost();
                initLocalhost((LocalContact)contact);
            } catch (IOException err) {
                LOG.error("IOException", err);
            }
        }
    }
    
    /**
     * Initializes the given {@link LocalContact} with the default
     * vendor, version and external addresses.
     */
    protected void initLocalhost(LocalContact contact) 
            throws UnknownHostException {
        contact.setVendor(DHTManager.VENDOR);
        contact.setVersion(DHTManager.VERSION);
        contact.setContactAddress(getExternalAddress());  
        
        // Everything but ACTIVE mode is considered firewalled.
        contact.setFirewalled(mode != DHTMode.ACTIVE);
        contact.nextInstanceID();
    }
    
    /**
     * Returns the current external address.
     */
    protected SocketAddress getExternalAddress() 
            throws UnknownHostException {
        
        InetAddress address = null;
        
        byte[] external = networkManager.getExternalAddress();
        int port = networkManager.getNonForcedPort();
        
        if (external != null) {
            address = InetAddress.getByAddress(external);
        }
        
        if (address == null || address.isAnyLocalAddress()) {
            address = InetAddress.getLocalHost();
        }
        
        return new InetSocketAddress(address, port);
    }
}
