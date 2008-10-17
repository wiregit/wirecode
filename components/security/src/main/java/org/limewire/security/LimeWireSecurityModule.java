package org.limewire.security;

import org.limewire.security.auth.UserStore;
import org.limewire.security.auth.UserStoreImpl;
import org.limewire.security.certificate.LimeWireSecurityCertificateModule;

import com.google.inject.AbstractModule;

public class LimeWireSecurityModule extends AbstractModule {

    @Override
    protected void configure() {
        binder().install(new LimeWireSecurityCertificateModule());
        bind(UserStore.class).to(UserStoreImpl.class);
    }

}
