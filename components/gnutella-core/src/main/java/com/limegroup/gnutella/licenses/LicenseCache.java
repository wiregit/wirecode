package com.limegroup.gnutella.licenses;

import java.util.Map;
import java.util.HashMap;

import org.apache.commons.httpclient.URI;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A repository of licenses.
 */
class LicenseCache {
    
    private static final Log LOG = LogFactory.getLog(LicenseCache.class);
    
    private static final LicenseCache INSTANCE = new LicenseCache();
    private LicenseCache() {}
    public static LicenseCache instance() { return INSTANCE; }
    
    private final Map /* String (URL) -> License */ licenses = new HashMap();
    
    public synchronized void addVerifiedLicense(License license) {
        String url = license.getLicenseURI().toString();
        licenses.put(url, license);
    }
    
    public synchronized License getLicense(String licenseString, URI licenseURI) {
        License license = (License)licenses.get(licenseURI.toString());
        if(license != null)
            return license.copy(licenseString);
        else
             return null;
    }
}