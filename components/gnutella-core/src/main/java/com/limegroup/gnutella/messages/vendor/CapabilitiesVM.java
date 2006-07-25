package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.version.UpdateHandler;

/** 
 * The message that lets other know what capabilities you support.  Everytime 
 * you add a capability you should modify this class.
 *
 */
public final class CapabilitiesVM extends VendorMessage {

    /**
     * Bytes for advertising that we support a 'feature' search.
     * The value is 'WHAT' for legacy reasons, because 'what is new' 
     * was the first feature search supported.
     */
    static final byte[] FEATURE_SEARCH_BYTES = {(byte)87, (byte)72,
                                                      (byte)65, (byte)84};
    /**
     * The bytes for supporting SIMPP.  This used to be 'SIMP', but that
     * implementation was broken.  We now use 'IMPP' to advertise support.
     */
    private static final byte[] SIMPP_CAPABILITY_BYTES = {'I', 'M', 'P', 'P' };
    
    /**
     * The bytes for the LMUP message.
     */
    private static final byte[] LIME_UPDATE_BYTES = { 'L', 'M', 'U', 'P' };
    
    /**
     * The bytes for the Mojito DHT Capable message.
     */
    private static final byte[] LIME_DHT_ACTIVE_CAPABLE_BYTES = { 'M', 'D', 'H', 'T' };
    
    /**
     * The current version of this message.
     */
    public static final int VERSION = 0;

    /**
     * The capabilities supported.
     */
    private final Set<SupportedMessageBlock> _capabilitiesSupported = new HashSet<SupportedMessageBlock>();

    /**
     * The current instance of this CVM that this node will forward to others
     */
    private static CapabilitiesVM _instance;

