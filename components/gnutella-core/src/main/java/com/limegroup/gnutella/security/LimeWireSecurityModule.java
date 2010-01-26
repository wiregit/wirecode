package com.limegroup.gnutella.security;

import com.google.inject.AbstractModule;

public class LimeWireSecurityModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(CertificateParser.class).to(CertificateParserImpl.class);
        bind(CertificateVerifier.class).to(CertificateVerifierImpl.class);
        bind(FileCertificateReaderImpl.class);
        bind(HttpCertificateReaderImpl.class);
    }

}
