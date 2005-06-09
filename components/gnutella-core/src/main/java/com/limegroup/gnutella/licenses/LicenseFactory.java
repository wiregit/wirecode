package com.limegroup.gnutella.licenses;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.metadata.WeedInfo;
import com.limegroup.gnutella.metadata.WRMXML;

/**
 * A factory for constructing Licenses based on licenses.
 */
public final class LicenseFactory {
    
    private static final Log LOG = LogFactory.getLog(LicenseFactory.class);
    
    public static final String WEED_NAME = "Weed License";
    public static final String CC_NAME = "Creative Commons License";
    public static final String UNKNOWN_NAME = "Unknown License";
    
    private LicenseFactory() {}
    
    /**
     * Checks if the specified license-URI is valid for the given URN
     * without doing any expensive lookups.
     *
     * The URI must have been retrieved via getLicenseURI.
     *
     */
    public static boolean isVerifiedAndValid(URN urn, String licenseString) {
        URI uri = getLicenseURI(licenseString);
        return uri != null && LicenseCache.instance().isVerifiedAndValid(urn, uri);
    }
    
    /**
     * Gets the name associated with this license string.
     */
    public static String getLicenseName(String licenseString) {
        if(isCCLicense(licenseString))
            return CC_NAME;
        else if(isWeedLicense(licenseString))
            return WEED_NAME;
        else if(isUnknownLicense(licenseString))
            return UNKNOWN_NAME;
        else
            return null;
    }
    
    /**
     * Returns a License for the given license string, if one
     * can be constructed.  If no License exists to validate
     * the license, returns null.
     */
    public static License create(String licenseString) {
        if(licenseString == null)
            return null;
        
        if(LOG.isTraceEnabled())
            LOG.trace("Attempting to create license from: " + licenseString);
        
        License license = null;
        URI uri = getLicenseURI(licenseString);
        
        // Try to get a cached version, first.
        if(uri != null)
            license = LicenseCache.instance().getLicense(licenseString, uri);
        
        // If the cached version didn't exist, try to make one.
        if(license == null) {
            if(isCCLicense(licenseString)) {
                if(uri != null)
                    license = new CCLicense(licenseString, uri);
                else
                    license = new BadCCLicense(licenseString);
            } else if(isWeedLicense(licenseString) && uri != null) {
                license = new WeedLicense(uri);
            } else if(isUnknownLicense(licenseString)) {
                license = new UnknownLicense();
            }
        }
        
        // If we managed to get one, and it's a NamedLicense, try and set its name.
        if(license != null && license instanceof NamedLicense)
            ((NamedLicense)license).setLicenseName(getLicenseName(licenseString));

        return license;
    }
    
    /** Determines if the given string can be a CC license. */
    private static boolean isCCLicense(String s) {
        return s.indexOf(CCConstants.URL_INDICATOR) != -1;
    }
    
    /** Determines if the given string can be a Weed license. */
    private static boolean isWeedLicense(String s) {
        return s.startsWith(WeedInfo.LAINFO);
    }
    
    /** Determines if the given string can be an Unknown license. */
    private static boolean isUnknownLicense(String s) {
        return s.startsWith(WRMXML.PROTECTED);
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
            
        // Look for CC first.
        URI uri = getCCLicenseURI(license);
        
        // Then Weed.
        if(uri == null)
            uri = getWeedLicenseURI(license);
            
        // ADD MORE LICENSES IN THE FORM OF
        // if( uri == null)
        //      uri = getXXXLicenseURI(license)
        // AS WE UNDERSTAND MORE...
        
        return uri;
    }
        
    /** Gets a CC license URI from the given license string. */
    private static URI getCCLicenseURI(String license) {
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
    
    /** Gets a Weed license URI from the given license string. */
    private static URI getWeedLicenseURI(String license) {
        int lainfo = license.indexOf(WeedInfo.LAINFO);
        if(lainfo == -1)
            return null;
            
        int cidx = license.indexOf(WeedInfo.CID);
        int vidx = license.indexOf(WeedInfo.VID);
        
        // If no cid or vid, exit.
        if(cidx == -1 || vidx == -1) {
            LOG.debug("No cid or vid, bailing.");
            return null;
        }
            
        cidx += WeedInfo.CID.length();;
        vidx += WeedInfo.VID.length();;
            
        int cend = license.indexOf(" ", cidx);
        int vend = license.indexOf(" ", vidx);
        // If there's no ending space for BOTH, exit.
        // (it's okay if one is at the end, but both can't be)
        if(cend == -1 && vend == -1) {
            LOG.debug("No endings for both cid & vid, bailing");
            return null;
        }
        if(cend == -1)
            cend = license.length();
        if(vend == -1)
            vend = license.length();
        
        // If the cid or vid are empty, exit.
        String cid = license.substring(cidx, cend).trim();
        String vid = license.substring(vidx, vend).trim();
        if(cid.length() == 0 || vid.length() == 0) {
            LOG.debug("cid or vid is empty, bailing");
            return null;
        }
        
        if(cid.startsWith(WeedInfo.VID.trim()) || vid.startsWith(WeedInfo.CID.trim())) {
            LOG.debug("cid starts with vid, or vice versa, bailing.");
            return null;
        }
        
        return WeedLicense.buildURI(cid, vid);
    }
}
       