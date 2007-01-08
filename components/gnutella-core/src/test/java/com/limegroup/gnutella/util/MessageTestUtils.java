package com.limegroup.gnutella.util;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.PingReply;

/**
 * Utility class for creating common message types for tests.
 */
public class MessageTestUtils {

    /**
     * Cached constant for the vendor GGEP extension.
     */
    private static final byte[] CACHED_VENDOR = new byte[5];

    /**
     * Should never be instantiated.
     */
    private MessageTestUtils() {}
    
    /**
     * Creates a new <tt>PingReply</tt> instance with the GGEP extension 
     * advertising free ultrapeer and leaf slots.  The generated pong will
     * have a random "unique" IP address that is statistically unlikely to 
     * collide with other addresses returned by this method.
     * 
     * @return a new <tt>PingReply</tt> for testing with the GGEP extension 
     *  advertising free ultrapeer and leaf slots
     */
    public static PingReply createPongWithFreeLeafSlots() {
        GGEP ggep = newGGEP(20, true, true, true, false);
        
        byte a = (byte)(40 + (Math.random()*80));
        byte b = (byte)(40 + (Math.random()*80));
        byte c = (byte)(40 + (Math.random()*80));
        byte d = (byte)(40 + (Math.random()*80));
        
        PingReply pr = PingReply.create(GUID.makeGuid(), (byte)1, 6346, 
            new byte[]{a,b,c,d}, 10, 10, true, ggep);
        return pr;
    }
    
    /**
     * Creates a new <tt>PingReply</tt> instance with the GGEP extension 
     * advertising free ultrapeer and leaf slots.  The generated pong will
     * have a random "unique" IP address that is statistically unlikely to 
     * collide with other addresses returned by this method.
     * 
     * @return a new <tt>PingReply</tt> for testing with the GGEP extension 
     *  advertising free ultrapeer and leaf slots
     */
    public static PingReply createPongWithUltrapeerSlots() {
        GGEP ggep = newGGEP(20, true, true, false, true);
        
        byte a = (byte)(40 + (Math.random()*80));
        byte b = (byte)(40 + (Math.random()*80));
        byte c = (byte)(40 + (Math.random()*80));
        byte d = (byte)(40 + (Math.random()*80));
        
        PingReply pr = PingReply.create(GUID.makeGuid(), (byte)1, 6346, 
            new byte[]{a,b,c,d}, 10, 10, true, ggep);
        return pr;
    }

    /** 
     * Returns the GGEP payload bytes to encode the given uptime. 
     */
    private static GGEP newGGEP(int dailyUptime, boolean isUltrapeer,
                                boolean isGUESSCapable, boolean freeLeaf, 
                                boolean freeUP) {
        GGEP ggep = new GGEP(true);
        
        if (dailyUptime >= 0)
            ggep.put(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME, dailyUptime);
        
        if (isGUESSCapable && isUltrapeer) {
            // indicate guess support
            byte[] vNum = {
                convertToGUESSFormat(LimeWireUtils.getGUESSMajorVersionNumber(),
                                     LimeWireUtils.getGUESSMinorVersionNumber())};
            ggep.put(GGEP.GGEP_HEADER_UNICAST_SUPPORT, vNum);
        }
        
        if (isUltrapeer) { 
            // indicate UP support
            addUltrapeerExtension(ggep, freeLeaf, freeUP);
        }
        
        // all pongs should have vendor info
        ggep.put(GGEP.GGEP_HEADER_VENDOR_INFO, CACHED_VENDOR); 

        return ggep;
    }


    /**
     * Adds the ultrapeer GGEP extension to the pong.  This has the version of
     * the Ultrapeer protocol that we support as well as the number of free
     * leaf and Ultrapeer slots available.
     * 
     * @param ggep the <tt>GGEP</tt> instance to add the extension to
     */
    private static void addUltrapeerExtension(GGEP ggep, boolean freeLeaf, 
                                              boolean freeUP) {
        byte[] payload = new byte[3];
        // put version
        payload[0] = convertToGUESSFormat(LimeWireUtils.getUPMajorVersionNumber(),
                                          LimeWireUtils.getUPMinorVersionNumber()
                                          );
        if(freeLeaf) {
            payload[1] = (byte)10;
        } else {
            payload[1] = (byte)0;
        }
        if(freeUP) {
            payload[2] = (byte)10;
        } else {
            payload[2] = (byte)0;
        }

        // add it
        ggep.put(GGEP.GGEP_HEADER_UP_SUPPORT, payload);
    }

    /** 
     * puts major as the high order bits, minor as the low order bits.
     *
     * @exception IllegalArgumentException thrown if major/minor is greater 
     *  than 15 or less than 0.
     */
    private static byte convertToGUESSFormat(int major, int minor) 
        throws IllegalArgumentException {
        if ((major < 0) || (minor < 0) || (major > 15) || (minor > 15))
            throw new IllegalArgumentException();
        // set major
        int retInt = major;
        retInt = retInt << 4;
        // set minor
        retInt |= minor;

        return (byte) retInt;
    }
}
