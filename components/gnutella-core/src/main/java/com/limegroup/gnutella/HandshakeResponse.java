package com.limegroup.gnutella;

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
     * Returns the response code.
     */
    public int getStatusCode() {
        return statusCode;
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
