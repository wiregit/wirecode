package com.limegroup.gnutella.licenses;

import java.util.Map;
import java.util.HashMap;

import org.apache.commons.httpclient.URI;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import com.limegroup.gnutella.URN;

/**
 * A repository of licenses.
 */
class LicenseCache {
    
    private static final Log LOG = LogFactory.getLog(LicenseCache.class);
    
    private static final LicenseCache INSTANCE = new LicenseCache();
    private LicenseCache() {}
    public static LicenseCache instance() { return INSTANCE; }
    
    private final Map /* String (URL) -> License */ licenses = new HashMap();
    
    /**
     * Adds a verified license.
     */
    synchronized void addVerifiedLicense(License license) {
        String url = license.getLicenseURI().toString();
        licenses.put(url, license);
    }
    
    /**
     * Retrieves the cached license for the specified URI, substituting
     * the license string for a new one.
     */
    synchronized License getLicense(String licenseString, URI licenseURI) {
        License license = (License)licenses.get(licenseURI.toString());
        if(license != null)
            return license.copy(licenseString);
        else
             return null;
    }
    
    /**
     * Determines if the license is verified for the given URN and URI.
     */
    synchronized boolean isLicensed(URN urn, URI uri) {
        License license = (License)licenses.get(uri.toString());
        if(license != null) {
            if(!license.isValid())
                return false;
            URN expect = license.getExpectedURN();
            if(expect != null)
                return expect.equals(urn);
            else // cannot do URN match if no expected URN.
                return true;
        } else {
            return false; // unverified.
        }
    }
}