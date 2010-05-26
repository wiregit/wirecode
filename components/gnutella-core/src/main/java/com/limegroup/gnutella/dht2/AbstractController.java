package com.limegroup.gnutella.dht2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.entity.CollisionException;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.LocalContact;
import org.limewire.mojito2.util.IoUtils;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

public abstract class AbstractController implements Controller {
    
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
        IoUtils.close(getMojitoDHT());
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
        
        Context context = dht.getContext();
        return context.isLocalNode(contact);
    }
    
    @Override
    public void addressChanged() {
        MojitoDHT dht = getMojitoDHT();
        if (dht != null) {
            try {
                Contact contact = dht.getLocalNode();
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
        
        switch (mode) {
            case PASSIVE:
            case PASSIVE_LEAF:
                contact.setFirewalled(true);
                break;
        }
        
        contact.nextInstanceID();
    }
    
    /**
     * Returns the current external address.
     */
    protected SocketAddress getExternalAddress() 
            throws UnknownHostException {
        
        InetAddress address = InetAddress.getByAddress(
                networkManager.getExternalAddress());
        int port = networkManager.getNonForcedPort();
        
        if (address.isAnyLocalAddress()) {
            address = InetAddress.getLocalHost();
        }
        
        return new InetSocketAddress(address, port);
    }
}
