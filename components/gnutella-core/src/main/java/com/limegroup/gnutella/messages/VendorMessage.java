package com.limegroup.gnutella.messages;

import java.io.*;
import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteOrder;

/** Vendor Messages are Gnutella Messages that are NEVER forwarded after
 *  recieved.  They have sub-messages in their payloads.
 */
public class VendorMessage extends Message {

    private static final int LENGTH_MINUS_PAYLOAD = 8;
    private static final byte[] LIME_BYTES = {(byte) 76, (byte) 73,
                                              (byte) 77, (byte) 69};
                                              

    /**
     * Bytes 0-3 of the Vendor Message.  Something like "LIME".getBytes().
     */
    private byte[] _vendorIDBytes = null;

    /**
     * The Sub-Selector for this message.  Bytes 4-5 of the Vendor Message.
     */
    private int _subSelector = -1;

    /**
     * The Version number of the message.  Bytes 6-7 of the Vendor Message.
     */
    private int _version = -1;

    /** The actual payload of the message.
     */
    byte[] _payload = null;

    //----------------------------------
    // CONSTRUCTORS
    //----------------------------------


    /** Package Level access only.  Used by specific vendor-message to
     *  construct a Vendor Message.
     *  @param vendorIDBytes The Vendor ID of this message (bytes).  
     *  @param selector The selector of the message.
     *  @param version  The version of this message.
     *  @param payload  The actual semantic meaning.  Should not be null!
     *  @exception IllegalArgumentException Thrown if vendorIDBytes, selector,
     *  or version is 'too big'.
     */
    VendorMessage(byte[] vendorIDBytes, int selector, int version, 
                  byte[] payload) throws IllegalArgumentException {
        super((byte)0x31, (byte)1, LENGTH_MINUS_PAYLOAD + payload.length);
        if ((vendorIDBytes.length != 4))
            throw new IllegalArgumentException("Vendor ID Invalid!");
        if ((selector & 0xFFFF0000) != 0)
            throw new IllegalArgumentException("Selector Invalid!");
        if ((version & 0xFFFF0000) != 0)
            throw new IllegalArgumentException("Version Invalid!");
        _vendorIDBytes = vendorIDBytes;
        _subSelector = selector;
        _version = version;
        _payload = payload;
    }

    /** Should be used when encountered a Message from the Network.  Primarily
        built for the convenience of the class Message.
     */
    VendorMessage(byte[] guid, byte ttl, byte hops, 
                  byte[] fromNetwork) {
        super(guid, (byte)0x31, ttl, hops, fromNetwork.length);
        int index = 0;
        _vendorIDBytes = new byte[4];
        // first 4 bytes are vendor ID
        for (int i = 0; i < _vendorIDBytes.length; i++, index++)
            _vendorIDBytes[i] = fromNetwork[index];
        _subSelector = ByteOrder.ubytes2int(ByteOrder.leb2short(fromNetwork,
                                                                index));
        index += 2; // skip past selector....
        _version = ByteOrder.ubytes2int(ByteOrder.leb2short(fromNetwork,
                                                                index));
        index += 2; // skip past version....
        Assert.that(index == (LENGTH_MINUS_PAYLOAD));
        _payload = new byte[fromNetwork.length-LENGTH_MINUS_PAYLOAD];
        // lastly copy the payload....
        System.arraycopy(fromNetwork, index, _payload, 0, _payload.length);
    }

    //----------------------------------


    //----------------------------------
    // ACCESSOR methods
    //----------------------------------

    public String getVendorID() {
        return new String(_vendorIDBytes);
    }

    public VendorMessagePayload getVendorMessagePayload() {
        return VendorMessagePayload.getVendorMessagePayload(_vendorIDBytes,
                                                            _subSelector,
                                                            _version,
                                                            _payload);
    }

    //----------------------------------



    //----------------------------------
    // FULFILL abstract Message methods
    //----------------------------------

    // INHERIT COMMENT
    protected void writePayload(OutputStream out) throws IOException {
        out.write(_vendorIDBytes);
        ByteOrder.short2leb((short)_subSelector, out);
        ByteOrder.short2leb((short)_version, out);
        out.write(_payload);
    }

    // INHERIT COMMENT
    public Message stripExtendedPayload() {
        // doesn't make sense for VendorMessage to strip anything....
        return this;
    }

    // INHERIT COMMENT
    public void recordDrop() {
    }

    //----------------------------------


}
