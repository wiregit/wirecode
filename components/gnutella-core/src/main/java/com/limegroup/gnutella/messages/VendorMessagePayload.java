package com.limegroup.gnutella.messages;

import java.io.*;

/**
 * Base class for the payload part of VendorMessages.  Also serves a similar
 * purpose as Message - you'll be able to get different VendorMessagePayload
 * subclasses here....
 */
public abstract class VendorMessagePayload {
    //Functional IDs defined by Gnutella VendorMessage protocol....
    public static final int F_HOPS_FLOW = 4;
    public static final int F_TCP_CONNECT_BACK = 7;

    //----------------------
    // INSTANCE DATA
    //----------------------
    protected byte[] _payload = null;
    protected int _version = -1;
    private int _selector = -1;

    //----------------------

    //----------------------
    // ACCESSORS
    //----------------------


    //----------------------
    // Methods for all subclasses....
    //----------------------

    static VendorMessagePayload getVendorMessagePayload(byte[] vendorID,
                                                        int selector, 
                                                        int version,
                                                        byte[] payload)
        throws IllegalArgumentException {
        return null;
    }
    

    /**
     * Use this to get a Gnutella-able VendorMessage from the class.
     */
    public VendorMessage getVendorMessage() {
        return null;
    }
    
    //----------------------
    
    
    //----------------------
    // ABSTRACT METHODS
    //----------------------
    
    public abstract void writePayload(OutputStream out) throws IOException;
    
    //----------------------
    
}
