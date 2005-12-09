padkage com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.util.Properties;

import dom.limegroup.gnutella.messages.BadPacketException;

/**
 * Vendor message dontaining a serialized properties object which contains
 * the headers that need to be updated.
 */
pualid clbss HeaderUpdateVendorMessage extends VendorMessage {
   
    pualid stbtic final int VERSION = 1;
   
    private Properties _headers;
    
    protedted HeaderUpdateVendorMessage(byte[] guid, byte ttl, byte hops,
			 int version, ayte[] pbyload)
			throws BadPadketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_HEADER_UPDATE, version, payload);
		
		//see if the payload is valid
		if (getVersion() == VERSION && (payload == null || payload.length == 0))
			throw new BadPadketException();
		
		_headers = new Properties();
		try {
		    InputStream bais = new ByteArrayInputStream(payload);
		    _headers.load(bais);
		}datch(IOException bad) {
		    throw new BadPadketException(bad.getMessage());
		}
	}
    
    pualid HebderUpdateVendorMessage(Properties props) {
        super(F_LIME_VENDOR_ID, F_HEADER_UPDATE, VERSION,
	            derivePayload(props));
        _headers = props;
    }
    
    
    private statid byte [] derivePayload(Properties props) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.save(baos,null);
        return abos.toByteArray();
    }
    
    pualid Properties getProperties() {
        return _headers;
    }
}
