package org.limewire.security.certificate;

import com.google.inject.AbstractModule;

public class LimeWireSecurityCertificateModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RootCAProvider.class).to(RootCAProviderImpl.class);
        bind(HashLookupProvider.class).to(HashLookupProviderDNSTXTImpl.class);
        bind(KeyStoreProvider.class).to(KeyStoreProviderImpl.class);
        bind(HashCalculator.class).to(HashCalculatorSHA1Impl.class);
        bind(CipherProvider.class).to(CipherProviderImpl.class);
        bind(CertificateVerifier.class).to(CertificateVerifierImpl.class);
    }

}
