package com.limegroup.gnutella.simpp;

import java.io.File;
import java.net.URI;

import org.limewire.inject.LazySingleton;
import org.limewire.util.CommonUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.limegroup.gnutella.security.CertificateProvider;
import com.limegroup.gnutella.security.CertificateProviderImpl;
import com.limegroup.gnutella.security.CertificateVerifier;
import com.limegroup.gnutella.security.CertifiedMessageVerifier;
import com.limegroup.gnutella.security.CertifiedMessageVerifierImpl;
import com.limegroup.gnutella.security.DefaultDataProvider;
import com.limegroup.gnutella.security.FileCertificateReader;
import com.limegroup.gnutella.security.HttpCertificateReader;

public class LimeWireSimppModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SimppManager.class).to(SimppManagerImpl.class);
        bind(DefaultDataProvider.class).annotatedWith(Simpp.class).to(SimppDataProviderImpl.class);
        bind(SimppSender.class);
        bind(SimppDataVerifier.class).to(SimppDataVerifierImpl.class);
    }
    
    @Provides @LazySingleton @Simpp CertificateProvider simppCertificateProvider(FileCertificateReader fileCertificateReader, HttpCertificateReader httpCertificateReader,
            CertificateVerifier certificateVerifier) {
        return new CertificateProviderImpl(fileCertificateReader, httpCertificateReader, certificateVerifier, new File(CommonUtils.getUserSettingsDir(), "simpp.cert"), URI.create("http://certs.limewire.com/simpp/simpp.cert"));
    }
    
    @Provides @Simpp CertifiedMessageVerifier simppMessageVerifier(@Simpp CertificateProvider certificateProvider, CertificateVerifier certificateVerifier) {
        return new CertifiedMessageVerifierImpl(certificateProvider, certificateVerifier);
    }
    
}
