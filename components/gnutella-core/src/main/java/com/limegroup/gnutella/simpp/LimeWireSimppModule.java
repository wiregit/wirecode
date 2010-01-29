package com.limegroup.gnutella.simpp;

import java.io.File;

import org.limewire.inject.LazySingleton;
import org.limewire.util.CommonUtils;
import org.limewire.util.URIUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.limegroup.gnutella.security.CertificateProvider;
import com.limegroup.gnutella.security.CertificateProviderImpl;
import com.limegroup.gnutella.security.CertificateVerifier;
import com.limegroup.gnutella.security.CertifiedMessageVerifier;
import com.limegroup.gnutella.security.CertifiedMessageVerifierImpl;
import com.limegroup.gnutella.security.FileCertificateReader;
import com.limegroup.gnutella.security.HttpCertificateReader;

public class LimeWireSimppModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SimppManager.class).to(SimppManagerImpl.class);
        bind(SimppDataProvider.class).to(SimppDataProviderImpl.class);
        bind(SimppSender.class);
    }
    
    @Provides @LazySingleton @Simpp CertificateProvider simppCertificateProvider(FileCertificateReader fileCertificateReader, HttpCertificateReader httpCertificateReader,
            CertificateVerifier certificateVerifier) {
        return new CertificateProviderImpl(fileCertificateReader, httpCertificateReader, certificateVerifier, new File(CommonUtils.getUserSettingsDir(), "simpp.cert"), URIUtils.toSafeUri("http://localhost/"));
    }
    
    @Provides @Simpp CertifiedMessageVerifier simppMessageVerifier(@Simpp CertificateProvider certificateProvider) {
        return new CertifiedMessageVerifierImpl(certificateProvider);
    }
    
}
