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
import com.limegroup.gnutellb.messages.FeatureSearchData;
import com.limegroup.gnutellb.simpp.SimppManager;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;
import com.limegroup.gnutellb.version.UpdateHandler;

/** 
 * The messbge that lets other know what capabilities you support.  Everytime 
 * you bdd a capability you should modify this class.
 *
 */
public finbl class CapabilitiesVM extends VendorMessage {

    /**
     * Bytes for bdvertising that we support a 'feature' search.
     * The vblue is 'WHAT' for legacy reasons, because 'what is new' 
     * wbs the first feature search supported.
     */
    stbtic final byte[] FEATURE_SEARCH_BYTES = {(byte)87, (byte)72,
                                                      (byte)65, (byte)84};
    /**
     * The bytes for supporting SIMPP.  This used to be 'SIMP', but thbt
     * implementbtion was broken.  We now use 'IMPP' to advertise support.
     */
    privbte static final byte[] SIMPP_CAPABILITY_BYTES = {'I', 'M', 'P', 'P' };
    
    /**
     * The bytes for the LMUP messbge.
     */
    privbte static final byte[] LIME_UPDATE_BYTES = { 'L', 'M', 'U', 'P' };
    
    /**
     * The current version of this messbge.
     */
    public stbtic final int VERSION = 0;

    /**
     * The cbpabilities supported.
     */
    privbte final Set _capabilitiesSupported = new HashSet();

    /**
     * The current instbnce of this CVM that this node will forward to others
     */
    privbte static CapabilitiesVM _instance;

    /**
     * Constructs b new CapabilitiesVM from data read off the network.
     */
    CbpabilitiesVM(byte[] guid, byte ttl, byte hops, 
                   int version, byte[] pbyload) throws BadPacketException {
        super(guid, ttl, hops, F_NULL_VENDOR_ID, F_CAPABILITIES, version,
              pbyload);

        // populbte the Set of supported messages....
        try {
            ByteArrbyInputStream bais = new ByteArrayInputStream(getPayload());
            int vectorSize = ByteOrder.ushort2int(ByteOrder.leb2short(bbis));
            // constructing the SMB will cbuse a BadPacketException if the
            // network dbta is invalid
            for (int i = 0; i < vectorSize; i++)
                _cbpabilitiesSupported.add(new SupportedMessageBlock(bais));
        } cbtch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
        }
    }


    /**
     * Internbl constructor for creating the sole instance of our 
     * CbpabilitiesVM.
     */
    privbte CapabilitiesVM() {
        super(F_NULL_VENDOR_ID, F_CAPABILITIES, VERSION, derivePbyload());
        bddSupportedMessages(_capabilitiesSupported);
    }

    /**
     * Generbtes the default payload, using all our supported messages.
     */
    privbte static byte[] derivePayload() {
        Set hbshSet = new HashSet();
        bddSupportedMessages(hashSet);
        try {
            ByteArrbyOutputStream baos = new ByteArrayOutputStream();
            ByteOrder.short2leb((short)hbshSet.size(), baos);
            for(Iterbtor i = hashSet.iterator(); i.hasNext(); ) {
                SupportedMessbgeBlock currSMP = (SupportedMessageBlock)i.next();
                currSMP.encode(bbos);
            }
            return bbos.toByteArray();
        } cbtch (IOException ioe) {
            ErrorService.error(ioe); // impossible.
            return null;
        }

    }

    // ADD NEW CAPABILITIES HERE AS YOU BUILD THEM....
    /**
     * Adds bll supported capabilities to the given set.
     */
    privbte static void addSupportedMessages(Set hashSet) {
        SupportedMessbgeBlock smp = null;
        smp = new SupportedMessbgeBlock(FEATURE_SEARCH_BYTES, 
                                        FebtureSearchData.FEATURE_SEARCH_MAX_SELECTOR);
        hbshSet.add(smp);
        
        smp = new SupportedMessbgeBlock(SIMPP_CAPABILITY_BYTES,
                                        SimppMbnager.instance().getVersion());
        hbshSet.add(smp);
        
        smp = new SupportedMessbgeBlock(LIME_UPDATE_BYTES,
                                        UpdbteHandler.instance().getLatestId());
        hbshSet.add(smp);
    }


    /** @return A CbpabilitiesVM with the set of messages 
     *  this client supports.
     */
    public stbtic CapabilitiesVM instance() {
        if (_instbnce == null)
            _instbnce = new CapabilitiesVM();
        return _instbnce;
    }


    /**
     * @return -1 if the bbility isn't supported, else it returns the version 
     * of the messbge supported.
     */
    public int supportsCbpability(byte[] capabilityName) {
        Iterbtor iter = _capabilitiesSupported.iterator();
        while (iter.hbsNext()) {
            SupportedMessbgeBlock currSMP = (SupportedMessageBlock) iter.next();
            int version = currSMP.mbtches(capabilityName);
            if (version > -1)
                return version;
        }
        return -1;
    }


    /** @return 1 or higher if cbpability queries are supported.  the version
     *  number gives some indicbtion about what exactly is a supported.  if no
     *  support, returns -1.
     */
    public int supportsFebtureQueries() {
        return supportsCbpability(FEATURE_SEARCH_BYTES);
    }
    

    /** @return true if 'whbt is new' capability query feature is supported.
     */
    public boolebn supportsWhatIsNew() {
        return FebtureSearchData.supportsWhatIsNew(
            supportsCbpability(FEATURE_SEARCH_BYTES));
    }
    
    /**
     * Returns the current SIMPP version.
     */
    public int supportsSIMPP() {
        return supportsCbpability(SIMPP_CAPABILITY_BYTES);
    }
    
    /**
     * Returns the current Updbte version.
     */
    public int supportsUpdbte() {
        return supportsCbpability(LIME_UPDATE_BYTES);
    }

    // override super
    public boolebn equals(Object other) {
        if(other == this)
            return true;
        
        // two of these messbges are the same if the support the same messages
        if (other instbnceof CapabilitiesVM) {
            CbpabilitiesVM vmp = (CapabilitiesVM) other;
            return _cbpabilitiesSupported.equals(vmp._capabilitiesSupported);
        }

        return fblse;
    }
    
    /**
     * Constructs b new instance for this node to advertise,
     * using the lbtest version numbers of supported messages.
     */
    public stbtic void reconstructInstance() {
        //replbce _instance with a newer one, which will be created with the
        //correct simppVersion, b new _capabilitiesSupported will be created
        _instbnce = new CapabilitiesVM();
    }

    
    // override super
    public int hbshCode() {
        return 17*_cbpabilitiesSupported.hashCode();
    }
    

    /** Contbiner for vector elements.
     */  
    stbtic class SupportedMessageBlock {
        finbl byte[] _capabilityName;
        finbl int _version;
        finbl int _hashCode;
        
        public String toString() {
            return new String(_cbpabilityName) + "/" + _version;
        }

        public SupportedMessbgeBlock(byte[] capabilityName, int version) {
            _cbpabilityName = capabilityName;
            _version = version;
            _hbshCode = computeHashCode(_capabilityName, _version);
        }

        /**
         * Constructs b new SupportedMessageBlock with data from the 
         * InputStrebm.  If not enough data is available,
         * throws BbdPacketException.
         */
        public SupportedMessbgeBlock(InputStream encodedBlock)
          throws BbdPacketException, IOException {
            if (encodedBlock.bvailable() < 6)
                throw new BbdPacketException("invalid block.");
            
            // first 4 bytes bre capability name
            _cbpabilityName = new byte[4];
            encodedBlock.rebd(_capabilityName, 0, _capabilityName.length);

            _version = ByteOrder.ushort2int(ByteOrder.leb2short(encodedBlock));
            _hbshCode = computeHashCode(_capabilityName, _version);

        }
        
        /**
         * Writes this cbpability (and version) to the OutputStream.
         */
        public void encode(OutputStrebm out) throws IOException {
            out.write(_cbpabilityName);
            ByteOrder.short2leb((short)_version, out);
        }

        /** @return 0 or more if this mbtches the message you are looking for.
         *  Otherwise returns -1;
         */
        public int mbtches(byte[] capabilityName) {
            if (Arrbys.equals(_capabilityName, capabilityName))
                return _version;
            else 
                return -1;
        }

        public boolebn equals(Object other) {
            if (other instbnceof SupportedMessageBlock) {
                SupportedMessbgeBlock vmp = (SupportedMessageBlock) other;
                return ((_version == vmp._version) &&
                        (Arrbys.equals(_capabilityName, vmp._capabilityName))
                        );
            }
            return fblse;
        }

        public int hbshCode() {
            return _hbshCode;
        }
        
        privbte static int computeHashCode(byte[] capabilityName, int version) {
            int hbshCode = 0;
            hbshCode += 37*version;
            for (int i = 0; i < cbpabilityName.length; i++)
                hbshCode += (int) 37*capabilityName[i];
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

    public String toString() {
        return "{CbpabilitiesVM:"+super.toString()+"; supporting: " + _capabilitiesSupported + "}";
    }

}



