package org.limewire.security;

import java.security.PublicKey;

public interface SecureMessageVerifier {

    /** Queues this SecureMessage for verification.  The callback will be notified of success or failure. */
    public void verify(SecureMessage sm, SecureMessageCallback smc);

    /** 
     * Queues this SecureMessage for verification. The callback will 
     * be notified of success or failure.
     */
    public void verify(PublicKey pubKey, String algorithm, SecureMessage sm,
            SecureMessageCallback smc);

    /**
     * Enqueues a custom Verifier
     */
    public void verify(Verifier verifier);

}