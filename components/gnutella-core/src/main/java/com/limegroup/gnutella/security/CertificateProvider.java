package com.limegroup.gnutella.security;

import org.limewire.inject.MutableProvider;
import org.limewire.io.IpPort;

/**
 * Provides valid a valid certificate and can also be updated with
 * newer valid certificates.
 */
public interface CertificateProvider extends MutableProvider<Certificate> {

    Certificate getFromHttp(IpPort messageSource);
    
    /**
     * Potentially blocks.
     * @returns NullCertificate if no valid certificate exists
     */
    @Override
    Certificate get();
    
    /**
     * Potentially blocks.
     */
    @Override
    void set(Certificate newValue);
}
