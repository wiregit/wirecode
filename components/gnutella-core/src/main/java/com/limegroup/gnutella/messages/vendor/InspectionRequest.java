package com.limegroup.gnutella.messages.vendor;

import java.net.UnknownHostException;

import org.limewire.io.IPPortCombo;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.GUID;
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
    static final String TIMESTAMP_KEY = "T";

    private final String[] requested;
    private final boolean timestamp;
    
    public InspectionRequest(byte[] guid, byte ttl, byte hops, 
            int version, byte[] payload, int network)
            throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_INSPECTION_REQ, version, payload, network);
        String requested;
        try {
            requested = ggep.getString(INSPECTION_KEY);
            timestamp = ggep.hasKey(TIMESTAMP_KEY);
        } catch (BadGGEPPropertyException bad) {
            throw new BadPacketException();
        }
        
        this.requested = requested.split(";"); 
    }
    
    InspectionRequest(GGEPSigner signer, String... requested) {
        this(new GUID(),signer, false, 1, null, null, requested);
    }
    /**
     * @param timestamp true if the response should contain a timestamp.
     * @param requested requested fields for inspection.  
     * See <tt>InspectionUtils</tt> for description of the format.
     */
    public InspectionRequest(GUID g, GGEPSigner signer, boolean timestamp, 
            long version, IpPort returnAddr, IpPort destAddress, String... requested) {
        super(F_LIME_VENDOR_ID, F_INSPECTION_REQ, VERSION, signer,
                deriveGGEP(timestamp, version, returnAddr, destAddress, requested));
        setGUID(g);
        this.requested = requested;
        this.timestamp = timestamp;
    }

    public String[] getRequestedFields() {
        return requested;
    }
    
    public boolean requestsTimeStamp() {
        return timestamp;
    }
    
    public void setGUID(GUID g) {
        super.setGUID(g);
    }
    
    private static GGEP deriveGGEP(boolean timestamp, long version, IpPort returnAddr, IpPort destAddr, String... requested) {
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
        if (timestamp)
            g.put(TIMESTAMP_KEY);

        if (returnAddr != null) {
            try {
                IPPortCombo ipc = new IPPortCombo(returnAddr.getAddress(), returnAddr.getPort());
                g.put(RETURN_ADDRESS_KEY, ipc.toBytes());
            } catch (UnknownHostException ignore){}
        }
        
        if (destAddr != null) {
            try {
                IPPortCombo ipc = new IPPortCombo(destAddr.getAddress(), destAddr.getPort());
                g.put(TO_ADDRESS_KEY, ipc.toBytes());
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
