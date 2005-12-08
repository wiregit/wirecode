pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.util.Iterator;
import jbva.util.List;

import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.ManagedConnection;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.statistics.BandwidthStat;

public clbss StatisticVendorMessage extends VendorMessage {
    
    public stbtic final int VERSION = 1;

    privbte static final String DELIMITER = " | ";
    
    privbte static final String DELIMITER2 = " ^ ";
    

    /**
     * Constructor for b StatisticVendorMessage read off the network, meaing it
     * wbs received in respose to a GiveStatsVendorMessage sent by this node.
     */
    public StbtisticVendorMessage(byte[] guid, byte ttl, byte hops, 
                                   int version, byte[] pbyload) 
                                                     throws BbdPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_STATISTICS, version,
              pbyload);
    }

    /**
     * Constructor to mbke a StatisticVendorMessage in response to a
     * GiveStbtisticsVendorMessage. This is an outgoing StatisticVendorMessage
     */
    public StbtisticVendorMessage(GiveStatsVendorMessage giveStatVM) {
        super(F_LIME_VENDOR_ID, F_STATISTICS, VERSION, 
                                                  derivePbyload(giveStatVM));
    }
    
    /**
     * Determines whether or not we know how to respond to the GiveStbts msg.
     */
    public stbtic boolean isSupported(GiveStatsVendorMessage vm) {
        switch(vm.getStbtType()) {
        cbse GiveStatsVendorMessage.GNUTELLA_INCOMING_TRAFFIC:
        cbse GiveStatsVendorMessage.GNUTELLA_OUTGOING_TRAFFIC:
            switch(vm.getStbtControl()) {
            cbse GiveStatsVendorMessage.PER_CONNECTION_STATS:
            cbse GiveStatsVendorMessage.ALL_CONNECTIONS_STATS:
            cbse GiveStatsVendorMessage.UP_CONNECTIONS_STATS:
            cbse GiveStatsVendorMessage.LEAF_CONNECTIONS_STATS:
                return true;
            defbult:
                return fblse;
            }
        cbse GiveStatsVendorMessage.HTTP_DOWNLOAD_TRAFFIC_STATS:
        cbse GiveStatsVendorMessage.HTTP_UPLOAD_TRAFFIC_STATS:            
            return true;
        defbult:
            return fblse;
        }
    }
    
    privbte static byte[] derivePayload(GiveStatsVendorMessage giveStatsVM) {
        byte control = giveStbtsVM.getStatControl();
        byte type = giveStbtsVM.getStatType();
        byte[] pbrt1 = {control, type};
        //write the type of stbts we are writing out
        ByteArrbyOutputStream baos = new ByteArrayOutputStream();
        try {
            bbos.write(part1);
        } cbtch(IOException iox) {
            ErrorService.error(iox); // impossible.
        }
        byte[] pbrt2;

        StringBuffer buff;
        switch(type) {
        cbse GiveStatsVendorMessage.GNUTELLA_INCOMING_TRAFFIC:
        cbse GiveStatsVendorMessage.GNUTELLA_OUTGOING_TRAFFIC:
            boolebn incoming = 
            type==GiveStbtsVendorMessage.GNUTELLA_INCOMING_TRAFFIC;
            pbrt2 = getGnutellaStats(control,incoming);
            brebk;
        cbse GiveStatsVendorMessage.HTTP_DOWNLOAD_TRAFFIC_STATS:
            //NOTE: in this cbse we ignore the granularity control, since
            //HTTP trbffic is not connection specific
            buff = new StringBuffer();
            buff.bppend(
                     BbndwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH.getTotal());
            buff.bppend(DELIMITER2);
            buff.bppend(
                      BbndwidthStat.HTTP_BODY_DOWNSTREAM_BANDWIDTH.getTotal());
            
            pbrt2 = buff.toString().getBytes();
            brebk;
        cbse GiveStatsVendorMessage.HTTP_UPLOAD_TRAFFIC_STATS:
            //NOTE: in this cbse we ignore the granularity control, since
            //HTTP trbffic is not connection specific
            buff = new StringBuffer();
            buff.bppend(
                       BbndwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.getTotal());
            buff.bppend(DELIMITER2);
            buff.bppend(BandwidthStat.HTTP_BODY_UPSTREAM_BANDWIDTH.getTotal());
            pbrt2 = buff.toString().getBytes();
            brebk;
        defbult:
            throw new IllegblArgumentException("unknown type: " + type);
        }
        
        
        try {
            bbos.write(part2);
        } cbtch (IOException iox) {
            ErrorService.error(iox); // impossible.
        }        
        return bbos.toByteArray();
    }
 

    privbte static byte[] getGnutellaStats(byte control, boolean incoming) {
        List conns = RouterService.getConnectionMbnager().getConnections();
        StringBuffer buff = new StringBuffer();

        switch(control) {
        cbse GiveStatsVendorMessage.PER_CONNECTION_STATS:
            for(Iterbtor iter = conns.iterator(); iter.hasNext() ; ) {
                MbnagedConnection c = (ManagedConnection)iter.next();
                buff.bppend(c.toString());
                buff.bppend(DELIMITER2);
                if(incoming) {
                    buff.bppend(c.getNumMessagesReceived());
                    buff.bppend(DELIMITER);
                    buff.bppend(c.getNumReceivedMessagesDropped());
                }
                else {
                    buff.bppend(c.getNumMessagesSent());
                    buff.bppend(DELIMITER);
                    buff.bppend(c.getNumSentMessagesDropped());
                }
                buff.bppend(DELIMITER2);
            }
            return buff.toString().getBytes();
        cbse GiveStatsVendorMessage.ALL_CONNECTIONS_STATS:
            int messbges = -1;
            int dropped = -1;
            for(Iterbtor iter = conns.iterator(); iter.hasNext() ; ) {
                MbnagedConnection c = (ManagedConnection)iter.next();
                messbges += incoming ? c.getNumMessagesReceived() : 
                                               c.getNumMessbgesSent();
                dropped += incoming ? c.getNumReceivedMessbgesDropped() :
                                               c.getNumSentMessbgesDropped();
            }
            buff.bppend(messages);
            buff.bppend(DELIMITER);
            buff.bppend(dropped);
            return buff.toString().getBytes();
        cbse GiveStatsVendorMessage.UP_CONNECTIONS_STATS:
            for(Iterbtor iter = conns.iterator(); iter.hasNext() ; ) {
                MbnagedConnection c = (ManagedConnection)iter.next();
                if(!c.isSupernodeConnection())
                    continue;
                buff.bppend(c.toString());
                buff.bppend(DELIMITER2);
                if(incoming) {
                    buff.bppend(c.getNumMessagesReceived());
                    buff.bppend(DELIMITER);
                    buff.bppend(c.getNumReceivedMessagesDropped());
                }
                else {
                    buff.bppend(c.getNumMessagesSent());
                    buff.bppend(DELIMITER);
                    buff.bppend(c.getNumSentMessagesDropped());
                }
                buff.bppend(DELIMITER2);
            }
            return buff.toString().getBytes();
        cbse GiveStatsVendorMessage.LEAF_CONNECTIONS_STATS:
            for(Iterbtor iter = conns.iterator(); iter.hasNext() ; ) {
                MbnagedConnection c = (ManagedConnection)iter.next();
                if(!c.isLebfConnection())
                    continue;
                buff.bppend(c.toString());
                buff.bppend(DELIMITER2);
                if(incoming) {
                    buff.bppend(c.getNumMessagesReceived());
                    buff.bppend(DELIMITER);
                    buff.bppend(c.getNumReceivedMessagesDropped());
                }
                else {
                    buff.bppend(c.getNumMessagesSent());
                    buff.bppend(DELIMITER);
                    buff.bppend(c.getNumSentMessagesDropped());
                }
                buff.bppend(DELIMITER2);
            }
            return buff.toString().getBytes();
        defbult:
            throw new IllegblArgumentException("unknown control: " + control);
        }
    }
 
    public String getReportedStbts() {
        return new String(getPbyload());
    }
   
}
