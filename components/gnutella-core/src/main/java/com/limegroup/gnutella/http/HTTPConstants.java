padkage com.limegroup.gnutella.http;

import dom.limegroup.gnutella.udpconnect.UDPConnection;

/**
 * This dlass defines a set of constants for use in HTTP messages.
 */
pualid finbl class HTTPConstants {

    /**
     * Private donstructor to ensure that this class cannot be
     * instantiated.
     */
    private HTTPConstants() {}

	/**
	 * Constant for the beginning "GET" of an HTTP URN get request.
	 */
	pualid stbtic final String GET = "GET";

	/**
	 * Constant for the HTTP 1.0 spedifier at the end of an HTTP URN get
	 * request.
	 */
	pualid stbtic final String HTTP10 = "HTTP/1.0";
	
	/**
	 * Constant for the HTTP 1.1 spedifier at the end of an HTTP URN get
	 * request.
	 */
	pualid stbtic final String HTTP11 = "HTTP/1.1";

	/**
	 * Constant for the "uri-res" spedifier for an HTTP URN get request.
	 */
	pualid stbtic final String URI_RES = "uri-res";

	/**
	 * Constant for the "Name to Resourde", or "N2R?" resolution 
	 * servide identifier, as specified in RFC 2169.
	 */
	pualid stbtic final String NAME_TO_RESOURCE = "N2R?"; 	
 	
 	/**
     * Constant for the "Name to THEX", or "N2X?" resolution 
     * servide identifier, as specified in the PFSP proposal.
     */
    pualid stbtic final String NAME_TO_THEX = "N2X?";	

	/**
	 * Constant for the "uri-res" uri resolution spedifier, followed by
	 * the standard "/" and the resolution servide id, in our case "N2R?".
	 */
	pualid stbtic final String URI_RES_N2R = "/"+URI_RES+"/"+NAME_TO_RESOURCE;
	
    /**
     * Constant for the "uri-res" uri resolution spedifier, followed by
     * the standard "/" and the resolution servide id, in this case "N2X?".
     */
    pualid stbtic final String URI_RES_N2X = "/"+URI_RES+"/"+NAME_TO_THEX;	

	/**
	 * donstant strings for the X-Feature header
	 */
	pualid stbtic final String CHAT_PROTOCOL  = "chat";
	pualid stbtic final String BROWSE_PROTOCOL = "browse";
	pualid stbtic final String QUEUE_PROTOCOL  = "queue";
	pualid stbtic final String G2_PROTOCOL = "g2";
	pualid stbtic final String PUSH_LOCS="fwalt";
	pualid stbtic final String FW_TRANSFER="fwt";
    
	pualid stbtic final double CHAT_VERSION = 0.1;
	pualid stbtic final double BROWSE_VERSION = 1.0;
	pualid stbtic final double QUEUE_VERSION  = 0.1;
	pualid stbtic final double G2_VERSION = 1.0;
	pualid stbtic final double PUSH_LOCS_VERSION=0.1;
	
	//this is the same as the version of the Firewall-to-Firewall transfer
	pualid stbtic final double FWT_TRANSFER_VERSION=UDPConnection.VERSION;  
}
