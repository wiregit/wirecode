package com.limegroup.gnutella.http;

import java.io.IOException;

/**
 * Class that stores the name and the value of a single HTTP header.
 * 
 * TODO: add support for HTTPHeaderName and HTTPHeaderValue classes
 */
public final class HTTPHeader {

    /**
     * The header name.
     */
    private final String NAME;
    
    /**
     * The header value;
     */
    private final String VALUE;

    /**
     * Creates a new <tt>HTTPHeader</tt> instance by extracting the header name
     * and the header value from the specified header string.
     * 
     * @param header the string containing the http header
     * @return a new <tt>HTTPHeader</tt> with the name and the value extracted
     *  from the string parameter
     * @throws IOException if there if an IO error reading the expected values
     *  from the headers string
     */
    public static HTTPHeader createHeader(String header) throws IOException  {
        return new HTTPHeader(header);
    }
    
    /**
     * Creates a new <tt>HTTPHeader</tt> by extracting the name and value from
     * the specified string.
     * 
     * @param header the string containing the header name and value
     * @throws IOException if the supplied string does not match the expected
     *  HTTP header syntax
     */
    private HTTPHeader(String header) throws IOException  {
        if (header == null)  {
            throw new NullPointerException("null header");
        }            
        int i = header.indexOf(':');
        if (i<0)  {
            throw new IOException("could not find colon");
        }
        
        NAME = header.substring(0, i);
        VALUE = header.substring(i+1).trim();   
    }
    
    /**
     * Accessor for the name of this HTTP header.
     * 
     * @return the name of this HTTP header as a string
     */
    public String getHeaderNameString()  {
        return NAME;
    }
    
    /**
     * Accessor for the value of this HTTP header
     * @return the value of this HTTP header as a string
     */
    public String getHeaderValueString()  {
        return VALUE;
    }
    
    //overridded to provide extra information about this header
    public String toString()  {
        return NAME + ": " + VALUE;
    }
}
