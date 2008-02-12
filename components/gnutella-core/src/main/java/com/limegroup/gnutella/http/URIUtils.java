package com.limegroup.gnutella.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.io.UnsupportedEncodingException;

import org.limewire.http.Constants;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.settings.BugSettings;

/**
 * Utilities for URIs
 */
public class URIUtils {
    
    private static final String RESERVED = ";/?:@&=+$,";

    /**
     * a temporary method to allow the tracking of URI's
     * that cannot be constructed via java.net.URI.
     * 
     * Sends feedback via the ErrorService.
     * 
     * @param e
     */
    public static void error(URISyntaxException e) {
        if (BugSettings.SEND_URI_BUGS.getValue())
            ErrorService.error(e);
    }

    /**
     * Creates a <code>URI</code> from the input string.
     * The preferred way to invoke this method is with an URL-encoded string.
     * 
     * However, if the string has not been encoded, this method will encode it.
     * It is ambiguous whether a string has been encoded or not, which is why
     * it is preferred to pass in the string pre-encoded.
     * 
     * This method is useful when manipulating a URI and you don't know if it is 
     * encoded or not.
     * 
     * @param uriString the uri to be created
     * @return
     * @throws URISyntaxException
     */
    public static URI toURI(final String uriString) throws URISyntaxException {
        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            // the uriString was perhaps not encoded.
            // try to encode it.
            String encodedURIString = encodeURI(uriString);
            try {
                uri = new URI(encodedURIString);
            } catch (URISyntaxException e1) {
                // encoding the uriString didn't help.
                // this probably means there is something structuraly
                // wrong with it.
                
                // NOTE: throwing the original exception.
                // initing with second Exception.  Not the normal
                // use case for initCause(), but this will at least capture both 
                // stack traces
                if(e.getCause() == null) {
                    e.initCause(e1);
                }
                throw e;
            }
        }
        return uri;
    }

    private static String encodeURI(String url) {
        StringBuilder encodedURL = new StringBuilder();
        StringTokenizer st = new StringTokenizer(url, RESERVED, true);
        while(st.hasMoreElements()) {
            String s = st.nextToken();
            if(isDelimiter(s)) {
                encodedURL.append(s);
            } else {
                try {
                    encodedURL.append(URLEncoder.encode(s, Constants.ASCII_ENCODING));
                } catch (UnsupportedEncodingException e1) {
                    // should never happen
                    ErrorService.error(e1);
                }
            }
        }
        return encodedURL.toString();
    }

    private static boolean isDelimiter(String s) {
        return RESERVED.contains(s);
    }
}
