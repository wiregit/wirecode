package com.limegroup.gnutella.licenses;

import java.net.URL;

import com.limegroup.gnutella.URN;

/**
 * Contains methods related to verification.
 */
public interface Verifier {
    
    static final int NOTHING   = 0;
    static final int VERIFYING = 1;
    static final int VERIFIED  = 2;
    static final int VERIFY_FAILED = 3;
    
    /**
     * True if this license has been verified succesfully.
     */
    public boolean isVerified();
    
    /**
     * True if this license is in the process of being verified.
     */
    public boolean isVerifying();
    
    /**
     * True if verification has been attempted (succesful or not) on this license.
     */
    public boolean isVerificationDone();
    
    /**
     * Returns a description of this license.
     */
    public String getVerifiedDescription();
    
    /**
     * Returns a URL that the user can visit to manually verify.
     */
    public URL getURL();
    
    /**
     * Returns the license URL.
     */
    public URL getLicenseURL();
    
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
    public void verify(VerificationCallback listener);
}