    /**
     * Constructs a new CapabilitiesVM from data read off the network.
     */
    CapabilitiesVM(byte[] guid, byte ttl, byte hops, 
                   int version, byte[] payload) throws BadPacketException {
        super(guid, ttl, hops, F_NULL_VENDOR_ID, F_CAPABILITIES, version,
              payload);

        // populate the Set of supported messages....
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(getPayload());
            int vectorSize = ByteOrder.ushort2int(ByteOrder.leb2short(bais));
            // constructing the SMB will cause a BadPacketException if the
            // network data is invalid
            for (int i = 0; i < vectorSize; i++)
                _capabilitiesSupported.add(new SupportedMessageBlock(bais));
        } catch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
        }
    }


    /**
     * Internal constructor for creating the sole instance of our 
     * CapabilitiesVM.
     */
    private CapabilitiesVM() {
        super(F_NULL_VENDOR_ID, F_CAPABILITIES, VERSION, derivePayload());
        addSupportedMessages(_capabilitiesSupported);
    }

    /**
     * Generates the default payload, using all our supported messages.
     */
    private static byte[] derivePayload() {
        Set<SupportedMessageBlock> hashSet = new HashSet<SupportedMessageBlock>();
        addSupportedMessages(hashSet);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)hashSet.size(), baos);
            for(SupportedMessageBlock currSMP : hashSet)
                currSMP.encode(baos);
            return baos.toByteArray();
        } catch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
            return null;
        }

    }

    // ADD NEW CAPABILITIES HERE AS YOU BUILD THEM....
    /**
     * Adds all supported capabilities to the given set.
     */
    private static void addSupportedMessages(Set<? super SupportedMessageBlock> hashSet) {
        SupportedMessageBlock smp = null;
        smp = new SupportedMessageBlock(FEATURE_SEARCH_BYTES, 
                                        FeatureSearchData.FEATURE_SEARCH_MAX_SELECTOR);
        hashSet.add(smp);
        
        smp = new SupportedMessageBlock(SIMPP_CAPABILITY_BYTES,
                                        SimppManager.instance().getVersion());
        hashSet.add(smp);
        
        smp = new SupportedMessageBlock(LIME_UPDATE_BYTES,
                                        UpdateHandler.instance().getLatestId());
        hashSet.add(smp);
        
        if(RouterService.isActiveDHTNode()) {
            smp = new SupportedMessageBlock(LIME_DHT_ACTIVE_CAPABLE_BYTES,
                                            RouterService.getDHTManager().getVersion());
            hashSet.add(smp);
        }
        
    }


    /** @return A CapabilitiesVM with the set of messages 
     *  this client supports.
     */
    public static CapabilitiesVM instance() {
        if (_instance == null)
            _instance = new CapabilitiesVM();
        return _instance;
    }


    /**
     * @return -1 if the ability isn't supported, else it returns the version 
     * of the message supported.
     */
    public int supportsCapability(byte[] capabilityName) {
        for(SupportedMessageBlock currSMP : _capabilitiesSupported) {
            int version = currSMP.matches(capabilityName);
            if (version > -1)
                return version;
        }
        return -1;
    }


    /** @return 1 or higher if capability queries are supported.  the version
     *  number gives some indication about what exactly is a supported.  if no
     *  support, returns -1.
     */
    public int supportsFeatureQueries() {
        return supportsCapability(FEATURE_SEARCH_BYTES);
    }
    

    /** @return true if 'what is new' capability query feature is supported.
     */
    public boolean supportsWhatIsNew() {
        return FeatureSearchData.supportsWhatIsNew(
            supportsCapability(FEATURE_SEARCH_BYTES));
    }
    
    /**
     * Returns the current SIMPP version.
     */
    public int supportsSIMPP() {
        return supportsCapability(SIMPP_CAPABILITY_BYTES);
    }
    
    /**
     * Returns the current Update version.
     */
    public int supportsUpdate() {
        return supportsCapability(LIME_UPDATE_BYTES);
    }
    
    /**
     * Returns the current DHT version if this node is a DHT node
     */
    public int supportsDHT() {
        return supportsCapability(LIME_DHT_ACTIVE_CAPABLE_BYTES);
    }

    // override super
    public boolean equals(Object other) {
        if(other == this)
            return true;
        
        // two of these messages are the same if the support the same messages
        if (other instanceof CapabilitiesVM) {
            CapabilitiesVM vmp = (CapabilitiesVM) other;
            return _capabilitiesSupported.equals(vmp._capabilitiesSupported);
        }

        return false;
    }
    
    /**
     * Constructs a new instance for this node to advertise,
     * using the latest version numbers of supported messages.
     */
    public static void reconstructInstance() {
        //replace _instance with a newer one, which will be created with the
        //correct simppVersion, a new _capabilitiesSupported will be created
        _instance = new CapabilitiesVM();
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
        
        public String toString() {
            return new String(_capabilityName) + "/" + _version;
        }

        public SupportedMessageBlock(byte[] capabilityName, int version) {
            _capabilityName = capabilityName;
            _version = version;
            _hashCode = computeHashCode(_capabilityName, _version);
        }

        /**
         * Constructs a new SupportedMessageBlock with data from the 
         * InputStream.  If not enough data is available,
         * throws BadPacketException.
         */
        public SupportedMessageBlock(InputStream encodedBlock)
          throws BadPacketException, IOException {
            if (encodedBlock.available() < 6)
                throw new BadPacketException("invalid block.");
            
            // first 4 bytes are capability name
            _capabilityName = new byte[4];
            encodedBlock.read(_capabilityName, 0, _capabilityName.length);

            _version = ByteOrder.ushort2int(ByteOrder.leb2short(encodedBlock));
            _hashCode = computeHashCode(_capabilityName, _version);

        }
        
        /**
         * Writes this capability (and version) to the OutputStream.
         */
        public void encode(OutputStream out) throws IOException {
            out.write(_capabilityName);
            ByteOrder.short2leb((short)_version, out);
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
                hashCode += 37*capabilityName[i];
            return hashCode;
        }
    }

    /** Overridden purely for stats handling.
     */
    protected void writePayload(OutputStream out) throws IOException {
        super.writePayload(out);
        SentMessageStatHandler.TCP_MESSAGES_SUPPORTED.addMessage(this);
    }

    /** Overridden purely for stats handling.
     */
    public void recordDrop() {
        super.recordDrop();
    }

    public String toString() {
        return "{CapabilitiesVM:"+super.toString()+"; supporting: " + _capabilitiesSupported + "}";
    }

}



