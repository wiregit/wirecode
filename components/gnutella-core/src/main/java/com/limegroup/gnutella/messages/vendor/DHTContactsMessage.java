package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.limewire.io.IOUtils;
import org.limewire.mojito2.message.MessageInputStream;
import org.limewire.mojito2.message.MessageOutputStream;
import org.limewire.mojito2.routing.Contact;
import org.limewire.security.MACCalculatorRepositoryManager;

import com.limegroup.gnutella.messages.BadPacketException;

/**
 * Passive DHT nodes (firewalled leafs and ultrapeers) may use the
 * DHTContactsMessage to exchange fresh Contact information with each
 * other to keep their RouteTables updated without putting load on the DHT.
 */
public class DHTContactsMessage extends AbstractVendorMessage {
    
    public static final int VERSION = 1;
    
    private final Contact[] nodes;
    
    public DHTContactsMessage(Contact node) {
        this(new Contact[] { node });
    }
    
    public DHTContactsMessage(Contact[] nodes) {
        super(F_LIME_VENDOR_ID, F_DHT_CONTACTS, VERSION, derivePayload(nodes));
        
        this.nodes = nodes;
    }
    
    public DHTContactsMessage(byte[] guid, byte ttl, byte hops, 
            int version, byte[] payload, Network network, 
            MACCalculatorRepositoryManager macManager) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, 
                F_DHT_CONTACTS, version, payload, network);
        
        ByteArrayInputStream bais = new ByteArrayInputStream(payload);
        MessageInputStream in = new MessageInputStream(bais, macManager);
        
        try {
            this.nodes = in.readContacts();
        } catch (IOException err) {
            throw new BadPacketException(err);
        } finally {
            IOUtils.close(in);
        }
    }
    
    public Contact[] getContacts() {
        return nodes;
    }
    
    private static byte[] derivePayload(Contact[] nodes) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessageOutputStream out = new MessageOutputStream(baos);
        
        try {
            out.writeContacts(nodes);
        } catch (IOException err) {
        } finally {
            IOUtils.close(out);
        }
        
        return baos.toByteArray();
    }
}
