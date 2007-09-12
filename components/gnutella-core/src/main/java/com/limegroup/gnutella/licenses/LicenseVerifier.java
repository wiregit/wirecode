package com.limegroup.gnutella.licenses;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.limewire.concurrent.ExecutorsHelper;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class LicenseVerifier {

    /**
     * The queue that all license verification attempts are processed in.
     */
    private static final ExecutorService VQUEUE = ExecutorsHelper
            .newProcessingQueue("LicenseVerifier");

    private final Provider<LicenseCache> licenseCache;

    @Inject
    public LicenseVerifier(Provider<LicenseCache> licenseCache) {
        this.licenseCache = licenseCache;
    }

    /**
     * Starts verification of the license.
     * 
     * The listener is notified when verification is finished.
     */
    public void verify(final License license, final VerificationListener listener) {
        VQUEUE.execute(new Verifier(license, listener));
    }

    public void verifyAndWait(final License license, final VerificationListener listener)
            throws InterruptedException, ExecutionException {
        Future<?> result = VQUEUE.submit(new Verifier(license, listener));
        result.get();
    }

    /**
     * Runnable that actually does the verification. This will retrieve the body
     * of a web page from the licenseURI, parse it, set the last verified time,
     * and cache it in the LicenseCache.
     */
    private class Verifier implements Runnable {

        private final License license;

        private final VerificationListener verificationListener;

        public Verifier(License license, VerificationListener verificationListener) {
            this.license = license;
            this.verificationListener = verificationListener;
        }

        public void run() {
            license.verify(licenseCache.get());
            if (verificationListener != null) {
                verificationListener.licenseVerified(license);
            }
        }
    }
    
}
