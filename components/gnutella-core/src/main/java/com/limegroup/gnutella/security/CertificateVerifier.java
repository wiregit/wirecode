package com.limegroup.gnutella.security;

import java.security.SignatureException;

public interface CertificateVerifier {

    Certificate verify(Certificate certificate) throws SignatureException;
    
}
