package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.messages.AbstractMessage;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;

/** Vendor Messages are Gnutella Messages that are NEVER forwarded after
 *  recieved.
 *  This message is abstract because it provides common methods for ALL
 *  VendorMessages, but it makes no sense to instantiate a VendorMessage.
 */
public abstract class VendorMessage extends AbstractMessage {

    //Functional IDs defined by Gnutella VendorMessage protocol....
    public static final int F_MESSAGES_SUPPORTED = 0;
    public static final int F_HOPS_FLOW = 4;
    public static final int F_TCP_CONNECT_BACK = 7;
    public static final int F_UDP_CONNECT_BACK = 7;
    public static final int F_UDP_CONNECT_BACK_REDIR = 8;
    public static final int F_CAPABILITIES = 10;
    public static final int F_LIME_ACK = 11;
    public static final int F_REPLY_NUMBER = 12;
    public static final int F_OOB_PROXYING_CONTROL = 13;
    public static final int F_PUSH_PROXY_REQ = 21;
    public static final int F_PUSH_PROXY_ACK = 22;
    @Deprecated
    public static final int F_GIVE_STATS = 14; 
    @Deprecated
    public static final int F_STATISTICS = 15; 
    public static final int F_CRAWLER_PING = 5;
    public static final int F_CRAWLER_PONG = 6;
    public static final int F_SIMPP_REQ = 16;
    public static final int F_SIMPP = 17;
    public static final int F_UDP_HEAD_PING = 23;
    public static final int F_UDP_HEAD_PONG = 24;
    public static final int F_HEADER_UPDATE = 25;
    public static final int F_UPDATE_REQ = 26;
    public static final int F_UPDATE_RESP = 27;
    public static final int F_CONTENT_REQ = 28;
    public static final int F_CONTENT_RESP = 29;
    public static final int F_INSPECTION_REQ = 30;
    public static final int F_INSPECTION_RESP = 31;
    public static final int F_ADVANCED_TOGGLE = 32;
    public static final int F_DHT_CONTACTS = 33;
    
    public static final byte[] F_LIME_VENDOR_ID = {(byte) 76, (byte) 73,
                                                      (byte) 77, (byte) 69};
    public static final byte[] F_BEAR_VENDOR_ID = {(byte) 66, (byte) 69,
                                                      (byte) 65, (byte) 82};
    public static final byte[] F_GTKG_VENDOR_ID = {(byte) 71, (byte) 84,
                                                      (byte) 75, (byte) 71};
    public static final byte[] F_NULL_VENDOR_ID = {(byte) 0, (byte) 0,
                                                      (byte) 0, (byte) 0};

    static final int LENGTH_MINUS_PAYLOAD = 8;
    
    /**
     * Bytes 0-3 of the Vendor Message.  Something like "LIME".getBytes().
     */
    private final byte[] _vendorID;

    /**
     * The Sub-Selector for this message.  Bytes 4-5 of the Vendor Message.
     */
    private final int _selector;

    /**
     * The Version number of the message.  Bytes 6-7 of the Vendor Message.
     */
    private final int _version;

    /**
     * The payload of this VendorMessage.  This usually holds data that is 
     * interpreted by the type of message determined by _vendorID, _selector,
     * and (to a lesser extent) _version.
     */
    private final byte[] _payload;

    /** Cache the hashcode cuz it isn't cheap to compute.
     */
    private final int _hashCode;

    //----------------------------------
    // CONSTRUCTORS
    //----------------------------------


    /**
     * Constructs a new VendorMessage with the given data.
     * Each Vendor Message class delegates to this constructor (or the one
     * also taking a network parameter) to construct new locally generated
     * VMs.
     *  @param vendorIDBytes The Vendor ID of this message (bytes).  
     *  @param selector The selector of the message.
     *  @param version  The version of this message.
     *  @param payload  The payload (not including vendorIDBytes, selector, and
     *  version.
     *  @exception NullPointerException Thrown if payload or vendorIDBytes are
     *  null.
     */
    protected VendorMessage(byte[] vendorIDBytes, int selector, int version, 
                            byte[] payload) {
        this(vendorIDBytes, selector, version, payload, Network.UNKNOWN);
    }
    
    /**
     * Constructs a new VendorMessage with the given data.
     * Each Vendor Message class delegates to this constructor (or the one that
     * doesn't take the network parameter) to construct new locally generated
     * VMs.
     *  @param vendorIDBytes The Vendor ID of this message (bytes).  
     *  @param selector The selector of the message.
     *  @param version  The version of this message.
     *  @param payload  The payload (not including vendorIDBytes, selector, and
     *  version.
     *  @param network The network this VM is to be written on.
     *  @exception NullPointerException Thrown if payload or vendorIDBytes are
     *  null.
     */
    protected VendorMessage(byte[] vendorIDBytes, int selector, int version, 
                            byte[] payload, Network network) {
        super(F_VENDOR_MESSAGE, (byte)1, LENGTH_MINUS_PAYLOAD + payload.length,
              network);
        if ((vendorIDBytes.length != 4))
            throw new IllegalArgumentException("wrong vendorID length: " +
                                                vendorIDBytes.length);
        if ((selector & 0xFFFF0000) != 0)
            throw new IllegalArgumentException("invalid selector: " + selector);
        if ((version & 0xFFFF0000) != 0)
            throw new IllegalArgumentException("invalid version: " + version);
        // set the instance params....
        _vendorID = vendorIDBytes;
        _selector = selector;
        _version = version;
        _payload = payload;
        // lastly compute the hash
        _hashCode = computeHashCode(_version, _selector, _vendorID, _payload);
    }

