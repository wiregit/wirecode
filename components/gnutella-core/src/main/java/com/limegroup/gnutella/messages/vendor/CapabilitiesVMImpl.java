package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.FeatureSearchData;

public class CapabilitiesVMImpl extends AbstractVendorMessage implements CapabilitiesVM {
    
    /**
     * The capabilities supported.
     */
    private final Set<SupportedMessageBlock> _capabilitiesSupported;

    /**
     * Constructs a new CapabilitiesVM from data read off the network.
     */
    CapabilitiesVMImpl(byte[] guid, byte ttl, byte hops, 
                   int version, byte[] payload, Network network) throws BadPacketException {
        super(guid, ttl, hops, F_NULL_VENDOR_ID, F_CAPABILITIES, version,
              payload, network);

        _capabilitiesSupported = new HashSet<SupportedMessageBlock>();
        
        // populate the Set of supported messages....
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(getPayload());
            int vectorSize = ByteOrder.ushort2int(ByteOrder.leb2short(bais));
            // constructing the SMB will cause a BadPacketException if the
            // network data is invalid
            for (int i = 0; i < vectorSize; i++) {
                _capabilitiesSupported.add(new SupportedMessageBlock(bais));
            }
        } catch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
        }
    }


    /**
     * Internal constructor for creating the sole instance of our 
     * CapabilitiesVM.
     */
    CapabilitiesVMImpl(Set<SupportedMessageBlock> _capabilitiesSupported) {
        super(F_NULL_VENDOR_ID, F_CAPABILITIES, VERSION, derivePayload(_capabilitiesSupported));
        this._capabilitiesSupported = _capabilitiesSupported;
    }
    
    /**
     * Generates the default payload, using all our supported messages.
     */
    private static byte[] derivePayload(Set<SupportedMessageBlock> hashSet) {
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
    
    /**
     * Return 1 or higher if TLS is supported by the connection.
     * This does not necessarily mean the connection is over
     * TLS though.
     */
    public int supportsTLS() {
        return supportsCapability(TLS_SUPPORT_BYTES);
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
     * Returns the current DHT version if this node is an ACTIVE DHT node
     */
    public int isActiveDHTNode() {
        return supportsCapability(DHTMode.ACTIVE.getCapabilityName());
    }
    
    /**
     * Returns the current DHT version if this node is an PASSIVE DHT node
     */
    public int isPassiveDHTNode() {
        return supportsCapability(DHTMode.PASSIVE.getCapabilityName());
    }

    /**
     * Returns the current DHT version if this node is an PASSIVE_LEAF DHT node
     */
    public int isPassiveLeafNode() {
        return supportsCapability(DHTMode.PASSIVE_LEAF.getCapabilityName());
    }
    
    /**
     * @return true unless the remote host indicated they can't accept 
     * incoming tcp. If they didn't say anything we assume they can
     */
    public boolean canAcceptIncomingTCP() {
        return supportsCapability(INCOMING_TCP_BYTES) != 0;
    }
    
    /**
     * @return true unless the remote host indicated they can't do 
     * firewall-to-firewall transfers. If they didn't say anything we assume they can
     */
    public boolean canDoFWT() {
        return supportsCapability(FWT_SUPPORT_BYTES) != 0;
    }
    
    // override super
    public boolean equals(Object other) {
        if(other == this)
            return true;
        
        // two of these messages are the same if the support the same messages
        if (other instanceof CapabilitiesVMImpl) {
            CapabilitiesVMImpl vmp = (CapabilitiesVMImpl) other;
            return _capabilitiesSupported.equals(vmp._capabilitiesSupported);
        }

        return false;
    }
    
    // override super
    public int hashCode() {
        return 17*_capabilitiesSupported.hashCode();
    }
    

    /** Container for vector elements.
     */  
    public static class SupportedMessageBlock {
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
    }

    public String toString() {
        return "{CapabilitiesVM:"+super.toString()+"; supporting: " + _capabilitiesSupported + "}";
    }

}
