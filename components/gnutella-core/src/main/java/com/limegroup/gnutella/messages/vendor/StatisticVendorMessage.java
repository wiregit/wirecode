package com.limegroup.gnutella.messages.vendor;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.*;
import java.io.*;

public class StatisticVendorMessage extends VendorMessage {
    
    private static final int VERSION = 1;

    private static final String DELIMITER = " | ";

    /**
     * Constructor for a StatisticVendorMessage read off the network, meaing it
     * was received in respose to a GiveStatsVendorMessage sent by this node.
     */
    public StatisticVendorMessage(byte[] guid, byte ttl, byte hops, 
                                   int version, byte[] payload) 
                                                     throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_GIVE_STATS, version,
              payload);
    }

    /**
     * Constructor to make a StatisticVendorMessage in response to a
     * GiveStatisticsVendorMessage. This is an outgoing StatisticVendorMessage
     */
    public StatisticVendorMessage(GiveStatsVendorMessage giveStatVM) 
                                                  throws BadPacketException {
        super(F_LIME_VENDOR_ID, F_STATISTICS, VERSION, 
                                                  derivePayload(giveStatVM));
    }
    
    private static byte[] derivePayload(GiveStatsVendorMessage giveStatsVM) 
                                                     throws BadPacketException {
        byte control = giveStatsVM.getStatControl();
        byte type = giveStatsVM.getStatType();
        
        if(type == GiveStatsVendorMessage.GNUTELLA_INCOMING_TRAFFIC ||
           type == GiveStatsVendorMessage.GNUTELLA_OUTGOING_TRAFFIC) {

            boolean incoming = 
            type==GiveStatsVendorMessage.GNUTELLA_INCOMING_TRAFFIC;
            
            return getGnutellaStats(control,incoming);
        }
        else if(type == GiveStatsVendorMessage.HTTP_DOWNLOAD_TRAFFIC_STATS) {
            //NOTE: in this case we ignore the granularity control, since HTTP
            //traffic is not connection specific            
            StringBuffer buff = new StringBuffer();
            buff.append("HTTP header downstream bandwidth :");
            buff.append(
                   BandwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH.getTotal());
            buff.append("HTTP body downstream bandwidth :");
            buff.append(
                      BandwidthStat.HTTP_BODY_DOWNSTREAM_BANDWIDTH.getTotal());
                      
            return buff.toString().getBytes();
        }
        else if( type == GiveStatsVendorMessage.HTTP_UPLOAD_TRAFFIC_STATS) {
            //NOTE: in this case we ignore the granularity control, since HTTP
            //traffic is not connection specific            
            StringBuffer buff = new StringBuffer();
            buff.append("HTTP header upstream bandwidth :");
            buff.append(
                      BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.getTotal());
            buff.append("HTTP body upstream bandwidth :");
            buff.append(BandwidthStat.HTTP_BODY_UPSTREAM_BANDWIDTH.getTotal());
            return buff.toString().getBytes();            
        }
        else {
            throw new BadPacketException("unknown stat type requested");
        }
    }
 

    private static byte[] getGnutellaStats(byte control, boolean incoming) 
                                                     throws BadPacketException {
        List conns = RouterService.getConnectionManager().getConnections();
        StringBuffer buff = new StringBuffer();

        if(control == GiveStatsVendorMessage.PER_CONNECTION_STATS) {
            for(Iterator iter = conns.iterator(); iter.hasNext() ; ) {
                ManagedConnection c = (ManagedConnection)iter.next();
                buff.append(c.toString());
                buff.append(" bandwidth ");
                if(incoming) {
                    buff.append("(incoming) :");
                    buff.append(c.getMeasuredDownstreamBandwidth());
                }
                else {
                    buff.append("(outgoing) :");
                    buff.append(c.getMeasuredUpstreamBandwidth());
                }
                buff.append(DELIMITER);
            }
            return buff.toString().getBytes();
        }
        else if(control == GiveStatsVendorMessage.ALL_CONNECTIONS_STATS) {
            float bandwidth = -1;
            for(Iterator iter = conns.iterator(); iter.hasNext() ; ) {
                ManagedConnection c = (ManagedConnection)iter.next();
                bandwidth += incoming ? c.getMeasuredDownstreamBandwidth() : 
                                               c.getMeasuredUpstreamBandwidth();
            }
            buff.append("All connections bandwidth ");
            if(incoming)
                buff.append("(incoming) :");
            else
                buff.append("(outgoing) :");
            buff.append(bandwidth);
            return buff.toString().getBytes();
        }
        else if(control == GiveStatsVendorMessage.UP_CONNECTIONS_STATS) {
            for(Iterator iter = conns.iterator(); iter.hasNext() ; ) {
                ManagedConnection c = (ManagedConnection)iter.next();
                if(!c.isSupernodeConnection())
                    continue;
                buff.append(c.toString());
                buff.append(" bandwidth ");
                if(incoming) {
                    buff.append("(incoming) :");
                    buff.append(c.getMeasuredDownstreamBandwidth());
                }
                else {
                    buff.append("(outgoing) :");
                    buff.append(c.getMeasuredUpstreamBandwidth());
                }
                buff.append(DELIMITER);
            }
            return buff.toString().getBytes();
        }
        else if(control == GiveStatsVendorMessage.LEAF_CONNECTIONS_STATS) {
            for(Iterator iter = conns.iterator(); iter.hasNext() ; ) {
                ManagedConnection c = (ManagedConnection)iter.next();
                if(!c.isLeafConnection())
                    continue;
                buff.append(c.toString());
                buff.append(" bandwidth ");
                if(incoming) {
                    buff.append("(incoming) :");
                    buff.append(c.getMeasuredDownstreamBandwidth());
                }
                else {
                    buff.append("(outgoing) :");
                    buff.append(c.getMeasuredUpstreamBandwidth());
                }
                buff.append(DELIMITER);
            }
            return buff.toString().getBytes();
        }
        else {
            throw new BadPacketException("unknown granularity requested");
        }
    }
    
}
