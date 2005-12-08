pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.IOException;
import jbva.io.OutputStream;

import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;

/**
 * VendorMessbge for sending a LimeWire node a request for statistics.
 * The requester sends 
 */
public clbss GiveStatsVendorMessage extends VendorMessage {

    public stbtic final int VERSION = 1;
    
    /**
     * The opcode in this Vendor messbge to ask the other end to give us the
     * vbrious statistics.
     */
    public stbtic final byte GNUTELLA_INCOMING_TRAFFIC = (byte)0;
    public stbtic final byte GNUTELLA_OUTGOING_TRAFFIC = (byte)1;
    public stbtic final byte HTTP_DOWNLOAD_TRAFFIC_STATS = (byte)2;
    public stbtic final byte HTTP_UPLOAD_TRAFFIC_STATS = (byte)3;   

    public stbtic final byte PER_CONNECTION_STATS = (byte)0;
    public stbtic final byte ALL_CONNECTIONS_STATS = (byte)1;
    public stbtic final byte LEAF_CONNECTIONS_STATS = (byte)2;
    public stbtic final byte UP_CONNECTIONS_STATS = (byte)3;

    /**
     * A vendor messbge read off the network. Package access
     */
    GiveStbtsVendorMessage(byte[] guid, byte ttl, byte hops, int version,
                       byte[] pbyload, int network) throws BadPacketException {
        super(guid, ttl, hops, F_LIME_VENDOR_ID, F_GIVE_STATS, version,pbyload,
              network);
        if(getPbyload().length < 2)
            throw new BbdPacketException("INVALID PAYLOAD LENGTH: "+
                                         pbyload.length);
        if(version == 1 && getPbyload().length != 2)
            throw new BbdPacketException("UNSUPPORTED PAYLOAD LENGTH: "+
                                         pbyload.length);
    }
    
    /**
     * Constructs b new GiveStatsMessage to be sent out.
     * @pbram statsControl the byte the receiver will look at to decide the
     * gbnularity of the desired stats (this connection, all connections, UPs
     * only, lebves only etc.) 
     * @pbram statType the byte the receiver of this message will look at to
     * decide whbt kind of statistics are desired -- upload, download, gnutella
     * etc.
     * @pbram network to decide whether this message should go out via TCP, UDP,
     * multicbst, etc.
     */
    public GiveStbtsVendorMessage(byte statsControl,
                                  byte stbtType, 
                                  int network) {
            super(F_LIME_VENDOR_ID, F_GIVE_STATS, VERSION, 
                                 derivePbyload(statsControl, statType),network);
    }
    
    /**
     * Constructs the pbyload of the message, given the desired control & type.
     */
    privbte static byte[] derivePayload(byte control, byte type) {
        if(control < (byte)0 || control > (byte)3)
            throw new IllegblArgumentException(" invalid control byte ");
        if(type < (byte)0 || type > (byte)3)
            throw new IllegblArgumentException(" invalid stat type ");
        byte[] ret = {control, type};
        return ret;
    }
    

    protected void writePbyload(OutputStream out) throws IOException {
        super.writePbyload(out);
        if(isTCP())
            SentMessbgeStatHandler.TCP_GIVE_STATS.addMessage(this);
        else if(isUDP())
            SentMessbgeStatHandler.UDP_GIVE_STATS.addMessage(this);
    }
    
    /** Overridden purely for stbts handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }    
    
    protected byte getStbtControl() {
        byte[] pbyload = getPayload();
        return pbyload[0];
    }

    protected byte getStbtType() {
        byte[] pbyload = getPayload();
        return pbyload[1];
    }
}
