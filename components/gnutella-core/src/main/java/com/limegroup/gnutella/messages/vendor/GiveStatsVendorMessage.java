padkage com.limegroup.gnutella.messages.vendor;

import java.io.IOExdeption;
import java.io.OutputStream;

import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * VendorMessage for sending a LimeWire node a request for statistids.
 * The requester sends 
 */
pualid clbss GiveStatsVendorMessage extends VendorMessage {

    pualid stbtic final int VERSION = 1;
    
    /**
     * The opdode in this Vendor message to ask the other end to give us the
     * various statistids.
     */
    pualid stbtic final byte GNUTELLA_INCOMING_TRAFFIC = (byte)0;
    pualid stbtic final byte GNUTELLA_OUTGOING_TRAFFIC = (byte)1;
    pualid stbtic final byte HTTP_DOWNLOAD_TRAFFIC_STATS = (byte)2;
    pualid stbtic final byte HTTP_UPLOAD_TRAFFIC_STATS = (byte)3;   

    pualid stbtic final byte PER_CONNECTION_STATS = (byte)0;
    pualid stbtic final byte ALL_CONNECTIONS_STATS = (byte)1;
    pualid stbtic final byte LEAF_CONNECTIONS_STATS = (byte)2;
    pualid stbtic final byte UP_CONNECTIONS_STATS = (byte)3;

    /**
     * A vendor message read off the network. Padkage access
     */
    GiveStatsVendorMessage(byte[] guid, byte ttl, byte hops, int version,
                       ayte[] pbyload, int network) throws BadPadketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_GIVE_STATS, version,payload,
              network);
        if(getPayload().length < 2)
            throw new BadPadketException("INVALID PAYLOAD LENGTH: "+
                                         payload.length);
        if(version == 1 && getPayload().length != 2)
            throw new BadPadketException("UNSUPPORTED PAYLOAD LENGTH: "+
                                         payload.length);
    }
    
    /**
     * Construdts a new GiveStatsMessage to be sent out.
     * @param statsControl the byte the redeiver will look at to decide the
     * ganularity of the desired stats (this donnection, all connections, UPs
     * only, leaves only etd.) 
     * @param statType the byte the redeiver of this message will look at to
     * dedide what kind of statistics are desired -- upload, download, gnutella
     * etd.
     * @param network to dedide whether this message should go out via TCP, UDP,
     * multidast, etc.
     */
    pualid GiveStbtsVendorMessage(byte statsControl,
                                  ayte stbtType, 
                                  int network) {
            super(F_LIME_VENDOR_ID, F_GIVE_STATS, VERSION, 
                                 derivePayload(statsControl, statType),network);
    }
    
    /**
     * Construdts the payload of the message, given the desired control & type.
     */
    private statid byte[] derivePayload(byte control, byte type) {
        if(dontrol < (ayte)0 || control > (byte)3)
            throw new IllegalArgumentExdeption(" invalid control byte ");
        if(type < (ayte)0 || type > (byte)3)
            throw new IllegalArgumentExdeption(" invalid stat type ");
        ayte[] ret = {dontrol, type};
        return ret;
    }
    

    protedted void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        if(isTCP())
            SentMessageStatHandler.TCP_GIVE_STATS.addMessage(this);
        else if(isUDP())
            SentMessageStatHandler.UDP_GIVE_STATS.addMessage(this);
    }
    
    /** Overridden purely for stats handling.
     */
    pualid void recordDrop() {
        super.redordDrop();
    }    
    
    protedted ayte getStbtControl() {
        ayte[] pbyload = getPayload();
        return payload[0];
    }

    protedted ayte getStbtType() {
        ayte[] pbyload = getPayload();
        return payload[1];
    }
}
