package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.*;
import java.io.*;

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
    public static final byte GNUTELLA_INCOMING_TRAFFIC = (byte)1;
    
    public static final byte GNUTELLA_OUTGOING_TRAFFIC = (byte)2;

    public static final byte DOWNLOAD_TRAFFIC_STATS = (byte)3;
    
    public static final byte UPLOAD_TRAFFIC_STATS = (byte)4;   

    public static final byte PER_CONNECTION_STATS = (byte)1;
    public static final byte ALL_CONNECTIONS_STATS = (byte)2;


    /**
     * A vendor message read off the network. Package access
     */
    GiveStatsVendorMessage(byte[] guid, byte ttl, byte hops, int version,
                           byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_GIVE_STATS, version,
              payload);
        if(version == 1 && getPayload().length != 2)
            throw new BadPacketException("UNSUPPORTED PAYLOAD LENGTH: "+
                                         payload.length);
        //TODO1: OK. Find out what kind of stat is requested and return it
        //with the StatisticVendorMessage.
        //Note the response StatisticVendor message must go out in UDP, or
        //TCP depending on whether this message was received via UDP or TCP
        
    }
    
    /**
     * @param opCode the byte the receiver of the vendor message looks at to
     * decide what stats have been requested.
     */
    public GiveStatsVendorMessage(byte statsControl, byte statType) 
                                              throws BadPacketException {
            super(F_LIME_VENDOR_ID, F_GIVE_STATS, VERSION, 
                                        derivePayload(statsControl, statType));
    }
    
    private static byte[] derivePayload(byte control, byte type) {
        if(control < (byte)0 || control > (byte)2)
            throw new IllegalArgumentException(" invalid control byte ");
        if(type < (byte)0 || type > (byte)4 )
            throw new IllegalArgumentException(" invalid stat type ");
        byte[] ret = {control, type};
        return ret;
    }
    

    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        if(RECORD_STATS) {
            ;//TODO replace this with TCP/UDP stats
//              if(tcp)
//                  SentMessageStatHandler.TCP_GIVE_STATS.addMessage(this);
//              else //UDP
//                  SentMessageStatHandler.UDP_GIVE_STATS.addMessage(this);
        }
    }
    
    /**
     * Overriding abstract method, used for statistoics gathering. 
     */ 
    public void recordDrop() {
        super.recordDrop();
    }
    
}
