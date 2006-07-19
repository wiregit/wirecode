package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.BandwidthStat;

public class StatisticVendorMessage extends VendorMessage {
    
    public static final int VERSION = 1;

    private static final String DELIMITER = " | ";
    
    private static final String DELIMITER2 = " ^ ";
    

    /**
     * Constructor for a StatisticVendorMessage read off the network, meaing it
     * was received in respose to a GiveStatsVendorMessage sent by this node.
     */
    public StatisticVendorMessage(byte[] guid, byte ttl, byte hops, 
                                   int version, byte[] payload) 
                                                     throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_STATISTICS, version,
              payload);
    }

    /**
     * Constructor to make a StatisticVendorMessage in response to a
     * GiveStatisticsVendorMessage. This is an outgoing StatisticVendorMessage
     */
    public StatisticVendorMessage(GiveStatsVendorMessage giveStatVM) {
        super(F_LIME_VENDOR_ID, F_STATISTICS, VERSION, 
                                                  derivePayload(giveStatVM));
    }
    
    /**
     * Determines whether or not we know how to respond to the GiveStats msg.
     */
    public static boolean isSupported(GiveStatsVendorMessage vm) {
        switch(vm.getStatType()) {
        case GiveStatsVendorMessage.GNUTELLA_INCOMING_TRAFFIC:
        case GiveStatsVendorMessage.GNUTELLA_OUTGOING_TRAFFIC:
            switch(vm.getStatControl()) {
            case GiveStatsVendorMessage.PER_CONNECTION_STATS:
            case GiveStatsVendorMessage.ALL_CONNECTIONS_STATS:
            case GiveStatsVendorMessage.UP_CONNECTIONS_STATS:
            case GiveStatsVendorMessage.LEAF_CONNECTIONS_STATS:
                return true;
            default:
                return false;
            }
        case GiveStatsVendorMessage.HTTP_DOWNLOAD_TRAFFIC_STATS:
        case GiveStatsVendorMessage.HTTP_UPLOAD_TRAFFIC_STATS:            
            return true;
        default:
            return false;
        }
    }
    
    private static byte[] derivePayload(GiveStatsVendorMessage giveStatsVM) {
        byte control = giveStatsVM.getStatControl();
        byte type = giveStatsVM.getStatType();
        byte[] part1 = {control, type};
        //write the type of stats we are writing out
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(part1);
        } catch(IOException iox) {
            ErrorService.error(iox); // impossible.
        }
        byte[] part2;

        StringBuffer buff;
        switch(type) {
        case GiveStatsVendorMessage.GNUTELLA_INCOMING_TRAFFIC:
        case GiveStatsVendorMessage.GNUTELLA_OUTGOING_TRAFFIC:
            boolean incoming = 
            type==GiveStatsVendorMessage.GNUTELLA_INCOMING_TRAFFIC;
            part2 = getGnutellaStats(control,incoming);
            break;
        case GiveStatsVendorMessage.HTTP_DOWNLOAD_TRAFFIC_STATS:
            //NOTE: in this case we ignore the granularity control, since
            //HTTP traffic is not connection specific
            buff = new StringBuffer();
            buff.append(
                     BandwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH.getTotal());
            buff.append(DELIMITER2);
            buff.append(
                      BandwidthStat.HTTP_BODY_DOWNSTREAM_BANDWIDTH.getTotal());
            
            part2 = buff.toString().getBytes();
            break;
        case GiveStatsVendorMessage.HTTP_UPLOAD_TRAFFIC_STATS:
            //NOTE: in this case we ignore the granularity control, since
            //HTTP traffic is not connection specific
            buff = new StringBuffer();
            buff.append(
                       BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.getTotal());
            buff.append(DELIMITER2);
            buff.append(BandwidthStat.HTTP_BODY_UPSTREAM_BANDWIDTH.getTotal());
            part2 = buff.toString().getBytes();
            break;
        default:
            throw new IllegalArgumentException("unknown type: " + type);
        }
        
        
        try {
            baos.write(part2);
        } catch (IOException iox) {
            ErrorService.error(iox); // impossible.
        }        
        return baos.toByteArray();
    }
 

    private static byte[] getGnutellaStats(byte control, boolean incoming) {
        List<ManagedConnection> conns = RouterService.getConnectionManager().getConnections();
        StringBuffer buff = new StringBuffer();

        switch(control) {
        case GiveStatsVendorMessage.PER_CONNECTION_STATS:
            for(ManagedConnection c : conns) {
                buff.append(c.toString());
                buff.append(DELIMITER2);
                if(incoming) {
                    buff.append(c.getNumMessagesReceived());
                    buff.append(DELIMITER);
                    buff.append(c.getNumReceivedMessagesDropped());
                }
                else {
                    buff.append(c.getNumMessagesSent());
                    buff.append(DELIMITER);
                    buff.append(c.getNumSentMessagesDropped());
                }
                buff.append(DELIMITER2);
            }
            return buff.toString().getBytes();
        case GiveStatsVendorMessage.ALL_CONNECTIONS_STATS:
            int messages = -1;
            int dropped = -1;
            for(ManagedConnection c : conns) {
                messages += incoming ? c.getNumMessagesReceived() : 
                                               c.getNumMessagesSent();
                dropped += incoming ? c.getNumReceivedMessagesDropped() :
                                               c.getNumSentMessagesDropped();
            }
            buff.append(messages);
            buff.append(DELIMITER);
            buff.append(dropped);
            return buff.toString().getBytes();
        case GiveStatsVendorMessage.UP_CONNECTIONS_STATS:
            for(ManagedConnection c: conns) {
                if(!c.isSupernodeConnection())
                    continue;
                buff.append(c.toString());
                buff.append(DELIMITER2);
                if(incoming) {
                    buff.append(c.getNumMessagesReceived());
                    buff.append(DELIMITER);
                    buff.append(c.getNumReceivedMessagesDropped());
                }
                else {
                    buff.append(c.getNumMessagesSent());
                    buff.append(DELIMITER);
                    buff.append(c.getNumSentMessagesDropped());
                }
                buff.append(DELIMITER2);
            }
            return buff.toString().getBytes();
        case GiveStatsVendorMessage.LEAF_CONNECTIONS_STATS:
            for(ManagedConnection c : conns) {
                if(!c.isLeafConnection())
                    continue;
                buff.append(c.toString());
                buff.append(DELIMITER2);
                if(incoming) {
                    buff.append(c.getNumMessagesReceived());
                    buff.append(DELIMITER);
                    buff.append(c.getNumReceivedMessagesDropped());
                }
                else {
                    buff.append(c.getNumMessagesSent());
                    buff.append(DELIMITER);
                    buff.append(c.getNumSentMessagesDropped());
                }
                buff.append(DELIMITER2);
            }
            return buff.toString().getBytes();
        default:
            throw new IllegalArgumentException("unknown control: " + control);
        }
    }
 
    public String getReportedStats() {
        return new String(getPayload());
    }
   
}
