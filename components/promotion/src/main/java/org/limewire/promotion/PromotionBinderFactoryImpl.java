package org.limewire.promotion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.promotion.exceptions.PromotionException;
import org.limewire.security.certificate.CertificateVerifier;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.KeyStoreProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PromotionBinderFactoryImpl implements PromotionBinderFactory {

    private final static Log LOG = LogFactory.getLog(PromotionBinderFactoryImpl.class);

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

    public PromotionBinder newBinder(InputStream in) {
        if (in == null) {
            return null;
        }
        PromotionBinder binder = new PromotionBinder(cipherProvider, keyStore, certificateVerifier);
        try {
            binder.initialize(toBytes(in));
        } catch (PromotionException e) {
            throw new RuntimeException("PromotionException:", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException:", e);
        }
        return binder;
    }

    private byte[] toBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
        return out.toByteArray();
    }
}
