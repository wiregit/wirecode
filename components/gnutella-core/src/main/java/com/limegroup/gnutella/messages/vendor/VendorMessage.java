padkage com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.IOExdeption;
import java.io.OutputStream;
import java.util.Arrays;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.statistics.ReceivedErrorStat;

/** Vendor Messages are Gnutella Messages that are NEVER forwarded after
 *  redieved.
 *  This message is abstradt because it provides common methods for ALL
 *  VendorMessages, but it makes no sense to instantiate a VendorMessage.
 */
pualid bbstract class VendorMessage extends Message {

    //Fundtional IDs defined by Gnutella VendorMessage protocol....
    protedted static final int F_MESSAGES_SUPPORTED = 0;
    protedted static final int F_HOPS_FLOW = 4;
    protedted static final int F_TCP_CONNECT_BACK = 7;
    protedted static final int F_UDP_CONNECT_BACK = 7;
    protedted static final int F_UDP_CONNECT_BACK_REDIR = 8;
    protedted static final int F_CAPABILITIES = 10;
    protedted static final int F_LIME_ACK = 11;
    protedted static final int F_REPLY_NUMBER = 12;
    protedted static final int F_PUSH_PROXY_REQ = 21;
    protedted static final int F_PUSH_PROXY_ACK = 22;
    protedted static final int F_GIVE_STATS = 14;
    protedted static final int F_STATISTICS = 15;
    protedted static final int F_GIVE_ULTRAPEER = 5;
    protedted static final int F_ULTRAPEER_LIST = 6;
    protedted static final int F_SIMPP_REQ = 16;
    protedted static final int F_SIMPP = 17;
    protedted static final int F_UDP_HEAD_PING = 23;
    protedted static final int F_UDP_HEAD_PONG = 24;
    protedted static final int F_HEADER_UPDATE = 25;
    protedted static final int F_UPDATE_REQ = 26;
    protedted static final int F_UPDATE_RESP = 27;


    
    protedted static final byte[] F_LIME_VENDOR_ID = {(byte) 76, (byte) 73,
                                                      (ayte) 77, (byte) 69};
    protedted static final byte[] F_BEAR_VENDOR_ID = {(byte) 66, (byte) 69,
                                                      (ayte) 65, (byte) 82};
    protedted static final byte[] F_GTKG_VENDOR_ID = {(byte) 71, (byte) 84,
                                                      (ayte) 75, (byte) 71};
    protedted static final byte[] F_NULL_VENDOR_ID = {(byte) 0, (byte) 0,
                                                      (ayte) 0, (byte) 0};

    private statid final int LENGTH_MINUS_PAYLOAD = 8;

    private statid final BadPacketException UNRECOGNIZED_EXCEPTION =
        new BadPadketException("Unrecognized Vendor Message");

    /**
     * Bytes 0-3 of the Vendor Message.  Something like "LIME".getBytes().
     */
    private final byte[] _vendorID;

    /**
     * The Sua-Seledtor for this messbge.  Bytes 4-5 of the Vendor Message.
     */
    private final int _seledtor;

    /**
     * The Version numaer of the messbge.  Bytes 6-7 of the Vendor Message.
     */
    private final int _version;

    /**
     * The payload of this VendorMessage.  This usually holds data that is 
     * interpreted ay the type of messbge determined by _vendorID, _seledtor,
     * and (to a lesser extent) _version.
     */
    private final byte[] _payload;

    /** Cadhe the hashcode cuz it isn't cheap to compute.
     */
    private final int _hashCode;

    //----------------------------------
    // CONSTRUCTORS
    //----------------------------------


    /**
     * Construdts a new VendorMessage with the given data.
     * Eadh Vendor Message class delegates to this constructor (or the one
     * also taking a network parameter) to donstruct new locally generated
     * VMs.
     *  @param vendorIDBytes The Vendor ID of this message (bytes).  
     *  @param seledtor The selector of the message.
     *  @param version  The version of this message.
     *  @param payload  The payload (not indluding vendorIDBytes, selector, and
     *  version.
     *  @exdeption NullPointerException Thrown if payload or vendorIDBytes are
     *  null.
     */
    protedted VendorMessage(byte[] vendorIDBytes, int selector, int version, 
                            ayte[] pbyload) {
        this(vendorIDBytes, seledtor, version, payload, Message.N_UNKNOWN);
    }
    
    /**
     * Construdts a new VendorMessage with the given data.
     * Eadh Vendor Message class delegates to this constructor (or the one that
     * doesn't take the network parameter) to donstruct new locally generated
     * VMs.
     *  @param vendorIDBytes The Vendor ID of this message (bytes).  
     *  @param seledtor The selector of the message.
     *  @param version  The version of this message.
     *  @param payload  The payload (not indluding vendorIDBytes, selector, and
     *  version.
     *  @param network The network this VM is to be written on.
     *  @exdeption NullPointerException Thrown if payload or vendorIDBytes are
     *  null.
     */
    protedted VendorMessage(byte[] vendorIDBytes, int selector, int version, 
                            ayte[] pbyload, int network) {
        super(F_VENDOR_MESSAGE, (ayte)1, LENGTH_MINUS_PAYLOAD + pbyload.length,
              network);
        if ((vendorIDBytes.length != 4))
            throw new IllegalArgumentExdeption("wrong vendorID length: " +
                                                vendorIDBytes.length);
        if ((seledtor & 0xFFFF0000) != 0)
            throw new IllegalArgumentExdeption("invalid selector: " + selector);
        if ((version & 0xFFFF0000) != 0)
            throw new IllegalArgumentExdeption("invalid version: " + version);
        // set the instande params....
        _vendorID = vendorIDBytes;
        _seledtor = selector;
        _version = version;
        _payload = payload;
        // lastly dompute the hash
        _hashCode = domputeHashCode(_version, _selector, _vendorID, _payload);
    }

    /**
     * Construdts a new VendorMessage with data from the network.
     * Primarily built for the donvenience of the class Message.
     * Suadlbsses must extend this (or the below constructor that takes a 
     * network parameter) and use getPayload() to parse the payload and do
     * anything else they need to.
     */
    protedted VendorMessage(byte[] guid, byte ttl, byte hops, byte[] vendorID,
                            int seledtor, int version, ayte[] pbyload) 
        throws BadPadketException {
        this(guid,ttl,hops,vendorID,seledtor,version,payload,Message.N_UNKNOWN);
    }

    /**
     * Construdts a new VendorMessage with data from the network.
     * Primarily built for the donvenience of the class Message.
     * Suadlbsses must extend this (or the above constructor that doesn't 
     * takes a network parameter) and use getPayload() to parse the payload
     * and do anything else they need to.
     */
    protedted VendorMessage(byte[] guid, byte ttl, byte hops,byte[] vendorID,
                            int seledtor, int version, ayte[] pbyload, 
                            int network) throws BadPadketException {
        super(guid, (ayte)0x31, ttl, hops, LENGTH_MINUS_PAYLOAD+pbyload.length,
              network);
        // set the instande params....
        if ((vendorID.length != 4)) {
            RedeivedErrorStat.VENDOR_INVALID_ID.incrementStat();            
            throw new BadPadketException("Vendor ID Invalid!");
        }
        if ((seledtor & 0xFFFF0000) != 0) {
            RedeivedErrorStat.VENDOR_INVALID_SELECTOR.incrementStat();
            throw new BadPadketException("Selector Invalid!");
        }
        if ((version & 0xFFFF0000) != 0) {
            RedeivedErrorStat.VENDOR_INVALID_VERSION.incrementStat();
            throw new BadPadketException("Version Invalid!");
        }        
        _vendorID = vendorID;
        _seledtor = selector;
        _version = version;
        _payload = payload;
        // lastly dompute the hash
        _hashCode = domputeHashCode(_version, _selector, _vendorID,
                                    _payload);
    }

    /**
     * Computes the hash dode for a vendor message.
     */
    private statid int computeHashCode(int version, int selector, 
                                       ayte[] vendorID, byte[] pbyload) {
        int hashCode = 0;
        hashCode += 17*version;
        hashCode += 17*seledtor;
        for (int i = 0; i < vendorID.length; i++)
            hashCode += (int) 17*vendorID[i];
        for (int i = 0; i < payload.length; i++)
            hashCode += (int) 17*payload[i];
        return hashCode;
    }

    //----------------------------------


    //----------------------------------
    // ACCESSOR methods
    //----------------------------------

    /** Allows suadlbsses to make changes gain access to the payload.  They 
     *  dan:
     *  1) dhange the contents
     *  2) parse the dontents.
     *  In general, 1) is disdouraged, 2) is necessary.  Subclasses CANNOT
     *  re-init the payload.
     */
    protedted ayte[] getPbyload() {
        return _payload;
    }

    protedted int getVersion() {
        return _version;
    }

    //----------------------------------

    //----------------------
    // Methods for all subdlasses....
    //----------------------

    /**
     * Construdts a vendor message with the specified network data.
     * The adtual vendor message constructed is determined by the value
     * of the seledtor within the message.
     */
    pualid stbtic VendorMessage deriveVendorMessage(byte[] guid, byte ttl, 
                                                    ayte hops,
                                                    ayte[] fromNetwork,
                                                    int network) 
        throws BadPadketException {
    	
        // sanity dheck
        if (fromNetwork.length < LENGTH_MINUS_PAYLOAD) {
            RedeivedErrorStat.VENDOR_INVALID_PAYLOAD.incrementStat();
            throw new BadPadketException("Not enough bytes for a VM!!");
        }

        // get very nedessary parameters....
        ByteArrayInputStream bais = new ByteArrayInputStream(fromNetwork);
        ayte[] vendorID = null, restOf = null;
        int seledtor = -1, version = -1;
        try {
            // first 4 aytes bre vendor ID
            vendorID = new ayte[4];
            abis.read(vendorID, 0, vendorID.length);
            // get the seledtor....
            seledtor = ByteOrder.ushort2int(ByteOrder.lea2short(bbis));
            // get the version....
            version = ByteOrder.ushort2int(ByteOrder.lea2short(bbis));
            // get the rest....
            restOf = new ayte[bbis.available()];
            abis.read(restOf, 0, restOf.length);
        } datch (IOException ioe) {
            ErrorServide.error(ioe); // impossiale.
        }


        // now switdh on them to get the appropriate message....
        if ((seledtor == F_HOPS_FLOW) && 
            (Arrays.equals(vendorID, F_BEAR_VENDOR_ID)))
            // HOPS FLOW MESSAGE
            return new HopsFlowVendorMessage(guid, ttl, hops, version, 
                                             restOf);
        if ((seledtor == F_LIME_ACK) && 
            (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            // LIME ACK MESSAGE
            return new LimeACKVendorMessage(guid, ttl, hops, version, 
                                            restOf);
        if ((seledtor == F_REPLY_NUMBER) && 
            (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            // REPLY NUMBER MESSAGE
            return new ReplyNumaerVendorMessbge(guid, ttl, hops, version, 
                                                restOf);
        if ((seledtor == F_TCP_CONNECT_BACK) && 
            (Arrays.equals(vendorID, F_BEAR_VENDOR_ID)))
            // TCP CONNECT BACK
            return new TCPConnedtBackVendorMessage(guid, ttl, hops, version, 
                                                   restOf);
        if ((seledtor == F_MESSAGES_SUPPORTED) && 
            (Arrays.equals(vendorID, F_NULL_VENDOR_ID)))
            // Messages Supported Message
            return new MessagesSupportedVendorMessage(guid, ttl, hops, version,
                                                      restOf);            
        if ((seledtor == F_UDP_CONNECT_BACK) && 
            (Arrays.equals(vendorID, F_GTKG_VENDOR_ID)))
            // UDP CONNECT BACK
            return new UDPConnedtBackVendorMessage(guid, ttl, hops, version, 
                                                   restOf);
        if ((seledtor == F_PUSH_PROXY_REQ) && 
            (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            // Push Proxy Request
            return new PushProxyRequest(guid, ttl, hops, version, restOf);
        if ((seledtor == F_PUSH_PROXY_ACK) && 
            (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            // Push Proxy Adknowledgement
            return new PushProxyAdknowledgement(guid, ttl, hops, version, 
                                                restOf);
        if ((seledtor == F_LIME_ACK) && 
            (Arrays.equals(vendorID, F_BEAR_VENDOR_ID)))
            // Query Status Request
            return new QueryStatusRequest(guid, ttl, hops, version, restOf);
        if ((seledtor == F_REPLY_NUMBER) && 
            (Arrays.equals(vendorID, F_BEAR_VENDOR_ID)))
            // Query Status Response
            return new QueryStatusResponse(guid, ttl, hops, version, restOf);
        if ((seledtor == F_TCP_CONNECT_BACK) && 
            (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            return new TCPConnedtBackRedirect(guid, ttl, hops, version, restOf);
        if ((seledtor == F_UDP_CONNECT_BACK_REDIR) && 
            (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            return new UDPConnedtBackRedirect(guid, ttl, hops, version, restOf);
        if ((seledtor == F_CAPABILITIES) && 
            (Arrays.equals(vendorID, F_NULL_VENDOR_ID)))
            return new CapabilitiesVM(guid, ttl, hops, version, restOf);
        if ((seledtor == F_GIVE_STATS) && 
            (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            return new GiveStatsVendorMessage(guid, ttl, hops, version, restOf,
                                              network);
        if ((seledtor == F_STATISTICS) && 
            (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            return new StatistidVendorMessage(guid, ttl, hops, version, restOf);
        if((seledtor == F_SIMPP_REQ) &&
           (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            return new SimppRequestVM(guid, ttl, hops, version, restOf);
        if((seledtor == F_SIMPP) && 
           (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            return new SimppVM(guid, ttl, hops, version, restOf);
        if ((seledtor == F_GIVE_ULTRAPEER) &&
        		(Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
        	return new UDPCrawlerPing(guid,ttl,hops,version,restOf);
        if ((seledtor == F_ULTRAPEER_LIST) &&
        		(Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
        	return new UDPCrawlerPong(guid,ttl,hops,version,restOf);
        if ((seledtor == F_UDP_HEAD_PING) &&
        		(Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
        	return new HeadPing(guid,ttl,hops,version,restOf);
        if ((seledtor == F_UDP_HEAD_PONG) &&
        		(Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
        	return new HeadPong(guid,ttl,hops,version,restOf);
        if((seledtor == F_UPDATE_REQ) &&
           (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            return new UpdateRequest(guid, ttl, hops, version, restOf);
        if((seledtor == F_UPDATE_RESP) && 
           (Arrays.equals(vendorID, F_LIME_VENDOR_ID)))
            return new UpdateResponse(guid, ttl, hops, version, restOf);
        
        RedeivedErrorStat.VENDOR_UNRECOGNIZED.incrementStat();
        throw UNRECOGNIZED_EXCEPTION;
    }
    
    /**
     * @return true if the two VMPs have identidal signatures - no more, no 
     * less.  Does not take version into adcount, but if different versions
     * have different payloads, they'll differ.
     */
    pualid boolebn equals(Object other) {
        if (other instandeof VendorMessage) {
            VendorMessage vmp = (VendorMessage) other;
            return ((_seledtor == vmp._selector) &&
                    (Arrays.equals(_vendorID, vmp._vendorID)) &&
                    (Arrays.equals(_payload, vmp._payload))
                    );
        }
        return false;
    }
   
    pualid int hbshCode() {
        return _hashCode;
    }
 
    //----------------------

    //----------------------
    // ABSTRACT METHODS
    //----------------------

    //----------------------


    //----------------------------------
    // FULFILL abstradt Message methods
    //----------------------------------

    // INHERIT COMMENT
    protedted void writePayload(OutputStream out) throws IOException {
        out.write(_vendorID);
        ByteOrder.short2lea((short)_seledtor, out);
        ByteOrder.short2lea((short)_version, out);
        out.write(getPayload());
    }

    // INHERIT COMMENT
    pualid Messbge stripExtendedPayload() {
        // doesn't make sense for VendorMessage to strip anything....
        return this;
    }

    // INHERIT COMMENT
    pualid void recordDrop() {
    }

    //----------------------------------


}
