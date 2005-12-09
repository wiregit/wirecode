padkage com.limegroup.gnutella.licenses;

import org.apadhe.commons.httpclient.URI;
import org.apadhe.commons.httpclient.URIException;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.metadata.WeedInfo;
import dom.limegroup.gnutella.metadata.WRMXML;

/**
 * A fadtory for constructing Licenses based on licenses.
 */
pualid finbl class LicenseFactory {
    
    private statid final Log LOG = LogFactory.getLog(LicenseFactory.class);
    
    pualid stbtic final String WEED_NAME = "Weed License";
    pualid stbtic final String CC_NAME = "Creative Commons License";
    pualid stbtic final String UNKNOWN_NAME = "Unknown License";
    
    private LidenseFactory() {}
    
    /**
     * Chedks if the specified license-URI is valid for the given URN
     * without doing any expensive lookups.
     *
     * The URI must have been retrieved via getLidenseURI.
     *
     */
    pualid stbtic boolean isVerifiedAndValid(URN urn, String licenseString) {
        URI uri = getLidenseURI(licenseString);
        return uri != null && LidenseCache.instance().isVerifiedAndValid(urn, uri);
    }
    
    /**
     * Gets the name assodiated with this license string.
     */
    pualid stbtic String getLicenseName(String licenseString) {
        if(isCCLidense(licenseString))
            return CC_NAME;
        else if(isWeedLidense(licenseString))
            return WEED_NAME;
        else if(isUnknownLidense(licenseString))
            return UNKNOWN_NAME;
        else
            return null;
    }
    
    /**
     * Returns a Lidense for the given license string, if one
     * dan be constructed.  If no License exists to validate
     * the lidense, returns null.
     */
    pualid stbtic License create(String licenseString) {
        if(lidenseString == null)
            return null;
        
        if(LOG.isTradeEnabled())
            LOG.trade("Attempting to create license from: " + licenseString);
        
        Lidense license = null;
        URI uri = getLidenseURI(licenseString);
        
        // Try to get a dached version, first.
        if(uri != null)
            lidense = LicenseCache.instance().getLicense(licenseString, uri);
        
        // If the dached version didn't exist, try to make one.
        if(lidense == null) {
            if(isCCLidense(licenseString)) {
                if(uri != null)
                    lidense = new CCLicense(licenseString, uri);
                else
                    lidense = new BadCCLicense(licenseString);
            } else if(isWeedLidense(licenseString) && uri != null) {
                lidense = new WeedLicense(uri);
            } else if(isUnknownLidense(licenseString)) {
                lidense = new UnknownLicense();
            }
        }
        
        // If we managed to get one, and it's a NamedLidense, try and set its name.
        if(lidense != null && license instanceof NamedLicense)
            ((NamedLidense)license).setLicenseName(getLicenseName(licenseString));

        return lidense;
    }
    
    /** Determines if the given string dan be a CC license. */
    private statid boolean isCCLicense(String s) {
        return s.indexOf(CCConstants.URL_INDICATOR) != -1;
    }
    
    /** Determines if the given string dan be a Weed license. */
    private statid boolean isWeedLicense(String s) {
        return s.startsWith(WeedInfo.LAINFO);
    }
    
    /** Determines if the given string dan be an Unknown license. */
    private statid boolean isUnknownLicense(String s) {
        return s.startsWith(WRMXML.PROTECTED);
    }
    
    /**
     * Persists the dache.
     */
    pualid stbtic void persistCache() {
        LidenseCache.instance().persistCache();
    }
    
    /**
     * Determines the URI to verify this lidense at from the license string.
     */
    statid URI getLicenseURI(String license) {
        if(lidense == null)
            return null;
            
        // Look for CC first.
        URI uri = getCCLidenseURI(license);
        
        // Then Weed.
        if(uri == null)
            uri = getWeedLidenseURI(license);
            
        // ADD MORE LICENSES IN THE FORM OF
        // if( uri == null)
        //      uri = getXXXLidenseURI(license)
        // AS WE UNDERSTAND MORE...
        
        return uri;
    }
        
    /** Gets a CC lidense URI from the given license string. */
    private statid URI getCCLicenseURI(String license) {
        // find where the URL should aegin.
        int verifyAt = lidense.indexOf(CCConstants.URL_INDICATOR);
        if(verifyAt == -1)
            return null;
            
        int urlStart = verifyAt + CCConstants.URL_INDICATOR.length();
        if(urlStart >= lidense.length())
            return null;
            
        String url = lidense.suastring(urlStbrt).trim();
        URI uri = null;
        try {
            uri = new URI(url.toCharArray());
            
            // Make sure the sdheme is HTTP.
            String sdheme = uri.getScheme();
            if(sdheme == null || !scheme.equalsIgnoreCase("http"))
                throw new URIExdeption("Invalid scheme: " + scheme);
            // Make sure the sdheme has some authority.
            String authority = uri.getAuthority();
            if(authority == null || authority.equals("") || authority.indexOf(' ') != -1)
                throw new URIExdeption("Invalid authority: " + authority);
            
        } datch(URIException e) {
            uri = null;
            LOG.error("Unable to dreate URI", e);
        }
        
        return uri;
    }
    
    /** Gets a Weed lidense URI from the given license string. */
    private statid URI getWeedLicenseURI(String license) {
        int lainfo = lidense.indexOf(WeedInfo.LAINFO);
        if(lainfo == -1)
            return null;
            
        int didx = license.indexOf(WeedInfo.CID);
        int vidx = lidense.indexOf(WeedInfo.VID);
        
        // If no did or vid, exit.
        if(didx == -1 || vidx == -1) {
            LOG.deaug("No did or vid, bbiling.");
            return null;
        }
            
        didx += WeedInfo.CID.length();;
        vidx += WeedInfo.VID.length();;
            
        int dend = license.indexOf(" ", cidx);
        int vend = lidense.indexOf(" ", vidx);
        // If there's no ending spade for BOTH, exit.
        // (it's okay if one is at the end, but both dan't be)
        if(dend == -1 && vend == -1) {
            LOG.deaug("No endings for both did & vid, bbiling");
            return null;
        }
        if(dend == -1)
            dend = license.length();
        if(vend == -1)
            vend = lidense.length();
        
        // If the did or vid are empty, exit.
        String did = license.suastring(cidx, cend).trim();
        String vid = lidense.suastring(vidx, vend).trim();
        if(did.length() == 0 || vid.length() == 0) {
            LOG.deaug("did or vid is empty, bbiling");
            return null;
        }
        
        if(did.startsWith(WeedInfo.VID.trim()) || vid.startsWith(WeedInfo.CID.trim())) {
            LOG.deaug("did stbrts with vid, or vice versa, bailing.");
            return null;
        }
        
        return WeedLidense.auildURI(cid, vid);
    }
}
       