    /**
     * Constructs a new VendorMessage with data from the network.
     * Primarily built for the convenience of the class Message.
     * Subclasses must extend this (or the below constructor that takes a 
     * network parameter) and use getPayload() to parse the payload and do
     * anything else they need to.
     */
    protected VendorMessage(byte[] guid, byte ttl, byte hops, byte[] vendorID,
                            int selector, int version, byte[] payload) 
        throws BadPacketException {
        this(guid,ttl,hops,vendorID,selector,version,payload, Network.UNKNOWN);
    }

    /**
     * Constructs a new VendorMessage with data from the network.
     * Primarily built for the convenience of the class Message.
     * Subclasses must extend this (or the above constructor that doesn't 
     * takes a network parameter) and use getPayload() to parse the payload
     * and do anything else they need to.
     */
    protected VendorMessage(byte[] guid, byte ttl, byte hops,byte[] vendorID,
                            int selector, int version, byte[] payload, 
                            Network network) throws BadPacketException {
        super(guid, (byte)0x31, ttl, hops, LENGTH_MINUS_PAYLOAD+payload.length,
              network);
        // set the instance params....
        if ((vendorID.length != 4)) {
            ReceivedErrorStat.VENDOR_INVALID_ID.incrementStat();            
            throw new BadPacketException("Vendor ID Invalid!");
        }
        if ((selector & 0xFFFF0000) != 0) {
            ReceivedErrorStat.VENDOR_INVALID_SELECTOR.incrementStat();
            throw new BadPacketException("Selector Invalid!");
        }
        if ((version & 0xFFFF0000) != 0) {
            ReceivedErrorStat.VENDOR_INVALID_VERSION.incrementStat();
            throw new BadPacketException("Version Invalid!");
        }        
        _vendorID = vendorID;
        _selector = selector;
        _version = version;
        _payload = payload;
        // lastly compute the hash
        _hashCode = computeHashCode(_version, _selector, _vendorID,
                                    _payload);
    }

    /**
     * Computes the hash code for a vendor message.
     */
    private static int computeHashCode(int version, int selector, 
                                       byte[] vendorID, byte[] payload) {
        int hashCode = 0;
        hashCode += 17*version;
        hashCode += 17*selector;
        for (int i = 0; i < vendorID.length; i++)
            hashCode += 17*vendorID[i];
        for (int i = 0; i < payload.length; i++)
            hashCode += 17*payload[i];
        return hashCode;
    }

    //----------------------------------


    //----------------------------------
    // ACCESSOR methods
    //----------------------------------

    /** Allows subclasses to make changes gain access to the payload.  They 
     *  can:
     *  1) change the contents
     *  2) parse the contents.
     *  In general, 1) is discouraged, 2) is necessary.  Subclasses CANNOT
     *  re-init the payload.
     */
    protected byte[] getPayload() {
        return _payload;
    }

    public int getVersion() {
        return _version;
    }

    //----------------------------------

    //----------------------
    // Methods for all subclasses....
    //----------------------
    
    /**
     * @return true if the two VMPs have identical signatures - no more, no 
     * less.  Does not take version into account, but if different versions
     * have different payloads, they'll differ.
     */
    public boolean equals(Object other) {
        if (other instanceof VendorMessage) {
            VendorMessage vmp = (VendorMessage) other;
            return ((_selector == vmp._selector) &&
                    (Arrays.equals(_vendorID, vmp._vendorID)) &&
                    (Arrays.equals(_payload, vmp._payload))
                    );
        }
        return false;
    }
   
    public int hashCode() {
        return _hashCode;
    }
 
    //----------------------

    //----------------------
    // ABSTRACT METHODS
    //----------------------

    //----------------------


    //----------------------------------
    // FULFILL abstract Message methods
    //----------------------------------

    // INHERIT COMMENT
    protected void writePayload(OutputStream out) throws IOException {
        out.write(_vendorID);
        ByteOrder.short2leb((short)_selector, out);
        ByteOrder.short2leb((short)_version, out);
        writeVendorPayload(out);
    }
    
    protected void writeVendorPayload(OutputStream out) throws IOException {
        out.write(getPayload());
    }

    // INHERIT COMMENT
    public void recordDrop() {
    }

    //----------------------------------


}
