package com.limegroup.gnutella;

import org.limewire.i18n.I18nMarker;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.ssl.SSLEngineTest;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.service.ErrorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.settings.SSLSettings;

@Singleton
class TlsTester implements Service {

    private final Provider<ByteBufferCache> bbCache;
    
    @Inject TlsTester(Provider<ByteBufferCache> bbCache) {
        this.bbCache = bbCache;
    }
    
    public String getServiceName() {
        return I18nMarker.marktr("TLS Encryption");
    }

    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    public void initialize() {}
    
    public void start() {
        if(SSLSettings.isIncomingTLSEnabled() || SSLSettings.isOutgoingTLSEnabled()) {
            SSLEngineTest sslTester = new SSLEngineTest(SSLUtils.getTLSContext(), SSLUtils.getTLSCipherSuites(), bbCache.get());
            if(!sslTester.go()) {
                Throwable t = sslTester.getLastFailureCause();
                SSLSettings.disableTLS(t);
                if(!SSLSettings.IGNORE_SSL_EXCEPTIONS.getValue() && !sslTester.isIgnorable(t))
                    ErrorService.error(t);
            }
        }
    }
    
    public void stop() {}
}
