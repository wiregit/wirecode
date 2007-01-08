package com.limegroup.gnutella.bootstrap;

import java.text.ParseException;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.HostCatcher;

/**
 * A URL for a GWebCache endpoint, plus some additional connection
 * history data:
 * 
 * <ul>
 * <li>The time this was discovered
 * <li>The times we were able to connect to this
 * <li>The times we were unable to connect to this
 * </ul>
 * 
 * Written to and read from gnutella.net.
 *
 * @see GWebCache
 * @see HostCatcher
 */
public class BootstrapServer {
    //TODO: factor code with ExtendedEndpoint?

    /** The URL to server's script, e.g., "http://path/to/script.php". */        
    private final URI _url;

    /** 
     * Constructs a new BootstrapServer from a URL or an extended
     * gnutella.net data line. 
     * 
     * @param s single line of the form "http://server.com/path/to/script" or
     *  or "http://server.com/path/to/script,dtime,ctimes,ftimes".  In the
     *  extended format, "dtime" is the host discovery time, ctimes is a
     *  semicolon separated list of successful connect times, and ftimes is
     *  semicolon separated list of unsuccessful connect times.
     * @exception ParseException line could not be be parsed in 
     *  either format.  The offset is not necessarily set.
     */
    public BootstrapServer(String s) throws ParseException {
        if (!StringUtils.startsWithIgnoreCase(s, "http"))
            throw new ParseException(s, 0);
            
        try {
            int i=s.indexOf(",");        //TODO: relies on s being URL encoded
            if (i<0)  //simple url
                _url = new URI(s.toCharArray());
            else  //extended gnutella.net
                _url = new URI(s.substring(0,i).toCharArray());
        } catch (URIException e) {
            throw new ParseException(s, 0);
        }
    }

    /** 
	 * Returns the URL to the server, minus any request parameters. This is
	 * guaranteed to be non-null.
	 */
    public String getURLString() {
        return _url.toString();
    }

    /** 
     * Returns a parsable represenation of this.  This can be reconstructed by
     * constructing a new BootstrapServer with the returned string as an
     * argument.  Does not include any end-of-line characters.
     */
    public String toString() {
        return _url.toString();
    }
 
    /**
     * Creates an integer suitable for hash table indexing.<p>
     * The hash code is based upon all the URL components relevant for URL
     * comparison. As such, this operation is a blocking operation.
     */
    public int hashCode() {
        return _url.hashCode();
    }

    /** 
     * Returns true if o is a BootStrapServer with the same URL.
     */
    public boolean equals(Object o) {
        if(o == this)
            return true;
        if (o instanceof BootstrapServer)
            return this._url.equals(((BootstrapServer)o)._url);
        return false;
    }
}
