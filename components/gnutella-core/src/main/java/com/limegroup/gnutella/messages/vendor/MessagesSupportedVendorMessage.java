package com.limegroup.gnutella.messages.vendor;

import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.ErrorService;

/** The message that lets other know what messages you support.  Everytime you
 *  add a subclass of VendorMessage you should modify this class (assuming your
 *  message is delivered over TCP).
 */
public final class MessagesSupportedVendorMessage extends VendorMessage {

    public static final int VERSION = 0;

    private final Set _messagesSupported = new HashSet();

    private static MessagesSupportedVendorMessage _instance;

    MessagesSupportedVendorMessage(byte[] guid, byte ttl, byte hops, 
                                   int version, byte[] payload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_NULL_VENDOR_ID, F_MESSAGES_SUPPORTED, version,
              payload);

        if (getVersion() > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");

        // populate the Set of supported messages....
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(getPayload());
            int vectorSize = ByteOrder.ubytes2int(ByteOrder.leb2short(bais));
            for (int i = 0; i < vectorSize; i++)
                _messagesSupported.add(new SupportedMessageBlock(bais));
        }
        catch (IOException ioe) {
            throw new BadPacketException("Couldn't write to a ByteStream!!!");
        }
    }


    /** @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     */
    private MessagesSupportedVendorMessage() throws BadPacketException {
        super(F_NULL_VENDOR_ID, F_MESSAGES_SUPPORTED, VERSION, derivePayload());
        addSupportedMessages(_messagesSupported);
    }

    private static byte[] derivePayload() throws BadPacketException {
        Set hashSet = new HashSet();
        addSupportedMessages(hashSet);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)hashSet.size(), baos);
            Iterator iter = hashSet.iterator();
            while (iter.hasNext()) {
                SupportedMessageBlock currSMP = 
                    (SupportedMessageBlock) iter.next();
                baos.write(currSMP.encode());
            }
            return baos.toByteArray();
        }
        catch (IOException ioe) {
            throw new BadPacketException("Couldn't write to a ByteStream!!!");
        }

    }

    // ADD NEW MESSAGES HERE AS YOU BUILD THEM....
    // you should only add messages supported over TCP
    private static void addSupportedMessages(Set hashSet) {
        SupportedMessageBlock smp = null;
        // TCP Connect Back
        smp = new SupportedMessageBlock(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK,
                                        TCPConnectBackVendorMessage.VERSION);
        hashSet.add(smp);
        // UDP Connect Back
        smp = new SupportedMessageBlock(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK,
                                        UDPConnectBackVendorMessage.VERSION);
        hashSet.add(smp);
        // Hops Flow
        smp = new SupportedMessageBlock(F_BEAR_VENDOR_ID, F_HOPS_FLOW,
                                        HopsFlowVendorMessage.VERSION);
        hashSet.add(smp);
        // Push Proxy Request
        smp = new SupportedMessageBlock(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ,
                                        PushProxyRequest.VERSION);
        hashSet.add(smp);        
        // Leaf Guidance Support
        smp = new SupportedMessageBlock(F_BEAR_VENDOR_ID, F_LIME_ACK,
                                        QueryStatusRequest.VERSION);
        hashSet.add(smp);
        // TCP CB Redirect
        smp = new SupportedMessageBlock(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK,
                                        TCPConnectBackRedirect.VERSION);
        hashSet.add(smp);
        // UDP CB Redirect
        smp = new SupportedMessageBlock(F_LIME_VENDOR_ID, 
                                        F_UDP_CONNECT_BACK_REDIR,
                                        UDPConnectBackRedirect.VERSION);
        hashSet.add(smp);
    }


    /** @return A MessagesSupportedVendorMessage with the set of messages 
     *  this client supports.
     */
    public static MessagesSupportedVendorMessage instance() 
        throws BadPacketException {
        if (_instance == null)
            _instance = new MessagesSupportedVendorMessage();
        return _instance;
    }


    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsMessage(byte[] vendorID, int selector) {
        Iterator iter = _messagesSupported.iterator();
        while (iter.hasNext()) {
            SupportedMessageBlock currSMP = 
                (SupportedMessageBlock) iter.next();
            int version = currSMP.matches(vendorID, selector);
            if (version > -1)
                return version;
        }
        return -1;
    }

    
    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsTCPConnectBack() {
        return supportsMessage(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK);
    }


    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsUDPConnectBack() {
        return supportsMessage(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK);
    }

    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsTCPConnectBackRedirect() {
        return supportsMessage(F_LIME_VENDOR_ID, F_TCP_CONNECT_BACK);
    }


    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsUDPConnectBackRedirect() {
        return supportsMessage(F_LIME_VENDOR_ID, F_UDP_CONNECT_BACK_REDIR);
    }

    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsHopsFlow() {
        return supportsMessage(F_BEAR_VENDOR_ID, F_HOPS_FLOW);
    }

    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsPushProxy() {
        return supportsMessage(F_LIME_VENDOR_ID, F_PUSH_PROXY_REQ);
    }

    /**
     * @return -1 if the message isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsLeafGuidance() {
        return supportsMessage(F_BEAR_VENDOR_ID, F_LIME_ACK);
    }

    // override super
    public boolean equals(Object other) {
        // basically two of these messages are the same if the support the same
        // messages
        if (other instanceof MessagesSupportedVendorMessage) {
            MessagesSupportedVendorMessage vmp = 
                (MessagesSupportedVendorMessage) other;
            return (_messagesSupported.equals(vmp._messagesSupported));
        }
        return false;
    }
    
    
    // override super
    public int hashCode() {
        return 17*_messagesSupported.hashCode();
    }
    

    /** Container for vector elements.
     */  
    static class SupportedMessageBlock {
        final byte[] _vendorID;
        final int _selector;
        final int _version;
        final int _hashCode;

        public SupportedMessageBlock(byte[] vendorID, int selector, 
                                     int version) {
            _vendorID = vendorID;
            _selector = selector;
            _version = version;
            _hashCode = computeHashCode(_vendorID, _selector, _version);
        }

        public SupportedMessageBlock(InputStream encodedBlock) 
            throws IOException {
            if (encodedBlock.available() < 8)
                throw new IOException();
            
            // first 4 bytes are vendor ID
            _vendorID = new byte[4];
            encodedBlock.read(_vendorID, 0, _vendorID.length);

            _selector =ByteOrder.ubytes2int(ByteOrder.leb2short(encodedBlock));
            _version = ByteOrder.ubytes2int(ByteOrder.leb2short(encodedBlock));
            _hashCode = computeHashCode(_vendorID, _selector, _version);
        }

        public byte[] encode() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(_vendorID);
                ByteOrder.short2leb((short)_selector, baos);
                ByteOrder.short2leb((short)_version, baos);
            }
            catch (IOException ioe) {
                ErrorService.error(ioe);
            }
            return baos.toByteArray();
        }

        /** @return 0 or more if this matches the message you are looking for.
         *  Otherwise returns -1;
         */
        public int matches(byte[] vendorID, int selector) {
            if ((Arrays.equals(_vendorID, vendorID)) && 
                (_selector == selector))
                return _version;
            else 
                return -1;
        }

        public boolean equals(Object other) {
            if (other instanceof SupportedMessageBlock) {
                SupportedMessageBlock vmp = (SupportedMessageBlock) other;
                return ((_selector == vmp._selector) &&
                        (_version == vmp._version) &&
                        (Arrays.equals(_vendorID, vmp._vendorID))
                        );
            }
            return false;
        }

        public int hashCode() {
            return _hashCode;
        }
        
        private static int computeHashCode(byte[] vendorID, int selector, 
                                           int version) {
            int hashCode = 0;
            hashCode += 37*version;
            hashCode += 37*selector;
            for (int i = 0; i < vendorID.length; i++)
                hashCode += (int) 37*vendorID[i];
            return hashCode;
        }
    }

    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        if (RECORD_STATS)
            SentMessageStatHandler.TCP_MESSAGES_SUPPORTED.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }

}



