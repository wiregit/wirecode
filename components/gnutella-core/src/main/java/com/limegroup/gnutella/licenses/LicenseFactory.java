package com.limegroup.gnutella.licenses;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import com.limegroup.gnutella.URN;

/**
 * A factory for constructing Licenses based on licenses.
 */
public final class LicenseFactory {
    
    private static final Log LOG = LogFactory.getLog(LicenseFactory.class);
    
    private LicenseFactory() {}
    
    /**
     * Checks if the specified license-URI is valid for the given URN
     * without doing any expensive lookups.
     */
    public static boolean isVerifiedAndValid(URN urn, String licenseString) {
        URI uri = getLicenseURI(licenseString);
        return uri != null && LicenseCache.instance().isVerifiedAndValid(urn, uri);
    }    
    
    /**
     * Returns a License for the given license string, if one
     * can be constructed.  If no License exists to validate
     * the license, returns null.
     */
    public static License create(String licenseString) {
        if(LOG.isTraceEnabled())
            LOG.trace("Attempting to create license from: " + licenseString);
        
        URI licenseURI = getLicenseURI(licenseString);
        // no verification location?  no license.
        if(licenseURI == null) {
            LOG.warn("Unable to locate licenseURI, bailing");
            if(licenseString != null)
                return new BadLicense(licenseString);
            else
                return null;
        } else if(LOG.isDebugEnabled())
            LOG.debug("Creating license from URI: " + licenseURI);
        
        // See if we have a cached license before we create a new one.
        License license = LicenseCache.instance().getLicense(licenseString, licenseURI);
        // no cached? create a new one.
        if(license == null) {
            LOG.debug("No cached license, creating new.");
            license = new CCLicense(licenseString, licenseURI);
        }

        return license;
    }
    
    /**
     * Persists the cache.
     */
    public static void persistCache() {
        LicenseCache.instance().persistCache();
    }
    
    /**
     * Determines the URI to verify this license at from the license string.
     */
    static URI getLicenseURI(String license) {
        if(license == null)
            return null;
        
        // find where the URL should begin.
        int verifyAt = license.indexOf(CCConstants.URL_INDICATOR);
        if(verifyAt == -1)
            return null;
            
        int urlStart = verifyAt + CCConstants.URL_INDICATOR.length();
        if(urlStart >= license.length())
            return null;
            
        String url = license.substring(urlStart).trim();
        URI uri = null;
        try {
            uri = new URI(url.toCharArray());
            
            // Make sure the scheme is HTTP.
            String scheme = uri.getScheme();
            if(scheme == null || !scheme.equalsIgnoreCase("http"))
                throw new URIException("Invalid scheme: " + scheme);
            // Make sure the scheme has some authority.
            String authority = uri.getAuthority();
            if(authority == null || authority.equals("") || authority.indexOf(' ') != -1)
                throw new URIException("Invalid authority: " + authority);
            
        } catch(URIException e) {
            uri = null;
            LOG.error("Unable to create URI", e);
        }
        
        return uri;
    }
}
       