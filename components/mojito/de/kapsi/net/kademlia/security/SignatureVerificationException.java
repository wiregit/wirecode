/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.security;

import java.security.SignatureException;

public class SignatureVerificationException extends SignatureException {

    public SignatureVerificationException() {
        super();
    }
    
    public SignatureVerificationException(String message) {
        super(message);
    }
}
