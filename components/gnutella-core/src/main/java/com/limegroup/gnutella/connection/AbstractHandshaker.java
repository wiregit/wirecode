package com.limegroup.gnutella.connection;

import java.util.Properties;

import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.http.HTTPHeader;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * This class abstracts out generalized properties of the handshaking 
 * subclasses, such as the headers to write and storing any headers that are 
 * read.  Subclasses perform specialized handshaking depending on whether or
 * not they block on network IO or any other factors.
 */
public abstract class AbstractHandshaker implements Handshaker {

    /**
     * Gnutella 0.6 connect string.
     */
    protected static String GNUTELLA_CONNECT_06 = "GNUTELLA CONNECT/0.6";

    /**
     * Gnutella 0.6 accept connection strings.
     */
    protected static final String GNUTELLA_OK_06  = "GNUTELLA/0.6 200 OK";
    protected static final String GNUTELLA_06     = "GNUTELLA/0.6";
    protected static final String _200_OK         = " 200 OK";
    protected static final String GNUTELLA_06_200 = "GNUTELLA/0.6 200";
    protected static final String CONNECT         = "CONNECT/";
    
    /** 
     * End of line for Gnutella 0.6 handshakes.
     */
    protected static final String CRLF="\r\n";
    
    
    /**
     * The <tt>Connection</tt> instance that manages this connection with
     * another Gnutella host.
     */
    protected final Connection CONNECTION;
    
    /** 
     * The headers read from the connection.
     */
    protected final Properties HEADERS_READ = new Properties();
    
    /** 
     * The list of all properties written during the handshake sequence,
     * analogous to HEADERS_READ.  This is needed because
     * RESPONSE_HEADERS lazily calculates properties according to what it
     * read. 
     */
    protected final Properties HEADERS_WRITTEN = new Properties();
    
    /** 
     * For outgoing Gnutella 0.6 connections, the properties written
     * after "GNUTELLA CONNECT".  Null otherwise. 
     */
    protected final Properties REQUEST_HEADERS;
    
    /** 
     * For outgoing Gnutella 0.6 connections, a function calculating the
     * properties written after the server's "GNUTELLA OK".  For incoming
     * Gnutella 0.6 connections, the properties written after the client's
     * "GNUTELLA CONNECT".
     * Non-final so that the responder can be garbage collected after we've
     * concluded the responding (by setting to null).
     */
    protected final HandshakeResponder RESPONSE_HEADERS;
    
    /**
     * Protected <tt>HeaderWriter</tt> for writing Gnutella connection headers.
     */
    protected HeaderWriter _headerWriter;
   
    /**
     * Protected <tt>HeaderReader</tt> for reading Gnutella connection headers.
     */    
    protected HeaderReader _headerReader;
    
    /**
     * Creates a new <tt>AbstractHandshaker</tt> with the specified headers 
     * for the specified connection.
     * 
     * @param conn the <tt>Connection</tt> to handshake for
     * @param requestHeaders the request headers to send to the other node
     * @param responseHeaders the class that stores any response headers
     */
    protected AbstractHandshaker(Connection conn, Properties requestHeaders, 
        HandshakeResponder responseHeaders) {
        CONNECTION = conn;
        REQUEST_HEADERS = requestHeaders;
        RESPONSE_HEADERS = responseHeaders;
    }

    /**
     * Returns the value of the given outgoing (written) connection header, or
     * null if no such header exists.  For example, getProperty("X-Supernode") 
     * tells whether I am a supernode or a leaf node.  If I wrote a property 
     * multiple time during the connection, this returns the latest.
     * 
     * @return the connection header value associated with the specified 
     *  connection header name, or <tt>null</tt> if the header does not exist
     */
    public String getHeaderWritten(String name) {
        return HEADERS_WRITTEN.getProperty(name);
    }

    // inherit doc comment
    public Properties getHeadersRead() {
        return HEADERS_READ;
    }

    // inherit doc comment
    public Properties getHeadersWritten() {
        return HEADERS_WRITTEN;
    }
    
    /**
     * Performs special handling for the remote IP header, setting our forced
     * ip address if the user has LimeWire set to use a forced ip.
     * 
     * @param header the HTTPHeader instane that may be the Remote-IP header
     */
    protected final void handleRemoteIP(HTTPHeader header)  {
        if (HeaderNames.REMOTE_IP.equals(header.getHeaderNameString()) && 
            ConnectionSettings.FORCE_IP_ADDRESS.getValue()) {
            try {
                ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue(
                    header.getHeaderValueString());
            } catch (IllegalArgumentException ex) {
                // ignore
            }
        }        
    }

}
