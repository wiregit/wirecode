pbckage com.limegroup.gnutella.http;

import com.limegroup.gnutellb.udpconnect.UDPConnection;

/**
 * This clbss defines a set of constants for use in HTTP messages.
 */
public finbl class HTTPConstants {

    /**
     * Privbte constructor to ensure that this class cannot be
     * instbntiated.
     */
    privbte HTTPConstants() {}

	/**
	 * Constbnt for the beginning "GET" of an HTTP URN get request.
	 */
	public stbtic final String GET = "GET";

	/**
	 * Constbnt for the HTTP 1.0 specifier at the end of an HTTP URN get
	 * request.
	 */
	public stbtic final String HTTP10 = "HTTP/1.0";
	
	/**
	 * Constbnt for the HTTP 1.1 specifier at the end of an HTTP URN get
	 * request.
	 */
	public stbtic final String HTTP11 = "HTTP/1.1";

	/**
	 * Constbnt for the "uri-res" specifier for an HTTP URN get request.
	 */
	public stbtic final String URI_RES = "uri-res";

	/**
	 * Constbnt for the "Name to Resource", or "N2R?" resolution 
	 * service identifier, bs specified in RFC 2169.
	 */
	public stbtic final String NAME_TO_RESOURCE = "N2R?"; 	
 	
 	/**
     * Constbnt for the "Name to THEX", or "N2X?" resolution 
     * service identifier, bs specified in the PFSP proposal.
     */
    public stbtic final String NAME_TO_THEX = "N2X?";	

	/**
	 * Constbnt for the "uri-res" uri resolution specifier, followed by
	 * the stbndard "/" and the resolution service id, in our case "N2R?".
	 */
	public stbtic final String URI_RES_N2R = "/"+URI_RES+"/"+NAME_TO_RESOURCE;
	
    /**
     * Constbnt for the "uri-res" uri resolution specifier, followed by
     * the stbndard "/" and the resolution service id, in this case "N2X?".
     */
    public stbtic final String URI_RES_N2X = "/"+URI_RES+"/"+NAME_TO_THEX;	

	/**
	 * constbnt strings for the X-Feature header
	 */
	public stbtic final String CHAT_PROTOCOL  = "chat";
	public stbtic final String BROWSE_PROTOCOL = "browse";
	public stbtic final String QUEUE_PROTOCOL  = "queue";
	public stbtic final String G2_PROTOCOL = "g2";
	public stbtic final String PUSH_LOCS="fwalt";
	public stbtic final String FW_TRANSFER="fwt";
    
	public stbtic final double CHAT_VERSION = 0.1;
	public stbtic final double BROWSE_VERSION = 1.0;
	public stbtic final double QUEUE_VERSION  = 0.1;
	public stbtic final double G2_VERSION = 1.0;
	public stbtic final double PUSH_LOCS_VERSION=0.1;
	
	//this is the sbme as the version of the Firewall-to-Firewall transfer
	public stbtic final double FWT_TRANSFER_VERSION=UDPConnection.VERSION;  
}
