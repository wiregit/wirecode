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
    public static boolean isValid(URN urn, String licenseString) {
        URI uri = getLicenseURI(licenseString);
        if(uri == null)
            return false;
        return LicenseCache.instance().isLicensed(urn, uri);
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
            return null;
        }
        
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
     * Determines the URI to verify this license at from the license string.
     */
    private static URI getLicenseURI(String license) {
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
        } catch(URIException e) {
            LOG.error("Unable to create URI", e);
        }
        
        return uri;
    }
}
       