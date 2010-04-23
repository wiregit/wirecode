package com.limegroup.gnutella.simpp;

import java.security.SignatureException;

public interface SimppDataVerifier {

    /**
     * Extracts the signed data from the message and verifies it against 
     * the signature in the data. 
     * @return the verified signed data
     * @throws SignatureException if the data does not verify against the 
     * signature
     */
    byte[] extractSignedData(byte[] simppPayload) throws SignatureException;

}