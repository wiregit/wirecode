package com.limegroup.gnutella.licenses;

import java.net.URL;
import java.net.MalformedURLException;
import org.apache.commons.httpclient.URI;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.ErrorService;

/**
 * A bad license (unverifiable).
 */
public class BadLicense implements License {
    
    private String license;
    
    public BadLicense(String license) {
        this.license = license;
    }
    
    /**
     * Attempts to guess what the license URI is from the license text.
     */    
    private URL guessLicenseDeed() {
        // find where "creativecommons.org/licenses/" is.
        int idx = license.indexOf(CCConstants.CC_URI_PREFIX);
        if(idx == -1)
            return null;
        // find the "http://" before it.
        int httpIdx = license.lastIndexOf("http://", idx);
        if(httpIdx == -1)
            return null;
        // find where the first space is after the http://.
        // if it's before the creativecommons.org part, that's bad.
        int spaceIdx = license.indexOf(" ", httpIdx);
        if(spaceIdx < idx)
            return null;
     
        try {       
            return new URL(license.substring(httpIdx, spaceIdx));
        } catch(MalformedURLException bad) {
            return null;
        }
    }    
    
    public boolean isVerified() { return true; }
    public boolean isVerifying() { return false; }
    public boolean isValid(URN urn) { return false; }
    public String getLicenseDescription() { return license;  }
    public URI getLicenseURI() { return null; }
    public URL getLicenseDeed() { return guessLicenseDeed(); }
    public URN getExpectedURN() { return null; }
    public String getLicense() { return license; }
    public void verify(VerificationListener listener) {}
    public long getLastVerifiedTime() { return 0; }
    
    public License copy(String license) {
        BadLicense newL = null;
        try {
            newL = (BadLicense)clone();
            newL.license = license;
        } catch(CloneNotSupportedException error) {
            ErrorService.error(error);
        }
        return newL;
    }    
}