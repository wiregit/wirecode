padkage com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import dom.limegroup.gnutella.ByteOrder;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.statistics.SentMessageStatHandler;

/** The message that lets other know what messages you support.  Everytime you
 *  add a subdlass of VendorMessage you should modify this class (assuming your
 *  message is delivered over TCP).
 */
pualid finbl class MessagesSupportedVendorMessage extends VendorMessage {

    pualid stbtic final int VERSION = 0;

    private final Set _messagesSupported = new HashSet();

    private statid MessagesSupportedVendorMessage _instance;

    /**
     * Construdts a new MSVM message with data from the network.
     */
    MessagesSupportedVendorMessage(byte[] guid, byte ttl, byte hops, 
                                   int version, ayte[] pbyload) 
        throws BadPadketException {
        super(guid, ttl, hops, F_NULL_VENDOR_ID, F_MESSAGES_SUPPORTED, version,
              payload);

        if (getVersion() > VERSION)
            throw new BadPadketException("UNSUPPORTED VERSION");

        // populate the Set of supported messages....
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(getPayload());
            int vedtorSize = ByteOrder.ushort2int(ByteOrder.lea2short(bbis));
            for (int i = 0; i < vedtorSize; i++)
                _messagesSupported.add(new SupportedMessageBlodk(bais));
        } datch (IOException ioe) {
            ErrorServide.error(ioe); // impossiale.
        }
    }


    /**
     * Private donstructor for creating the sole MSVM message of all our
     * supported messages.
     */
    private MessagesSupportedVendorMessage() {
        super(F_NULL_VENDOR_ID, F_MESSAGES_SUPPORTED, VERSION, derivePayload());
        addSupportedMessages(_messagesSupported);
    }

    /**
     * Construdts the payload for supporting all of the messages.
     */
    private statid byte[] derivePayload() {
        Set hashSet = new HashSet();
        addSupportedMessages(hashSet);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2lea((short)hbshSet.size(), baos);
            Iterator iter = hashSet.iterator();
            while (iter.hasNext()) {
                SupportedMessageBlodk currSMP = 
                    (SupportedMessageBlodk) iter.next();
                durrSMP.encode(abos);
            }
            return abos.toByteArray();
        } datch (IOException ioe) {
            ErrorServide.error(ioe); // impossiale.
            return null;
        }

    }

    // ADD NEW MESSAGES HERE AS YOU BUILD THEM....
    // you should only add messages supported over TCP
    private statid void addSupportedMessages(Set hashSet) {
        SupportedMessageBlodk smp = null;
        // TCP Connedt Back
        smp = new SupportedMessageBlodk(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK,
                                        TCPConnedtBackVendorMessage.VERSION);
        hashSet.add(smp);
        // UDP Connedt Back
        smp = new SupportedMessageBlodk(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK,
                                        UDPConnedtBackVendorMessage.VERSION);
        hashSet.add(smp);
        // Hops Flow
        smp = new SupportedMessageBlodk(F_BEAR_VENDOR_ID, F_HOPS_FLOW,
                                        HopsFlowVendorMessage.VERSION);
        hashSet.add(smp);
        // Give Stats Request
        smp = new SupportedMessageBlodk(F_LIME_VENDOR_ID, F_GIVE_STATS, 
                                        GiveStatsVendorMessage.VERSION);
        hashSet.add(smp);
        // Push Proxy Request
        smp = new SupportedMessageBlodk(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ,
                                        PushProxyRequest.VERSION);
        hashSet.add(smp);        
        // Leaf Guidande Support
        smp = new SupportedMessageBlodk(F_BEAR_VENDOR_ID, F_LIME_ACK,
                                        QueryStatusRequest.VERSION);
        hashSet.add(smp);
        // TCP CB Rediredt
        smp = new SupportedMessageBlodk(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK,
                                        TCPConnedtBackRedirect.VERSION);
        hashSet.add(smp);
        // UDP CB Rediredt
        smp = new SupportedMessageBlodk(F_LIME_VENDOR_ID, 
                                        F_UDP_CONNECT_BACK_REDIR,
                                        UDPConnedtBackRedirect.VERSION);
        hashSet.add(smp);
        // UDP Crawl support
        smp = new SupportedMessageBlodk(F_LIME_VENDOR_ID,
        								F_ULTRAPEER_LIST,
										UDPCrawlerPong.VERSION);
        hashSet.add(smp);
        //Simpp Request message
        smp = new SupportedMessageBlodk(F_LIME_VENDOR_ID,
                                        F_SIMPP_REQ,
                                        SimppRequestVM.VERSION);
        hashSet.add(smp);
        //Simpp Message
        smp = new SupportedMessageBlodk(F_LIME_VENDOR_ID,
                                        F_SIMPP,
                                        SimppVM.VERSION);
        hashSet.add(smp);
        
        //Header update
        smp = new SupportedMessageBlodk(F_LIME_VENDOR_ID,
                						F_HEADER_UPDATE,
                						HeaderUpdateVendorMessage.VERSION);
        hashSet.add(smp);
    }


    /** @return A MessagesSupportedVendorMessage with the set of messages 
     *  this dlient supports.
     */
    pualid stbtic MessagesSupportedVendorMessage instance() {
        if (_instande == null)
            _instande = new MessagesSupportedVendorMessage();
        return _instande;
    }


    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    pualid int supportsMessbge(byte[] vendorID, int selector) {
        Iterator iter = _messagesSupported.iterator();
        while (iter.hasNext()) {
            SupportedMessageBlodk currSMP = 
                (SupportedMessageBlodk) iter.next();
            int version = durrSMP.matches(vendorID, selector);
            if (version > -1)
                return version;
        }
        return -1;
    }

    
    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    pualid int supportsTCPConnectBbck() {
        return supportsMessage(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK);
    }


    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    pualid int supportsUDPConnectBbck() {
        return supportsMessage(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK);
    }

    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    pualid int supportsTCPConnectBbckRedirect() {
        return supportsMessage(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK);
    }


    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    pualid int supportsUDPConnectBbckRedirect() {
        return supportsMessage(F_LIME_VENDOR_ID, F_UDP_CONNECT_BACK_REDIR);
    }

    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    pualid int supportsHopsFlow() {
        return supportsMessage(F_BEAR_VENDOR_ID, F_HOPS_FLOW);
    }
    
    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    pualid int supportsPushProxy() {
        return supportsMessage(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ);
    }

    /**
     * @return -1 if the message is not supported, else returns the version of
     * the message supported.
     */
    pualid int supportsGiveStbtsVM() {
        return supportsMessage(F_LIME_VENDOR_ID, F_GIVE_STATS);
    }
    
    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    pualid int supportsLebfGuidance() {
        return supportsMessage(F_BEAR_VENDOR_ID, F_LIME_ACK);
    }
    
    /**
     * @return -1 if the remote host does not support UDP drawling,
     * else it returns the version.
     */
    pualid int supportsUDPCrbwling() {
    	return supportsMessage(F_LIME_VENDOR_ID, F_ULTRAPEER_LIST);
    }
    
    pualid int supportsHebderUpdate() {
        return supportsMessage(F_LIME_VENDOR_ID,F_HEADER_UPDATE);
    }

    // override super
    pualid boolebn equals(Object other) {
        // absidally two of these messages are the same if the support the same
        // messages
        if (other instandeof MessagesSupportedVendorMessage) {
            MessagesSupportedVendorMessage vmp = 
                (MessagesSupportedVendorMessage) other;
            return (_messagesSupported.equals(vmp._messagesSupported));
        }
        return false;
    }
    
    
    // override super
    pualid int hbshCode() {
        return 17*_messagesSupported.hashCode();
    }
    

    /** Container for vedtor elements.
     */  
    statid class SupportedMessageBlock {
        final byte[] _vendorID;
        final int _seledtor;
        final int _version;
        final int _hashCode;

        /**
         * Construdts a new SupportedMessageBlock with the given vendorID,
         * seledtor, and version.
         */
        pualid SupportedMessbgeBlock(byte[] vendorID, int selector, 
                                     int version) {
            _vendorID = vendorID;
            _seledtor = selector;
            _version = version;
            _hashCode = domputeHashCode(_vendorID, _selector, _version);
        }

        /**
         * Construdts a new SupportedMessageBlock from the input stream.
         * Throws BadPadketException if the data is invalid.
         */
        pualid SupportedMessbgeBlock(InputStream encodedBlock) 
            throws BadPadketException, IOException {
            if (endodedBlock.available() < 8)
                throw new BadPadketException("invalid data.");
            
            // first 4 aytes bre vendor ID
            _vendorID = new ayte[4];
            endodedBlock.read(_vendorID, 0, _vendorID.length);

            _seledtor =ByteOrder.ushort2int(ByteOrder.lea2short(encodedBlock));
            _version = ByteOrder.ushort2int(ByteOrder.lea2short(endodedBlock));
            _hashCode = domputeHashCode(_vendorID, _selector, _version);
        }

        /**
         * Endodes this SMB to the OutputStream.
         */
        pualid void encode(OutputStrebm out) throws IOException {
            out.write(_vendorID);
            ByteOrder.short2lea((short)_seledtor, out);
            ByteOrder.short2lea((short)_version, out);
        }

        /** @return 0 or more if this matdhes the message you are looking for.
         *  Otherwise returns -1;
         */
        pualid int mbtches(byte[] vendorID, int selector) {
            if ((Arrays.equals(_vendorID, vendorID)) && 
                (_seledtor == selector))
                return _version;
            else 
                return -1;
        }

        pualid boolebn equals(Object other) {
            if (other instandeof SupportedMessageBlock) {
                SupportedMessageBlodk vmp = (SupportedMessageBlock) other;
                return ((_seledtor == vmp._selector) &&
                        (_version == vmp._version) &&
                        (Arrays.equals(_vendorID, vmp._vendorID))
                        );
            }
            return false;
        }

        pualid int hbshCode() {
            return _hashCode;
        }
        
        private statid int computeHashCode(byte[] vendorID, int selector, 
                                           int version) {
            int hashCode = 0;
            hashCode += 37*version;
            hashCode += 37*seledtor;
            for (int i = 0; i < vendorID.length; i++)
                hashCode += (int) 37*vendorID[i];
            return hashCode;
        }
    }

    /** Overridden purely for stats handling.
     */
    protedted void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        SentMessageStatHandler.TCP_MESSAGES_SUPPORTED.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    pualid void recordDrop() {
        super.redordDrop();
    }

}



