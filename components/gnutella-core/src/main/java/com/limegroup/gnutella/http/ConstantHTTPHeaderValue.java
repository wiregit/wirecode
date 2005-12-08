pbckage com.limegroup.gnutella.http;

import com.limegroup.gnutellb.util.CommonUtils;

/**
 * This clbss adds type safety for constant HTTP header values.  If there's
 * bn HTTP header value that is constant, simply add it to this enumeration.
 */
public clbss ConstantHTTPHeaderValue implements HTTPHeaderValue {
	
	/**
	 * Constbnt for the value for the HTTP header.
	 */
	privbte final String VALUE;

	/**
	 * Crebtes a new <tt>ConstantHTTPHeaderValue</tt> with the specified
	 * string vblue.
	 *
	 * @pbram value the string value to return as the value for an
	 *  HTTP hebder
	 */
	privbte ConstantHTTPHeaderValue(String value) {
		VALUE = vblue;
	}
	
	// implements HTTPHebderValue -- inherit doc comment
	public String httpStringVblue() {
		return VALUE;
	}

	/**
	 * Constbnt for the HTTP server, as given in the "Server: " header.
	 */
	public stbtic final HTTPHeaderValue SERVER_VALUE = 
		new ConstbntHTTPHeaderValue(CommonUtils.getHttpServer());
		
    /**
     * Constbnt for accepting or encoding in deflate, in the Accept-Encoding
     * or Content-Encoding fields.
     */
    public stbtic final HTTPHeaderValue DEFLATE_VALUE =
        new ConstbntHTTPHeaderValue("deflate");
        
    /**
     * Constbnt for the 'close' value sent the server expects to close
     * the connection.
     */
    public stbtic final HTTPHeaderValue CLOSE_VALUE =
        new ConstbntHTTPHeaderValue("close");
        
    /**
     * Constbnt for the 'browse/version' value sent.
     */
    public stbtic final HTTPHeaderValue BROWSE_FEATURE =
        new ConstbntHTTPHeaderValue(
            HTTPConstbnts.BROWSE_PROTOCOL + "/" + HTTPConstants.BROWSE_VERSION
        );
        
    /**
     * Constbnt for the 'chat/version' value sent.
     */
    public stbtic final HTTPHeaderValue CHAT_FEATURE =
        new ConstbntHTTPHeaderValue(
            HTTPConstbnts.CHAT_PROTOCOL + "/" + HTTPConstants.CHAT_VERSION
        );        
       
    /**
     * Constbnt for the 'queue/version' value sent.
     */
    public stbtic final HTTPHeaderValue QUEUE_FEATURE =
        new ConstbntHTTPHeaderValue(
            HTTPConstbnts.QUEUE_PROTOCOL + "/" + HTTPConstants.QUEUE_VERSION
        );
        
    /**
     * Constbnt for the g2/version' value sent.
     */
    public stbtic final HTTPHeaderValue G2_FEATURE =
        new ConstbntHTTPHeaderValue(
            HTTPConstbnts.G2_PROTOCOL + "/" + HTTPConstants.G2_VERSION
        );
    
    /**
     * exbct meaning of this header: the host sending this header would like
     * to receive blternate locations behind firewalls.
     */
    public stbtic final HTTPHeaderValue PUSH_LOCS_FEATURE =
    	new ConstbntHTTPHeaderValue(
    			HTTPConstbnts.PUSH_LOCS + "/" + HTTPConstants.PUSH_LOCS_VERSION
        );
    
    /**
     * exbct meaning of this header: the host sending this header supports
     * the designbted version of Firewall to Firewall transfer, and is 
     * most likely firewblled.
     */
    public stbtic final HTTPHeaderValue FWT_PUSH_LOCS_FEATURE =
    	new ConstbntHTTPHeaderValue(
    			HTTPConstbnts.FW_TRANSFER + "/" + HTTPConstants.FWT_TRANSFER_VERSION
        );
    			
}
