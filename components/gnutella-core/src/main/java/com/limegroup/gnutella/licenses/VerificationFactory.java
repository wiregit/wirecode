package com.limegroup.gnutella.licenses;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A factory for constructing verifiers based on licenses.
 */
public final class VerificationFactory {
    
    private static final Log LOG = LogFactory.getLog(VerificationFactory.class);
    
    private VerificationFactory() {}
    
    /**
     * Returns a Verifier for the given license string, if one
     * can be constructed.  If no Verifier exists to validate
     * the license, returns null.
     */
    public static Verifier create(String license) {
        if(license == null)
            return null;
        
        /// ONLY ATTEMPTS Creative Commons VERIFICATION RIGHT NOW.
        
        // find where the URL should begin.
        int verifyAt = license.indexOf(CCConstants.URL_INDICATOR);
        if(verifyAt == -1)
            return null;
            
        int urlStart = verifyAt + CCConstants.URL_INDICATOR.length();
        if(urlStart >= license.length())
            return null;
            
        String url = license.substring(urlStart).trim();
        URI uri;
        try {
            uri = new URI(url.toCharArray());
        } catch(URIException e) {
            LOG.error("Unable to create URI", e);
            return null;
        }
        
        return new CCVerifier(license, uri);
    }
}
       