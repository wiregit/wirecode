package com.limegroup.gnutella.licenses;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A factory for constructing Licenses based on licenses.
 */
public final class LicenseFactory {
    
    private static final Log LOG = LogFactory.getLog(LicenseFactory.class);
    
    private LicenseFactory() {}
    
    /**
     * Returns a License for the given license string, if one
     * can be constructed.  If no License exists to validate
     * the license, returns null.
     */
    public static License create(String licenseString) {
        if(licenseString == null)
            return null;
        
        URI licenseURI = getLicenseURI(licenseString);
        // no verification location?  no license.
        if(licenseURI == null)
            return null;
        
        // See if we have a cached license before we create a new one.
        License license = LicenseCache.instance().getLicense(licenseString, licenseURI);
        // no cached? create a new one.
        if(license == null)
            license = new CCLicense(licenseString, licenseURI);
        
        return license;
    }
    
    /**
     * Determines the URI to verify this license at from the license string.
     */
    private static URI getLicenseURI(String license) {
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
        
        return null;
    }
}
       