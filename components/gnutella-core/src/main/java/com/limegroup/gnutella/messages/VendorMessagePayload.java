package com.limegroup.gnutella.messages;

import java.io.*;
import com.sun.java.util.collections.*;

/**
 * Base class for the payload part of VendorMessages.  Also serves a similar
 * purpose as Message - you'll be able to get different VendorMessagePayload
 * subclasses here....
 */
public abstract class VendorMessagePayload {
    //Functional IDs defined by Gnutella VendorMessage protocol....
    public static final int F_HOPS_FLOW = 4;
    public static final int F_TCP_CONNECT_BACK = 7;
    public static final int F_UDP_CONNECT_BACK = 7;
    
    public static final byte[] F_LIME_VENDOR_ID = {(byte) 76, (byte) 73,
                                                   (byte) 77, (byte) 69};
    public static final byte[] F_BEAR_VENDOR_ID = {(byte) 66, (byte) 69,
                                                   (byte) 65, (byte) 82};
    public static final byte[] F_GTKG_VENDOR_ID = {(byte) 71, (byte) 84,
                                                   (byte) 75, (byte) 72};

    //----------------------
    // INSTANCE DATA
    //----------------------
    private int _version = -1;
    private int _selector = -1;
    private byte[] _vendorID = null;

    VendorMessagePayload(byte[] vendorID, int version, int selector) 
        throws IllegalArgumentException {
        if (vendorID.length != 4)
            throw new IllegalArgumentException("Vendor ID Invalid!");
        if ((selector & 0xFFFF0000) != 0)
            throw new IllegalArgumentException("Selector Invalid!");
        if ((version & 0xFFFF0000) != 0)
            throw new IllegalArgumentException("Version Invalid!");
        _vendorID = vendorID;
        _selector = selector;
        _version = version;
    }

    //----------------------

    //----------------------
    // ACCESSORS
    //----------------------

    protected byte[] getVendorIDBytes() {
        return _vendorID;
    }

    public String getVendorID() {
        return new String(_vendorID);
    }


    //----------------------
    // Methods for all subclasses....
    //----------------------

    static VendorMessagePayload getVendorMessagePayload(byte[] vendorID,
                                                        int selector, 
                                                        int version,
                                                        byte[] payload)
        throws BadPacketException {
        if ((selector == F_HOPS_FLOW) && (Arrays.equals(vendorID, 
                                                        F_BEAR_VENDOR_ID)))
            // HOPS FLOW MESSAGE
            ;
        if ((selector == F_TCP_CONNECT_BACK) && 
            (Arrays.equals(vendorID, F_BEAR_VENDOR_ID)))
            // TCP CONNECT BACK
            return new TCPConnectBackVMP(version, payload);
        if ((selector == F_UDP_CONNECT_BACK) && 
            (Arrays.equals(vendorID, F_GTKG_VENDOR_ID)))
            // UDP CONNECT BACK
            return new UDPConnectBackVMP(version, payload);
        throw new BadPacketException("Unrecognized Vendor Message");
    }
    

    /**
     * Use this to get a NEW Gnutella-able VendorMessage from the class.
     */
    public VendorMessage getVendorMessage() {
        try {
            return new VendorMessage(_vendorID, _selector, _version, 
                                     getPayload());
        }
        catch (IllegalArgumentException impossible) {
            impossible.printStackTrace();
        }
        return null;
    }
    
    //----------------------
    
    //----------------------
    // ABSTRACT METHODS
    //----------------------

    /** No need to store the payload here - subclasses can do it themselves.
     *  But we should be able to access it.  This also forces subclasses to
     *  construct the payload.
     */
    protected abstract byte[] getPayload();

    //----------------------
    
}
