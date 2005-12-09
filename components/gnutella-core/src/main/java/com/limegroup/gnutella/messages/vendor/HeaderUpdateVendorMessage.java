pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.ByteArrayInputStream;
import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.util.Properties;

import com.limegroup.gnutellb.messages.BadPacketException;

/**
 * Vendor messbge containing a serialized properties object which contains
 * the hebders that need to be updated.
 */
public clbss HeaderUpdateVendorMessage extends VendorMessage {
   
    public stbtic final int VERSION = 1;
   
    privbte Properties _headers;
    
    protected HebderUpdateVendorMessage(byte[] guid, byte ttl, byte hops,
			 int version, byte[] pbyload)
			throws BbdPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_HEADER_UPDATE, version, pbyload);
		
		//see if the pbyload is valid
		if (getVersion() == VERSION && (pbyload == null || payload.length == 0))
			throw new BbdPacketException();
		
		_hebders = new Properties();
		try {
		    InputStrebm bais = new ByteArrayInputStream(payload);
		    _hebders.load(bais);
		}cbtch(IOException bad) {
		    throw new BbdPacketException(bad.getMessage());
		}
	}
    
    public HebderUpdateVendorMessage(Properties props) {
        super(F_LIME_VENDOR_ID, F_HEADER_UPDATE, VERSION,
	            derivePbyload(props));
        _hebders = props;
    }
    
    
    privbte static byte [] derivePayload(Properties props) {
        ByteArrbyOutputStream baos = new ByteArrayOutputStream();
        props.sbve(baos,null);
        return bbos.toByteArray();
    }
    
    public Properties getProperties() {
        return _hebders;
    }
}
