package com.limegroup.gnutella.messages;

import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.sun.java.util.collections.*;

/** The message that lets other know what messages you support.  Everytime you
 *  add a subclass of VendorMessagePayload you should modify this class.
 */
public final class MessagesSupportedVMP extends VendorMessagePayload {

    public static final int VERSION = 0;

    private byte[] _payload = null;
    private Set _messagesSupported = new HashSet();

    private static MessagesSupportedVMP _instance = new MessagesSupportedVMP();

    MessagesSupportedVMP(int version, byte[] payload) 
        throws BadPacketException {
        super(F_NULL_VENDOR_ID, F_MESSAGES_SUPPORTED, version);
        if (version > VERSION)
            throw new BadPacketException("UNSUPPORTED VERSION");
        // get the port from the payload....
        _payload = payload;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(_payload);
            int vectorSize = ByteOrder.ubytes2int(ByteOrder.leb2short(bais));
            for (int i = 0; i < vectorSize; i++)
                _messagesSupported.add(new SupportedMessageBlock(bais));
        }
        catch (IOException ioe) {
            throw new BadPacketException();
        }
    }

    /** @param port The port you want people to connect back to.  If you give a
     *  bad port I don't check so check yourself!
     */
    private MessagesSupportedVMP() {
        super(F_NULL_VENDOR_ID, F_MESSAGES_SUPPORTED, VERSION);
        addSupportedMessages();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)_messagesSupported.size(), baos);
            Iterator iter = _messagesSupported.iterator();
            while (iter.hasNext()) {
                SupportedMessageBlock currSMP = 
                    (SupportedMessageBlock) iter.next();
                baos.write(currSMP.encode());
            }
            _payload = baos.toByteArray();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    // ADD NEW MESSAGES HERE AS YOU BUILD THEM....
    private void addSupportedMessages() {
        // TCP Connect Back
        SupportedMessageBlock smp = null;
        smp = new SupportedMessageBlock(F_BEAR_VENDOR_ID, F_TCP_CONNECT_BACK,
                                        TCPConnectBackVMP.VERSION);
        _messagesSupported.add(smp);
        // UDP Connect Back
        smp = new SupportedMessageBlock(F_GTKG_VENDOR_ID, F_UDP_CONNECT_BACK,
                                        UDPConnectBackVMP.VERSION);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        _messagesSupported.add(smp);
    }

    protected byte[] getPayload() {
        // construction makes _payload
        return _payload;
    }

    /** @return A MessagesSupportedVMP with the set of messages this client
     *  supports.
     */
    public static MessagesSupportedVMP instance() {
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

    // override super
    public boolean equals(Object other) {
        // basically two of these messages are the same if the support the same
        // messages
        if (other instanceof MessagesSupportedVMP) {
            MessagesSupportedVMP vmp = (MessagesSupportedVMP) other;
            return (_messagesSupported.equals(vmp._messagesSupported));
        }
        return false;
    }
    
    
    // override super
    public int hashCode() {
        return _messagesSupported.hashCode();
    }
    

    /** Container for vector elements.
     */  
    static class SupportedMessageBlock {
        byte[] _vendorID = null;
        int _selector = 0;
        int _version = 0;

        public SupportedMessageBlock(byte[] vendorID, int selector, 
                                     int version) {
            _vendorID = vendorID;
            _selector = selector;
            _version = version;
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
        }

        public byte[] encode() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(_vendorID);
                ByteOrder.short2leb((short)_selector, baos);
                ByteOrder.short2leb((short)_version, baos);
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
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
            int hashCode = 0;
            hashCode += 37*_version;
            hashCode += 37*_selector;
            for (int i = 0; i < _vendorID.length; i++)
                hashCode += (int) 37*_vendorID[i];
            return hashCode;
        }
    }

}



