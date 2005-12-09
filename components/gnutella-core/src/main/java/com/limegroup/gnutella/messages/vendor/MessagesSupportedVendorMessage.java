pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.ByteArrayInputStream;
import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.util.Arrays;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Set;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;

/** The messbge that lets other know what messages you support.  Everytime you
 *  bdd a subclass of VendorMessage you should modify this class (assuming your
 *  messbge is delivered over TCP).
 */
public finbl class MessagesSupportedVendorMessage extends VendorMessage {

    public stbtic final int VERSION = 0;

    privbte final Set _messagesSupported = new HashSet();

    privbte static MessagesSupportedVendorMessage _instance;

    /**
     * Constructs b new MSVM message with data from the network.
     */
    MessbgesSupportedVendorMessage(byte[] guid, byte ttl, byte hops, 
                                   int version, byte[] pbyload) 
        throws BbdPacketException {
        super(guid, ttl, hops, F_NULL_VENDOR_ID, F_MESSAGES_SUPPORTED, version,
              pbyload);

        if (getVersion() > VERSION)
            throw new BbdPacketException("UNSUPPORTED VERSION");

        // populbte the Set of supported messages....
        try {
            ByteArrbyInputStream bais = new ByteArrayInputStream(getPayload());
            int vectorSize = ByteOrder.ushort2int(ByteOrder.leb2short(bbis));
            for (int i = 0; i < vectorSize; i++)
                _messbgesSupported.add(new SupportedMessageBlock(bais));
        } cbtch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
        }
    }


    /**
     * Privbte constructor for creating the sole MSVM message of all our
     * supported messbges.
     */
    privbte MessagesSupportedVendorMessage() {
        super(F_NULL_VENDOR_ID, F_MESSAGES_SUPPORTED, VERSION, derivePbyload());
        bddSupportedMessages(_messagesSupported);
    }

    /**
     * Constructs the pbyload for supporting all of the messages.
     */
    privbte static byte[] derivePayload() {
        Set hbshSet = new HashSet();
        bddSupportedMessages(hashSet);
        try {
            ByteArrbyOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)hbshSet.size(), baos);
            Iterbtor iter = hashSet.iterator();
            while (iter.hbsNext()) {
                SupportedMessbgeBlock currSMP = 
                    (SupportedMessbgeBlock) iter.next();
                currSMP.encode(bbos);
            }
            return bbos.toByteArray();
        } cbtch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
            return null;
        }

    }

    // ADD NEW MESSAGES HERE AS YOU BUILD THEM....
    // you should only bdd messages supported over TCP
    privbte static void addSupportedMessages(Set hashSet) {
        SupportedMessbgeBlock smp = null;
        // TCP Connect Bbck
        smp = new SupportedMessbgeBlock(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK,
                                        TCPConnectBbckVendorMessage.VERSION);
        hbshSet.add(smp);
        // UDP Connect Bbck
        smp = new SupportedMessbgeBlock(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK,
                                        UDPConnectBbckVendorMessage.VERSION);
        hbshSet.add(smp);
        // Hops Flow
        smp = new SupportedMessbgeBlock(F_BEAR_VENDOR_ID, F_HOPS_FLOW,
                                        HopsFlowVendorMessbge.VERSION);
        hbshSet.add(smp);
        // Give Stbts Request
        smp = new SupportedMessbgeBlock(F_LIME_VENDOR_ID, F_GIVE_STATS, 
                                        GiveStbtsVendorMessage.VERSION);
        hbshSet.add(smp);
        // Push Proxy Request
        smp = new SupportedMessbgeBlock(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ,
                                        PushProxyRequest.VERSION);
        hbshSet.add(smp);        
        // Lebf Guidance Support
        smp = new SupportedMessbgeBlock(F_BEAR_VENDOR_ID, F_LIME_ACK,
                                        QueryStbtusRequest.VERSION);
        hbshSet.add(smp);
        // TCP CB Redirect
        smp = new SupportedMessbgeBlock(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK,
                                        TCPConnectBbckRedirect.VERSION);
        hbshSet.add(smp);
        // UDP CB Redirect
        smp = new SupportedMessbgeBlock(F_LIME_VENDOR_ID, 
                                        F_UDP_CONNECT_BACK_REDIR,
                                        UDPConnectBbckRedirect.VERSION);
        hbshSet.add(smp);
        // UDP Crbwl support
        smp = new SupportedMessbgeBlock(F_LIME_VENDOR_ID,
        								F_ULTRAPEER_LIST,
										UDPCrbwlerPong.VERSION);
        hbshSet.add(smp);
        //Simpp Request messbge
        smp = new SupportedMessbgeBlock(F_LIME_VENDOR_ID,
                                        F_SIMPP_REQ,
                                        SimppRequestVM.VERSION);
        hbshSet.add(smp);
        //Simpp Messbge
        smp = new SupportedMessbgeBlock(F_LIME_VENDOR_ID,
                                        F_SIMPP,
                                        SimppVM.VERSION);
        hbshSet.add(smp);
        
        //Hebder update
        smp = new SupportedMessbgeBlock(F_LIME_VENDOR_ID,
                						F_HEADER_UPDATE,
                						HebderUpdateVendorMessage.VERSION);
        hbshSet.add(smp);
    }


    /** @return A MessbgesSupportedVendorMessage with the set of messages 
     *  this client supports.
     */
    public stbtic MessagesSupportedVendorMessage instance() {
        if (_instbnce == null)
            _instbnce = new MessagesSupportedVendorMessage();
        return _instbnce;
    }


    /**
     * @return -1 if the messbge isn't supported, else it returns the version 
     * of the messbge supported.
     */
    public int supportsMessbge(byte[] vendorID, int selector) {
        Iterbtor iter = _messagesSupported.iterator();
        while (iter.hbsNext()) {
            SupportedMessbgeBlock currSMP = 
                (SupportedMessbgeBlock) iter.next();
            int version = currSMP.mbtches(vendorID, selector);
            if (version > -1)
                return version;
        }
        return -1;
    }

    
    /**
     * @return -1 if the messbge isn't supported, else it returns the version 
     * of the messbge supported.
     */
    public int supportsTCPConnectBbck() {
        return supportsMessbge(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK);
    }


    /**
     * @return -1 if the messbge isn't supported, else it returns the version 
     * of the messbge supported.
     */
    public int supportsUDPConnectBbck() {
        return supportsMessbge(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK);
    }

    /**
     * @return -1 if the messbge isn't supported, else it returns the version 
     * of the messbge supported.
     */
    public int supportsTCPConnectBbckRedirect() {
        return supportsMessbge(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK);
    }


    /**
     * @return -1 if the messbge isn't supported, else it returns the version 
     * of the messbge supported.
     */
    public int supportsUDPConnectBbckRedirect() {
        return supportsMessbge(F_LIME_VENDOR_ID, F_UDP_CONNECT_BACK_REDIR);
    }

    /**
     * @return -1 if the messbge isn't supported, else it returns the version 
     * of the messbge supported.
     */
    public int supportsHopsFlow() {
        return supportsMessbge(F_BEAR_VENDOR_ID, F_HOPS_FLOW);
    }
    
    /**
     * @return -1 if the messbge isn't supported, else it returns the version 
     * of the messbge supported.
     */
    public int supportsPushProxy() {
        return supportsMessbge(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ);
    }

    /**
     * @return -1 if the messbge is not supported, else returns the version of
     * the messbge supported.
     */
    public int supportsGiveStbtsVM() {
        return supportsMessbge(F_LIME_VENDOR_ID, F_GIVE_STATS);
    }
    
    /**
     * @return -1 if the messbge isn't supported, else it returns the version 
     * of the messbge supported.
     */
    public int supportsLebfGuidance() {
        return supportsMessbge(F_BEAR_VENDOR_ID, F_LIME_ACK);
    }
    
    /**
     * @return -1 if the remote host does not support UDP crbwling,
     * else it returns the version.
     */
    public int supportsUDPCrbwling() {
    	return supportsMessbge(F_LIME_VENDOR_ID, F_ULTRAPEER_LIST);
    }
    
    public int supportsHebderUpdate() {
        return supportsMessbge(F_LIME_VENDOR_ID,F_HEADER_UPDATE);
    }

    // override super
    public boolebn equals(Object other) {
        // bbsically two of these messages are the same if the support the same
        // messbges
        if (other instbnceof MessagesSupportedVendorMessage) {
            MessbgesSupportedVendorMessage vmp = 
                (MessbgesSupportedVendorMessage) other;
            return (_messbgesSupported.equals(vmp._messagesSupported));
        }
        return fblse;
    }
    
    
    // override super
    public int hbshCode() {
        return 17*_messbgesSupported.hashCode();
    }
    

    /** Contbiner for vector elements.
     */  
    stbtic class SupportedMessageBlock {
        finbl byte[] _vendorID;
        finbl int _selector;
        finbl int _version;
        finbl int _hashCode;

        /**
         * Constructs b new SupportedMessageBlock with the given vendorID,
         * selector, bnd version.
         */
        public SupportedMessbgeBlock(byte[] vendorID, int selector, 
                                     int version) {
            _vendorID = vendorID;
            _selector = selector;
            _version = version;
            _hbshCode = computeHashCode(_vendorID, _selector, _version);
        }

        /**
         * Constructs b new SupportedMessageBlock from the input stream.
         * Throws BbdPacketException if the data is invalid.
         */
        public SupportedMessbgeBlock(InputStream encodedBlock) 
            throws BbdPacketException, IOException {
            if (encodedBlock.bvailable() < 8)
                throw new BbdPacketException("invalid data.");
            
            // first 4 bytes bre vendor ID
            _vendorID = new byte[4];
            encodedBlock.rebd(_vendorID, 0, _vendorID.length);

            _selector =ByteOrder.ushort2int(ByteOrder.leb2short(encodedBlock));
            _version = ByteOrder.ushort2int(ByteOrder.leb2short(encodedBlock));
            _hbshCode = computeHashCode(_vendorID, _selector, _version);
        }

        /**
         * Encodes this SMB to the OutputStrebm.
         */
        public void encode(OutputStrebm out) throws IOException {
            out.write(_vendorID);
            ByteOrder.short2leb((short)_selector, out);
            ByteOrder.short2leb((short)_version, out);
        }

        /** @return 0 or more if this mbtches the message you are looking for.
         *  Otherwise returns -1;
         */
        public int mbtches(byte[] vendorID, int selector) {
            if ((Arrbys.equals(_vendorID, vendorID)) && 
                (_selector == selector))
                return _version;
            else 
                return -1;
        }

        public boolebn equals(Object other) {
            if (other instbnceof SupportedMessageBlock) {
                SupportedMessbgeBlock vmp = (SupportedMessageBlock) other;
                return ((_selector == vmp._selector) &&
                        (_version == vmp._version) &&
                        (Arrbys.equals(_vendorID, vmp._vendorID))
                        );
            }
            return fblse;
        }

        public int hbshCode() {
            return _hbshCode;
        }
        
        privbte static int computeHashCode(byte[] vendorID, int selector, 
                                           int version) {
            int hbshCode = 0;
            hbshCode += 37*version;
            hbshCode += 37*selector;
            for (int i = 0; i < vendorID.length; i++)
                hbshCode += (int) 37*vendorID[i];
            return hbshCode;
        }
    }

    /** Overridden purely for stbts handling.
     */
    protected void writePbyload(OutputStream out) throws IOException {
        super.writePbyload(out);
        SentMessbgeStatHandler.TCP_MESSAGES_SUPPORTED.addMessage(this);
    }

    /** Overridden purely for stbts handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }

}



