package com.limegroup.gnutella.bootstrap;

import java.net.*;
import java.text.ParseException;
import com.limegroup.gnutella.util.StringUtils;

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
    private final URL _url;

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
            if (i<0)
                _url=new URL(s);                  //simple url
            else
                _url=new URL(s.substring(0,i));   //extended gnutella.net
        } catch (MalformedURLException e) {
            throw new ParseException(s, 0);
        }
    }

    /** 
	 * Returns the URL to the server, minus any request parameters. This is
	 * guaranteed to be non-null.
	 */
    public URL getURL() {
        return _url;
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
     * Returns true if o is a BootStrapServer with the same URL.
     */
    public boolean equals(Object o) {
        if (! (o instanceof BootstrapServer))
            return false;
        return o.toString().equals(this.toString());
    }
}
