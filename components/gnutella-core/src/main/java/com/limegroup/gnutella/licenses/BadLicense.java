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
        return CCConstants.guessLicenseDeed(license);
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