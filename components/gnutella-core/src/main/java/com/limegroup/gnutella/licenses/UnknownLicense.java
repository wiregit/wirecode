package com.limegroup.gnutella.licenses;

import java.net.URL;

import org.apache.commons.httpclient.URI;

import com.limegroup.gnutella.URN;

/**
 * An unknown license (unverifiable).
 */
public class UnknownLicense implements NamedLicense {
    private String name;
    
    /** Sets the license name. */
    public void setLicenseName(String name) { this.name = name; }
    
    public boolean isVerified() { return false; }
    public boolean isVerifying() { return false; }
    public boolean isValid(URN urn) { return false; }
    public String getLicenseDescription(URN urn) { return null; }
    public URI getLicenseURI() { return null; }
    public URL getLicenseDeed(URN urn) { return null; }
    public String getLicense() { return null; }
    public void verify(VerificationListener listener) {}
    public long getLastVerifiedTime() { return 0; }
    public String getLicenseName() { return name; }
    
    public License copy(String license, URI licenseURI) {
        throw new UnsupportedOperationException("no copying");
    }    
}