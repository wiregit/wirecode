package com.limegroup.gnutella.messages.vendor;

import java.io.*;
import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.statistics.*;
import com.sun.java.util.collections.*;

/** The message that lets other know what capabilities you support.  Everytime 
 *  you add a capability you should modify this class.
 */
public final class CapabilitiesVM extends VendorMessage {

    /** Bytes for 'WHAT'.
     */
    private static final byte[] WHAT_IS_CAPABILITY = {(byte)87, (byte)72,
                                                      (byte)65, (byte)84};
    private static final int WHAT_IS_CAPABILITY_VERSION = 1;

    public static final int VERSION = 0;

    private final Set _capabilitiesSupported = new HashSet();

    private static CapabilitiesVM _instance;

    CapabilitiesVM(byte[] guid, byte ttl, byte hops, 
                   int version, byte[] payload) 
        throws BadPacketException {
        super(guid, ttl, hops, F_NULL_VENDOR_ID, F_CAPABILITIES, version,
              payload);

        // populate the Set of supported messages....
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(getPayload());
            int vectorSize = ByteOrder.ubytes2int(ByteOrder.leb2short(bais));
            for (int i = 0; i < vectorSize; i++)
                _capabilitiesSupported.add(new SupportedMessageBlock(bais));
        }
        catch (IOException ioe) {
            throw new BadPacketException("Couldn't write to a ByteStream!!!");
        }
    }


    /** Private constructor that fills up our capabilities.
     */
    private CapabilitiesVM() throws BadPacketException {
        super(F_NULL_VENDOR_ID, F_CAPABILITIES, VERSION, derivePayload());
        addSupportedMessages(_capabilitiesSupported);
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

    // ADD NEW CAPABILITIES HERE AS YOU BUILD THEM....
    private static void addSupportedMessages(Set hashSet) {
        SupportedMessageBlock smp = null;
        smp = new SupportedMessageBlock(WHAT_IS_CAPABILITY, 
                                        WHAT_IS_CAPABILITY_VERSION);
        hashSet.add(smp);
    }


    /** @return A CapabilitiesVM with the set of messages 
     *  this client supports.
     */
    public static CapabilitiesVM instance() 
        throws BadPacketException {
        if (_instance == null)
            _instance = new CapabilitiesVM();
        return _instance;
    }


    /**
     * @return -1 if the ability isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsCapability(byte[] capabilityName) {
        Iterator iter = _capabilitiesSupported.iterator();
        while (iter.hasNext()) {
            SupportedMessageBlock currSMP = 
                (SupportedMessageBlock) iter.next();
            int version = currSMP.matches(capabilityName);
            if (version > -1)
                return version;
        }
        return -1;
    }


    /** @return 1 or higher if the what is capability is supported.  the version
     *  number gives some indication about what exactly is a supported.  if no
     *  support, returns -1.
     */
    public int supportsWhatIsCapability() {
        return supportsCapability(WHAT_IS_CAPABILITY);
    }
    

    /** @return true if 'what is new' feature is supported.
     */
    public boolean supportsWhatIsNew() {
        return supportsWhatIsCapability() > 0;
    }

    // override super
    public boolean equals(Object other) {
        // basically two of these messages are the same if the support the same
        // messages
        if (other instanceof CapabilitiesVM) {
            CapabilitiesVM vmp = 
                (CapabilitiesVM) other;
            return (_capabilitiesSupported.equals(vmp._capabilitiesSupported));
        }
        return false;
    }
    
    
    // override super
    public int hashCode() {
        return 17*_capabilitiesSupported.hashCode();
    }
    

    /** Container for vector elements.
     */  
    static class SupportedMessageBlock {
        final byte[] _capabilityName;
        final int _version;
        final int _hashCode;

        public SupportedMessageBlock(byte[] capabilityName, int version) {
            _capabilityName = capabilityName;
            _version = version;
            _hashCode = computeHashCode(_capabilityName, _version);
        }

        public SupportedMessageBlock(InputStream encodedBlock) 
            throws IOException {
            if (encodedBlock.available() < 6)
                throw new IOException();
            
            // first 4 bytes are capability name
            _capabilityName = new byte[4];
            encodedBlock.read(_capabilityName, 0, _capabilityName.length);

            _version = ByteOrder.ubytes2int(ByteOrder.leb2short(encodedBlock));
            _hashCode = computeHashCode(_capabilityName, _version);
        }

        public byte[] encode() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(_capabilityName);
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
        public int matches(byte[] capabilityName) {
            if (Arrays.equals(_capabilityName, capabilityName))
                return _version;
            else 
                return -1;
        }

        public boolean equals(Object other) {
            if (other instanceof SupportedMessageBlock) {
                SupportedMessageBlock vmp = (SupportedMessageBlock) other;
                return ((_version == vmp._version) &&
                        (Arrays.equals(_capabilityName, vmp._capabilityName))
                        );
            }
            return false;
        }

        public int hashCode() {
            return _hashCode;
        }
        
        private static int computeHashCode(byte[] capabilityName, int version) {
            int hashCode = 0;
            hashCode += 37*version;
            for (int i = 0; i < capabilityName.length; i++)
                hashCode += (int) 37*capabilityName[i];
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



