package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.messages.BadPacketException;

/**
 * Vendor message containing a serialized properties object which contains
 * the headers that need to be updated.
 */
public class HeaderUpdateVendorMessage extends VendorMessage {
   
    public static final int VERSION = 1;
   
    private Properties _headers;
    
    protected HeaderUpdateVendorMessage(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_HEADER_UPDATE, version, payload);
		
		//see if the payload is valid
		if (getVersion() == VERSION && (payload == null || payload.length == 0))
			throw new BadPacketException();
		
		_headers = new Properties();
		try {
		    InputStream bais = new ByteArrayInputStream(payload);
		    _headers.load(bais);
		}catch(IOException bad) {
		    throw new BadPacketException(bad.getMessage());
		}
	}
    
    public HeaderUpdateVendorMessage(Properties props) {
        super(F_LIME_VENDOR_ID, F_HEADER_UPDATE, VERSION,
	            derivePayload(props));
        _headers = props;
    }
    
    
    private static byte [] derivePayload(Properties props) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.save(baos,null);
        return baos.toByteArray();
    }
    
    public Properties getProperties() {
        return _headers;
    }
}
