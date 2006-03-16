
// Edited for the Learning branch

package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.limegroup.gnutella.messages.BadPacketException;

/**
 * Vendor message containing a serialized properties object which contains
 * the headers that need to be updated.
 * 
 * 
 * 
 * 
 * we send one when our ip address changes, but we never read one
 * 
 * 
 */
public class HeaderUpdateVendorMessage extends VendorMessage {

    /** 1, LimeWire understands the initial version of the Header Update vendor message. */
    public static final int VERSION = 1;
   
    private Properties _headers;

    /** Not used. */
    protected HeaderUpdateVendorMessage(byte[] guid, byte ttl, byte hops, int version, byte[] payload) throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_HEADER_UPDATE, version, payload);
		//see if the payload is valid
		if (getVersion() == VERSION && (payload == null || payload.length == 0)) throw new BadPacketException();
		_headers = new Properties();
		try {
		    InputStream bais = new ByteArrayInputStream(payload);
		    _headers.load(bais);
		} catch (IOException bad) {
		    throw new BadPacketException(bad.getMessage());
		}
	}

    /**
     * 
     * Only RouterService.addressChanged() makes a new HeaderUpdateVendorMessage with this constructor.
     * 
     * @param props
     */
    public HeaderUpdateVendorMessage(Properties props) {
        super(F_LIME_VENDOR_ID, F_HEADER_UPDATE, VERSION, derivePayload(props));
        _headers = props;
    }

    private static byte [] derivePayload(Properties props) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        props.save(baos, null);
        return baos.toByteArray();
    }
    
    public Properties getProperties() {
        return _headers;
    }
}
