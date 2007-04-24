package com.limegroup.gnutella.messages.vendor;

import java.net.UnknownHostException;

import org.limewire.io.IPPortCombo;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * Message requesting inspection for specified values.
 * 
 * Note this is very LimeWire-specific, so other vendors will
 * almost certainly have no use for supporting this message.
 */
public class InspectionRequest extends RoutableGGEPMessage {
    
    static final int VERSION = 1;
    static final String INSPECTION_KEY = "I";

    private final String[] requested;
    
    public InspectionRequest(byte[] guid, byte ttl, byte hops, 
            int version, byte[] payload, int network)
            throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_INSPECTION_REQ, version, payload, network);
        String requested;
        try {
            requested = ggep.getString(INSPECTION_KEY);
        } catch (BadGGEPPropertyException bad) {
            throw new BadPacketException();
        }
        
        this.requested = requested.split(";"); 
    }
    
    InspectionRequest(String... requested) {
        this(1, null, requested);
    }
    /**
     * @param requested requested fields for inspection.  
     * See <tt>InspectionUtils</tt> for description of the format.
     */
    InspectionRequest(int version, IpPort returnAddr, String... requested) {
        super(F_LIME_VENDOR_ID, F_INSPECTION_REQ, VERSION,
                deriveGGEP(version, returnAddr, requested));
        this.requested = requested;
    }

    public String[] getRequestedFields() {
        return requested;
    }
    
    private static GGEP deriveGGEP(int version, IpPort returnAddr, String... requested) {
        /*
         * The selected fields are catenated and put in a compressed
         * ggep entry.
         */
        StringBuilder b = new StringBuilder();
        for (String r : requested)
            b.append(r).append(";");
        String ret;
        if (b.charAt(b.length() - 1) == ';')
            ret = b.substring(0, b.length() - 1);
        else
            ret = b.toString();
        
        GGEP g = new GGEP();
        g.putCompressed(INSPECTION_KEY, ret.getBytes());
        if (returnAddr != null) {
            try {
                IPPortCombo ipc = new IPPortCombo(returnAddr.getAddress(), returnAddr.getPort());
                g.put(RETURN_ADDRESS_KEY, ipc.toBytes());
            } catch (UnknownHostException ignore){}
        }
        
        if (version >= 0)
            g.put(VERSION_KEY, version);
        return g;
    }
    
    public int getVersion() {
        return super.getVersion();
    }
}
