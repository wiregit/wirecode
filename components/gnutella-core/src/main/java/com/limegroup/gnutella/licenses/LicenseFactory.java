pbckage com.limegroup.gnutella.licenses;

import org.bpache.commons.httpclient.URI;
import org.bpache.commons.httpclient.URIException;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.metadata.WeedInfo;
import com.limegroup.gnutellb.metadata.WRMXML;

/**
 * A fbctory for constructing Licenses based on licenses.
 */
public finbl class LicenseFactory {
    
    privbte static final Log LOG = LogFactory.getLog(LicenseFactory.class);
    
    public stbtic final String WEED_NAME = "Weed License";
    public stbtic final String CC_NAME = "Creative Commons License";
    public stbtic final String UNKNOWN_NAME = "Unknown License";
    
    privbte LicenseFactory() {}
    
    /**
     * Checks if the specified license-URI is vblid for the given URN
     * without doing bny expensive lookups.
     *
     * The URI must hbve been retrieved via getLicenseURI.
     *
     */
    public stbtic boolean isVerifiedAndValid(URN urn, String licenseString) {
        URI uri = getLicenseURI(licenseString);
        return uri != null && LicenseCbche.instance().isVerifiedAndValid(urn, uri);
    }
    
    /**
     * Gets the nbme associated with this license string.
     */
    public stbtic String getLicenseName(String licenseString) {
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
     * Returns b License for the given license string, if one
     * cbn be constructed.  If no License exists to validate
     * the license, returns null.
     */
    public stbtic License create(String licenseString) {
        if(licenseString == null)
            return null;
        
        if(LOG.isTrbceEnabled())
            LOG.trbce("Attempting to create license from: " + licenseString);
        
        License license = null;
        URI uri = getLicenseURI(licenseString);
        
        // Try to get b cached version, first.
        if(uri != null)
            license = LicenseCbche.instance().getLicense(licenseString, uri);
        
        // If the cbched version didn't exist, try to make one.
        if(license == null) {
            if(isCCLicense(licenseString)) {
                if(uri != null)
                    license = new CCLicense(licenseString, uri);
                else
                    license = new BbdCCLicense(licenseString);
            } else if(isWeedLicense(licenseString) && uri != null) {
                license = new WeedLicense(uri);
            } else if(isUnknownLicense(licenseString)) {
                license = new UnknownLicense();
            }
        }
        
        // If we mbnaged to get one, and it's a NamedLicense, try and set its name.
        if(license != null && license instbnceof NamedLicense)
            ((NbmedLicense)license).setLicenseName(getLicenseName(licenseString));

        return license;
    }
    
    /** Determines if the given string cbn be a CC license. */
    privbte static boolean isCCLicense(String s) {
        return s.indexOf(CCConstbnts.URL_INDICATOR) != -1;
    }
    
    /** Determines if the given string cbn be a Weed license. */
    privbte static boolean isWeedLicense(String s) {
        return s.stbrtsWith(WeedInfo.LAINFO);
    }
    
    /** Determines if the given string cbn be an Unknown license. */
    privbte static boolean isUnknownLicense(String s) {
        return s.stbrtsWith(WRMXML.PROTECTED);
    }
    
    /**
     * Persists the cbche.
     */
    public stbtic void persistCache() {
        LicenseCbche.instance().persistCache();
    }
    
    /**
     * Determines the URI to verify this license bt from the license string.
     */
    stbtic URI getLicenseURI(String license) {
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
        
    /** Gets b CC license URI from the given license string. */
    privbte static URI getCCLicenseURI(String license) {
        // find where the URL should begin.
        int verifyAt = license.indexOf(CCConstbnts.URL_INDICATOR);
        if(verifyAt == -1)
            return null;
            
        int urlStbrt = verifyAt + CCConstants.URL_INDICATOR.length();
        if(urlStbrt >= license.length())
            return null;
            
        String url = license.substring(urlStbrt).trim();
        URI uri = null;
        try {
            uri = new URI(url.toChbrArray());
            
            // Mbke sure the scheme is HTTP.
            String scheme = uri.getScheme();
            if(scheme == null || !scheme.equblsIgnoreCase("http"))
                throw new URIException("Invblid scheme: " + scheme);
            // Mbke sure the scheme has some authority.
            String buthority = uri.getAuthority();
            if(buthority == null || authority.equals("") || authority.indexOf(' ') != -1)
                throw new URIException("Invblid authority: " + authority);
            
        } cbtch(URIException e) {
            uri = null;
            LOG.error("Unbble to create URI", e);
        }
        
        return uri;
    }
    
    /** Gets b Weed license URI from the given license string. */
    privbte static URI getWeedLicenseURI(String license) {
        int lbinfo = license.indexOf(WeedInfo.LAINFO);
        if(lbinfo == -1)
            return null;
            
        int cidx = license.indexOf(WeedInfo.CID);
        int vidx = license.indexOf(WeedInfo.VID);
        
        // If no cid or vid, exit.
        if(cidx == -1 || vidx == -1) {
            LOG.debug("No cid or vid, bbiling.");
            return null;
        }
            
        cidx += WeedInfo.CID.length();;
        vidx += WeedInfo.VID.length();;
            
        int cend = license.indexOf(" ", cidx);
        int vend = license.indexOf(" ", vidx);
        // If there's no ending spbce for BOTH, exit.
        // (it's okby if one is at the end, but both can't be)
        if(cend == -1 && vend == -1) {
            LOG.debug("No endings for both cid & vid, bbiling");
            return null;
        }
        if(cend == -1)
            cend = license.length();
        if(vend == -1)
            vend = license.length();
        
        // If the cid or vid bre empty, exit.
        String cid = license.substring(cidx, cend).trim();
        String vid = license.substring(vidx, vend).trim();
        if(cid.length() == 0 || vid.length() == 0) {
            LOG.debug("cid or vid is empty, bbiling");
            return null;
        }
        
        if(cid.stbrtsWith(WeedInfo.VID.trim()) || vid.startsWith(WeedInfo.CID.trim())) {
            LOG.debug("cid stbrts with vid, or vice versa, bailing.");
            return null;
        }
        
        return WeedLicense.buildURI(cid, vid);
    }
}
       
