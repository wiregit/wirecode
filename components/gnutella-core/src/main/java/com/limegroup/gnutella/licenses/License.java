package com.limegroup.gnutella.licenses;

import java.net.URL;
import org.apache.commons.httpclient.URI;

import com.limegroup.gnutella.URN;

/**
 * Contains methods related to verification.
 */
public interface License {
    
    static final int UNVERIFIED = 0;
    static final int VERIFYING = 1;
    static final int VERIFIED = 2;
    
    /**
     * True if this license has been externally verified.
     *
     * This does NOT indicate whether or not the license was valid.
     */
    public boolean isVerified();
    
    /**
     * True if this license is currently being or in queue for verification.
     */
    public boolean isVerifying();
    
    /**
     * True if this license was verified and is valid & matches the given URN.
     */
    public boolean isValid(URN urn);
    
    /**
     * Returns a description of this license.
     */
    public String getLicenseDescription();
    
    /**
     * Returns a URI that the user can visit to manually verify.
     */
    public URI getLicenseURI();
    
    /**
     * Returns the location of the deed for this license.
     */
    public URL getLicenseDeed();
    
    /**
     * Retrieves the URN this verification should have, if any.
     */
    public URN getExpectedURN();
    
    /**
     * Returns the license, in human readable form.
     */
    public String getLicense();
    
    /**
     * Starts verification of the license.
     *
     * The listener is notified when verification is finished.
     */
    public void verify(VerificationListener listener);
    
    /**
     * Returns the last time this license was verified.
     */
    public long getLastVerifiedTime();
    
    /**
     * Returns a copy of this license with a new 'license' string.
     */
    public License copy(String license);
}