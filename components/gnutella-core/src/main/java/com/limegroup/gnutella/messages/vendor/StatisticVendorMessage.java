package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.*;
import java.io.*;

public class StatisticVendorMessage extends VendorMessage {
    
    private static final int VERSION = 1;

    /**
     * Constructor for a StatisticVendorMessage read off the network, meaing it
     * was received in respose to a GiveStatsVendorMessage sent by this node.
     */
    public StatisticVendorMessage(byte[] guid, byte ttl, byte hops, 
                                   int version, byte[] payload) 
                                                     throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_GIVE_STATS, version,
              payload);
        //TODO: write the payload out to a file.
    }

    /**
     * Constructor to make a StatisticVendorMessage in response to a
     * GiveStatisticsVendorMessage
     */
    public StatisticVendorMessage(GiveStatsVendorMessage giveStatVM) 
                                                  throws BadPacketException {
        super(F_LIME_VENDOR_ID, F_STATISTICS, VERSION, 
                                                  derivePayload(giveStatVM));
    }
    
    private static byte[] derivePayload(GiveStatsVendorMessage giveStatsVM) {
        byte control = giveStatsVM.getStatControl();
        byte type = giveStatsVM.getStatType();
        //TODO: gather the statistics to return and send it back.
        return null;
    }
    
}
