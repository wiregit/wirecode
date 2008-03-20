package org.limewire.promotion;

import org.limewire.promotion.exceptions.PromotionException;
import org.limewire.security.certificate.CertificateVerifier;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.KeyStoreProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PromotionBinderFactoryImpl implements PromotionBinderFactory {
    
    private CipherProvider cipherProvider;
    private KeyStoreProvider keyStore;
    private CertificateVerifier certificateVerifier;

    @Inject
    public PromotionBinderFactoryImpl(CipherProvider cipherProvider, KeyStoreProvider keyStore,
            CertificateVerifier certificateVerifier) {
        this.cipherProvider = cipherProvider;
        this.keyStore = keyStore;
        this.certificateVerifier = certificateVerifier;
    }    

    public PromotionBinder newBinder(byte[] bytes) {
        //
        // These bytes can be null, so can this request. For null bytes, return null.
        //
        if (bytes == null) {
            return null;
        }
        PromotionBinder binder = new PromotionBinder(cipherProvider, keyStore, certificateVerifier);         
        try {
            binder.initialize(bytes);
        } catch (PromotionException e) {
            e.printStackTrace();
        }
        return binder;
    }
}
