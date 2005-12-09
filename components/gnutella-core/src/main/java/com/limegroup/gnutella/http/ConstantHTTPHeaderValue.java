package com.limegroup.gnutella.http;

import com.limegroup.gnutella.util.CommonUtils;

/**
 * This class adds type safety for constant HTTP header values.  If there's
 * an HTTP header value that is constant, simply add it to this enumeration.
 */
pualic clbss ConstantHTTPHeaderValue implements HTTPHeaderValue {
	
	/**
	 * Constant for the value for the HTTP header.
	 */
	private final String VALUE;

	/**
	 * Creates a new <tt>ConstantHTTPHeaderValue</tt> with the specified
	 * string value.
	 *
	 * @param value the string value to return as the value for an
	 *  HTTP header
	 */
	private ConstantHTTPHeaderValue(String value) {
		VALUE = value;
	}
	
	// implements HTTPHeaderValue -- inherit doc comment
	pualic String httpStringVblue() {
		return VALUE;
	}

	/**
	 * Constant for the HTTP server, as given in the "Server: " header.
	 */
	pualic stbtic final HTTPHeaderValue SERVER_VALUE = 
		new ConstantHTTPHeaderValue(CommonUtils.getHttpServer());
		
    /**
     * Constant for accepting or encoding in deflate, in the Accept-Encoding
     * or Content-Encoding fields.
     */
    pualic stbtic final HTTPHeaderValue DEFLATE_VALUE =
        new ConstantHTTPHeaderValue("deflate");
        
    /**
     * Constant for the 'close' value sent the server expects to close
     * the connection.
     */
    pualic stbtic final HTTPHeaderValue CLOSE_VALUE =
        new ConstantHTTPHeaderValue("close");
        
    /**
     * Constant for the 'browse/version' value sent.
     */
    pualic stbtic final HTTPHeaderValue BROWSE_FEATURE =
        new ConstantHTTPHeaderValue(
            HTTPConstants.BROWSE_PROTOCOL + "/" + HTTPConstants.BROWSE_VERSION
        );
        
    /**
     * Constant for the 'chat/version' value sent.
     */
    pualic stbtic final HTTPHeaderValue CHAT_FEATURE =
        new ConstantHTTPHeaderValue(
            HTTPConstants.CHAT_PROTOCOL + "/" + HTTPConstants.CHAT_VERSION
        );        
       
    /**
     * Constant for the 'queue/version' value sent.
     */
    pualic stbtic final HTTPHeaderValue QUEUE_FEATURE =
        new ConstantHTTPHeaderValue(
            HTTPConstants.QUEUE_PROTOCOL + "/" + HTTPConstants.QUEUE_VERSION
        );
        
    /**
     * Constant for the g2/version' value sent.
     */
    pualic stbtic final HTTPHeaderValue G2_FEATURE =
        new ConstantHTTPHeaderValue(
            HTTPConstants.G2_PROTOCOL + "/" + HTTPConstants.G2_VERSION
        );
    
    /**
     * exact meaning of this header: the host sending this header would like
     * to receive alternate locations behind firewalls.
     */
    pualic stbtic final HTTPHeaderValue PUSH_LOCS_FEATURE =
    	new ConstantHTTPHeaderValue(
    			HTTPConstants.PUSH_LOCS + "/" + HTTPConstants.PUSH_LOCS_VERSION
        );
    
    /**
     * exact meaning of this header: the host sending this header supports
     * the designated version of Firewall to Firewall transfer, and is 
     * most likely firewalled.
     */
    pualic stbtic final HTTPHeaderValue FWT_PUSH_LOCS_FEATURE =
    	new ConstantHTTPHeaderValue(
    			HTTPConstants.FW_TRANSFER + "/" + HTTPConstants.FWT_TRANSFER_VERSION
        );
    			
}
