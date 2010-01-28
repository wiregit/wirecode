/**
 * 
 */
package com.limegroup.gnutella.security;

import java.security.PublicKey;

import com.limegroup.gnutella.util.DataUtils;

public class NullCertificate implements Certificate {
    
    @Override
    public String getCertificateString() {
        return null;
    }
    
    @Override
    public int getKeyVersion() {
        return -1;
    }
    
    @Override
    public PublicKey getPublicKey() {
        return new PublicKey() {
            @Override
            public String getFormat() {
                return "";
            }
            @Override
            public byte[] getEncoded() {
                return DataUtils.EMPTY_BYTE_ARRAY;
            }
            @Override
            public String getAlgorithm() {
                return "DSA";
            }
        };
    }
    
    @Override
    public byte[] getSignature() {
        return DataUtils.EMPTY_BYTE_ARRAY;
    }
    
    @Override
    public byte[] getSignedPayload() {
        return DataUtils.EMPTY_BYTE_ARRAY;
    }
}