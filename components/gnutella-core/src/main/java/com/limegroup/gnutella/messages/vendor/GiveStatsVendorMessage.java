package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * VendorMessage for sending a LimeWire node a request for statistics.
 * The requester sends 
 */
public class GiveStatsVendorMessage extends VendorMessage {

    public static final int VERSION = 1;
    
    /**
     * The opcode in this Vendor message to ask the other end to give us the
     * various statistics.
     */
    public static final byte GNUTELLA_INCOMING_TRAFFIC = (byte)0;
    public static final byte GNUTELLA_OUTGOING_TRAFFIC = (byte)1;
    public static final byte HTTP_DOWNLOAD_TRAFFIC_STATS = (byte)2;
    public static final byte HTTP_UPLOAD_TRAFFIC_STATS = (byte)3;   

    public static final byte PER_CONNECTION_STATS = (byte)0;
    public static final byte ALL_CONNECTIONS_STATS = (byte)1;
    public static final byte LEAF_CONNECTIONS_STATS = (byte)2;
    public static final byte UP_CONNECTIONS_STATS = (byte)3;

    /**
     * A vendor message read off the network. Package access
     */
    GiveStatsVendorMessage(byte[] guid, byte ttl, byte hops, int version,
                       byte[] payload, Network network) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_GIVE_STATS, version,payload,
              network);
        if(getPayload().length < 2)
            throw new BadPacketException("INVALID PAYLOAD LENGTH: "+
                                         payload.length);
        if(version == 1 && getPayload().length != 2)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: "+
                                         payload.length);
    }
    
    /**
     * Constructs a new GiveStatsMessage to be sent out.
     * @param statsControl the byte the receiver will look at to decide the
     * ganularity of the desired stats (this connection, all connections, UPs
     * only, leaves only etc.) 
     * @param statType the byte the receiver of this message will look at to
     * decide what kind of statistics are desired -- upload, download, gnutella
     * etc.
     * @param network to decide whether this message should go out via TCP, UDP,
     * multicast, etc.
     */
    public GiveStatsVendorMessage(byte statsControl,
                                  byte statType, 
                                  Network network) {
            super(F_LIME_VENDOR_ID, F_GIVE_STATS, VERSION, 
                                 derivePayload(statsControl, statType),network);
    }
    
    /**
     * Constructs the payload of the message, given the desired control & type.
     */
    private static byte[] derivePayload(byte control, byte type) {
        if(control < (byte)0 || control > (byte)3)
            throw new IllegalArgumentException(" invalid control byte ");
        if(type < (byte)0 || type > (byte)3)
            throw new IllegalArgumentException(" invalid stat type ");
        byte[] ret = {control, type};
        return ret;
    }
    

    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        if(isTCP())
            SentMessageStatHandler.TCP_GIVE_STATS.addMessage(this);
        else if(isUDP())
            SentMessageStatHandler.UDP_GIVE_STATS.addMessage(this);
    }
    
    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }    
    
    protected byte getStatControl() {
        byte[] payload = getPayload();
        return payload[0];
    }

    protected byte getStatType() {
        byte[] payload = getPayload();
        return payload[1];
    }
}
