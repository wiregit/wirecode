package com.limegroup.gnutella.licenses;

/**
 * A callback for verifying licenses.
 */
public interface VerificationCallback {
    
    public void verificationCompleted(Verifier verifier);
    
}