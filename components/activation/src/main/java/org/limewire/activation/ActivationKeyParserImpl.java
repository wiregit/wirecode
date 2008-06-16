package org.limewire.activation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import org.limewire.activation.exception.ActivationException;
import org.limewire.collection.Tuple;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.GGEP;
import org.limewire.security.certificate.CertificateProvider;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.CipherProvider.SignatureType;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ActivationKeyParserImpl implements ActivationKeyParser {
    private CipherProvider cipherProvider;

    private CertificateProvider certificateProvider;

    @Inject
    public ActivationKeyParserImpl(CipherProvider cipherProvider,
            CertificateProvider certificateProvider) {
        this.cipherProvider = cipherProvider;
        this.certificateProvider = certificateProvider;
    }

    public String generate(String header, ActivationKey activationKey, PrivateKey privateKey)
            throws ActivationException {
        StringBuilder body = new StringBuilder();
        body.append(header).append("\n\n");
        body.append(activationKey.toPEMEncoded());
        // Body is what we'll sign

        StringBuilder builder = new StringBuilder();
        builder.append(ActivationConstants.ACTIVATION_KEY_BEGIN).append('\n');
        builder.append(body.toString());
        builder.append(ActivationConstants.ACTIVATION_SIGN_BEGIN).append('\n');

        // Generate our signature
        try {
            byte[] signature = cipherProvider.sign(StringUtils.toUTF8Bytes(body.toString()),
                    privateKey, SignatureType.SHA1_WITH_RSA);
            builder.append(PemCodec.encode(signature));
        } catch (IOException ex) {
            throw new ActivationException("IOException while signing.", ex);
        }

        builder.append(ActivationConstants.ACTIVATION_SIGN_END);
        return builder.toString();
    }

    public ActivationKey parse(String encodedKey) throws ActivationException {
        Tuple<String, String> bodyAndSignature = parseBodyAndSignature(encodedKey);
        // Let's verify the signature
        byte[] message = StringUtils.toUTF8Bytes(bodyAndSignature.getFirst());
        byte[] signature = PemCodec.decode(bodyAndSignature.getSecond());
        try {
            Certificate certificate = certificateProvider
                    .getCertificate(ActivationConstants.ACTIVATION_CERTIFICATE_ALIAS);
            if (!cipherProvider.verify(message, signature, certificate.getPublicKey(),
                    SignatureType.SHA1_WITH_RSA)) {
                throw new ActivationException("Key failed signature check.");
            }
        } catch (CertificateException ex) {
            throw new ActivationException("Certificate exception during signature check.", ex);
        } catch (IOException ex) {
            throw new ActivationException("IO exception during signature check.", ex);
        }
        // OK, we're verified!

        Tuple<String, String> bodyAndGGEP = parseBodyAndGGEP(bodyAndSignature.getFirst());

        try {
            return new ActivationKey(new GGEP(PemCodec.decode(bodyAndGGEP.getSecond()), 0));
        } catch (BadGGEPBlockException ex) {
            throw new ActivationException("BadGGEPBlockException during final parse.", ex);
        }
    }

    /**
     * Takes a key string, and splits it in two, returning the body (between
     * start of the key and start of the signature) and signature (start of
     * signature to end of signature). Both fields always end with a '\n'.
     * Package-visible for testing.
     * 
     * <pre>
     * -----BEGIN LIMEWIRE KEY-----
     * body
     * -----BEGIN LIMEWIRE SIGNATURE-----
     * signature
     * -----END LIMEWIRE SIGNATURE-----
     * </pre>
     * 
     * @throws ActivationException if there is a problem finding any of the
     *         expected tokens, or some other problem pops up.
     */
    Tuple<String, String> parseBodyAndSignature(String encodedKey) throws ActivationException {
        BufferedReader reader = new BufferedReader(new StringReader(encodedKey));
        StringBuilder body = new StringBuilder();
        StringBuilder signature = new StringBuilder();
        boolean foundStart = false, foundSignature = false, foundEnd = false;
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (!foundStart) {
                    if (line.equals(ActivationConstants.ACTIVATION_KEY_BEGIN))
                        foundStart = true;
                    continue;
                }
                if (!foundSignature) {
                    if (line.equals(ActivationConstants.ACTIVATION_SIGN_BEGIN)) {
                        foundSignature = true;
                        continue;
                    }
                    body.append(line).append('\n');
                } else {
                    if (line.equals(ActivationConstants.ACTIVATION_SIGN_END)) {
                        foundEnd = true;
                        break;
                    }
                    signature.append(line).append('\n');
                }
            }
            if (!foundStart || !foundSignature || !foundEnd)
                throw new ActivationException("Unable to find expected token during parsing.");
        } catch (IOException impossible) {
            throw new ActivationException("IOException while parsing String???", impossible);
        }

        return new Tuple<String, String>(body.toString(), signature.toString());
    }

    /**
     * Parses the body text of a key into two parts: The first part consists of
     * all the lines of text leading up to the first empty line. The second part
     * consists of the content of all the following lines concatenated together
     * (without linebreaks) to facilitate base64 decoding the expected GGEP
     * code.
     * 
     * @throws ActivationException if expected tokens aren't found or any other
     *         parse error occurs.
     */
    Tuple<String, String> parseBodyAndGGEP(String encodedBody) throws ActivationException {
        BufferedReader reader = new BufferedReader(new StringReader(encodedBody));
        StringBuilder body = new StringBuilder();
        StringBuilder ggep = new StringBuilder();
        boolean foundBreak = false;
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (!foundBreak) {
                    if (line.equals("")) {
                        foundBreak = true;
                        continue;
                    }
                    body.append(line).append('\n');
                } else {
                    ggep.append(line);
                }
            }
            if (!foundBreak)
                throw new ActivationException("Unable to find expected break during parsing.");
        } catch (IOException impossible) {
            throw new ActivationException("IOException while parsing String???", impossible);
        }

        return new Tuple<String, String>(body.toString(), ggep.toString());
    }
}
