package com.limegroup.gnutella.handshaking;

import java.util.Properties;

/**
 * This class contains the necessary information to form a response to a 
 * connection handshake.  It contains a status code, a status message, and
 * the headers to use in the response.
 *
 * There are only two ways to create a HandshakeResponse.
 *
 * 1) Create an instance which defaults the status code and status message to
 *    be "200 OK".  Only the headers used in the response need to be passed in.
 * 
 * 2) Create an instance with a custom status code, status message, and the
 *    headers used in the response.
 */
public class HandshakeResponse {
    /**
     * The "default" status code in a connection handshake indicating that
     * the handshake was successful and the connection can be established.
     */
    public static final int OK = 200;
    
     /**
     * The "default" status message in a connection handshake indicating that
     * the handshake was successful and the connection can be established.
     */
    public static final String OK_MESSAGE = "OK";

    /** The error code that a shielded leaf node should give to incoming
     *  connections.  */
    public static final int SHIELDED = 503;
    /** The error message that a shielded leaf node should give to incoming
     *  connections.  */
    public static final String SHIELDED_MESSAGE = "I am a shielded leaf node";

    /** The error code that a node with no slots should give to incoming
     *  connections.  */
    public static final int SLOTS_FULL = 503;
    /** The error message that a node with no slots should give to incoming
     *  connections.  */
    public static final String SLOTS_FULL_MESSAGE = "Service unavailable";
    
    /**
     * Default bad status code to be used while rejecting connections
     */
    public static final int DEFAULT_BAD_STATUS_CODE = 503;
    
    /**
     * Default bad status message to be used while rejecting connections
     */
    public static final String DEFAULT_BAD_STATUS_MESSAGE 
        = "Service Not Available";
    
    /**
     * status code for unauthorized attempt
     */
    public static final int UNAUTHORIZED_CODE = 401;
    
    /**
     * status message for unauthorized attempt
     */
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    
    /**
     * Message indicating that we are unable to authenticate
     */
    public static final String UNABLE_TO_AUTHENTICATE 
        = "Unable To Authenticate";
    
    /**
     * Message indicating that we are trying to authenticate
     */
    public static final String AUTHENTICATING = "AUTHENTICATING";

    /**
     * HTTP-like status code used when handshaking (e.g., 200, 401, 503).
     */
    private int statusCode;

    /**
     * Message used with status code when handshaking (e.g., "OK, "Service Not
     * Available").  The status message together with the status code make up 
     * the status line (i.e., first line) of an HTTP-like response to a 
     * connection handshake.
     */
    private String statusMessage;

    /**
     * Headers to use in the response to a connection handshake.
     */
    private Properties headers;

    /**
     * Creates a HandshakeResponse which defaults the status code and status
     * message to be "200 Ok" and uses no headers in the response.
     */
    public HandshakeResponse() {
        statusCode = OK;
        statusMessage = OK_MESSAGE;
        this.headers = null;
    }    
    
    /**
     * Creates a HandshakeResponse which defaults the status code and status
     * message to be "200 Ok" and uses the desired headers in the response. 
     * @param headers the headers to use in the response. 
     */
    public HandshakeResponse(Properties headers) {
        statusCode = OK;
        statusMessage = OK_MESSAGE;
        this.headers = headers;
    }    

    /**
     * Creates a HandshakeResponse with the desired status code, status message, 
     * and headers to respond with.
     * @param code the response code to use.
     * @param message the response message to use.
     * @param headers the headers to use in the response.
     */
    public HandshakeResponse(int code, String message, Properties headers) { 
        this.statusCode = code;
        this.statusMessage = message;
        this.headers = headers;
    }
    
    /**
     * Creates a HandshakeResponse with the desired status line, 
     * and headers
     * @param statusLine the status code and status message together used in the
     * HTTP status line. (e.g., "200 OK", "503 Service Not Available")
     * @param headers the headers to use in the response.
     */
    public HandshakeResponse(String statusLine, Properties headers) { 
        try{
            //get the status code and message out of the status line
            int statusMessageIndex = statusLine.indexOf(" ");
            this.statusCode = Integer.parseInt(statusLine.substring(0, 
                statusMessageIndex).trim());
            this.statusMessage = statusLine.substring(
                statusMessageIndex).trim();
        }
        catch(Exception e){
            //in case of any exception, use default bad codes
            this.statusCode = DEFAULT_BAD_STATUS_CODE;
            this.statusMessage = DEFAULT_BAD_STATUS_MESSAGE;
        }
        
        this.headers = headers;
    }
    
    /** 
     * Returns the response code.
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Returns the status message. 
     * @return the status message (e.g. "OK" , "Service Not Available" etc.)
     */
    public String getStatusMessage(){
        return statusMessage;
    }
    
    /**
     * Tells if the status returned was OK or not.
     * @return true, if the status returned was not the OK status, false
     * otherwise
     */
    public boolean notOKStatusCode(){
        if(statusCode != OK)
            return true;
        else
            return false;
    }
    

    /**
     * Returns the status code and status message together used in a 
     * status line. (e.g., "200 OK", "503 Service Not Available")
     */
    public String getStatusLine() {
        return new String(statusCode + " " + statusMessage);
    }

    /**
     * Returns the headers to use in the response.
     */
    public Properties getHeaders() {
        return headers;
    }
}

