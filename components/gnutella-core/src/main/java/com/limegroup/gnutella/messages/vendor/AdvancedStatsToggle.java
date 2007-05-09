package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * Vendor message that toggles collecting of advanced stats on or off.
 */
public class AdvancedStatsToggle extends RoutableGGEPMessage {

    private static final int VERSION = 1;
    
    /** key for the block containing how long to keep stats on */
    static final String TIME_KEY = "T";
    
    /** key for the block instructing immediate turn off */
    static final String OFF_KEY = "OFF";
    
    /** 
     * How long to keep advanced stats on.  Negative if they should
     * be turned off immediately.
     */
    private final int time;
    
    /**
     * Network constructor.. this message will not be sent from nodes.
     */
    public AdvancedStatsToggle(byte[] guid, byte ttl, byte hops,
            int version, byte[] payload, int network)
            throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_ADVANCED_TOGGLE, version, payload, network);
        try {
            // check for shut off key first
            if (ggep.hasKey(OFF_KEY))
                time = -1;
            else
                this.time = ggep.getInt(TIME_KEY);
        } catch (BadGGEPPropertyException bad) {
            throw new BadPacketException();
        }
    }
    
    public AdvancedStatsToggle(int time) {
        super(F_LIME_VENDOR_ID, F_ADVANCED_TOGGLE, VERSION, deriveGGEP(time));
        this.time = time;
    }
    
    private static GGEP deriveGGEP(int time) {
        
        // ggep does not support negative integers, so if this is a
        // shut off just put the shut off key.
        GGEP g = new GGEP();
        if (time >= 0)
            g.put(TIME_KEY, time);
        else
            g.put(OFF_KEY);
        
        return g;
    }
    
    /**
     * @return if the advanced stats should be shut off immediately
     */
    public boolean shutOffNow() {
        return time < 0;
    }
    
    /**
     * @return the amount of time in milliseconds to keep advanced
     * stats on.
     */
    public int getTime() {
        return time;
    }

}
