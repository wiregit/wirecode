package com.limegroup.gnutella.version;

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

public class LimeWireUpdateModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DefaultDataProvider.class).annotatedWith(Update.class).to(UpdateDefaultDataProviderImpl.class);
    }
    
    @Provides @LazySingleton @Update CertificateProvider updateCertificateProvider(FileCertificateReader fileCertificateReader, HttpCertificateReader httpCertificateReader,
            CertificateVerifier certificateVerifier) {
        return new CertificateProviderImpl(fileCertificateReader, httpCertificateReader, certificateVerifier, new File(CommonUtils.getUserSettingsDir(), "update.cert"), URI.create("http://certs.limewire.com/update/update.cert"));
    }
    
    @Provides @Update CertifiedMessageVerifier updateMessageVerifier(@Update CertificateProvider certificateProvider, CertificateVerifier certificateVerifier) {
        return new CertifiedMessageVerifierImpl(certificateProvider, certificateVerifier);
    }

    public static void main(String[] args) {
        System.out.println(URI.create("http://certs.limewire.com/update/update.cert").toASCIIString());
    }
}
