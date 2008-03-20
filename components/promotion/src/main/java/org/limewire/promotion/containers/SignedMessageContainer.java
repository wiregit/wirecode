package org.limewire.promotion.containers;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.limewire.security.certificate.CertificateVerifier;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.KeyStoreProvider;
import org.limewire.security.certificate.CipherProvider.SignatureType;
import org.limewire.util.ByteUtil;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;

/**
 * Container to hold some other container along with a signature to validate the
 * contents of the wrapped container. Always signs with
 * {@link SignatureType#SHA1_WITH_RSA}. Stores the encoded message, signature,
 * and other metadata in GGEP encoded form.
 * <p>
 * GGEP Keys:
 * <ul>
 * <li>W=wrapped message (byte array)
 * <li>S=signature (byte array)
 * <li>A=key alias (String as UTF-8 byte array)
 * </ul>
 */
public class SignedMessageContainer implements MessageContainer {
    private static final String KEY_WRAPPED_BYTES = "W";

    private static final String KEY_SIGNATURE = "S";

    private static final String KEY_ALIAS = "A";

    private GGEP payload = new GGEP();

    public byte[] getType() {
        return ByteUtil.toUTF8Bytes("SIGN");
    }

    public byte[] getEncoded() {
        payload.put(TYPE_KEY, getType());
        return payload.toByteArray();
    }

    public void parse(GGEP rawGGEP) throws BadGGEPBlockException {
        if (!ByteUtil.areEqual(getType(), rawGGEP.get(TYPE_KEY)))
            throw new BadGGEPBlockException("Incorrect type.");
        if (!rawGGEP.hasKey(KEY_ALIAS))
            throw new BadGGEPBlockException("Missing alias");
        if (!rawGGEP.hasKey(KEY_SIGNATURE))
            throw new BadGGEPBlockException("Missing signature");
        if (!rawGGEP.hasKey(KEY_WRAPPED_BYTES))
            throw new BadGGEPBlockException("Missing wrapped message");

        this.payload = rawGGEP;
    }

    /**
     * Calls the {@link #getEncoded()} method on the passed in message and
     * stores the signed result into the payload.
     * 
     * @throws IOException if there is a problem signing.
     */
    public void setAndSignWrappedMessage(MessageContainer wrappedMessage,
            CipherProvider cipherProvider, PrivateKey privateKey, String keyAlias)
            throws IOException {
        byte[] messagePayload = wrappedMessage.getEncoded();
        byte[] signature = cipherProvider.sign(messagePayload, privateKey,
                SignatureType.SHA1_WITH_RSA);
        payload = new GGEP();
        payload.put(KEY_WRAPPED_BYTES, messagePayload);
        payload.put(KEY_SIGNATURE, signature);
        payload.put(KEY_ALIAS, ByteUtil.toUTF8Bytes(keyAlias));
    }

    /** Parses the */
    public MessageContainer getAndVerifyWrappedMessage(CipherProvider cipherProvider,
            KeyStoreProvider keyStore, CertificateVerifier certificateVerifier) throws IOException {
        try {
            String keyAlias = ByteUtil.toStringFromUTF8Bytes(payload.getBytes(KEY_ALIAS));
            Certificate cert = keyStore.getKeyStore().getCertificate(keyAlias);
            if (!certificateVerifier.isValid(cert))
                throw new IOException("Invalid certificate retrieved.");
            byte[] wrappedBytes = payload.getBytes(KEY_WRAPPED_BYTES);
            if (!cipherProvider.verify(wrappedBytes, payload.getBytes(KEY_SIGNATURE), cert
                    .getPublicKey(), SignatureType.SHA1_WITH_RSA))
                throw new IOException("Wrapped message did not match the signature.");
            return new MessageContainerParser().parse(wrappedBytes);
        } catch (BadGGEPBlockException ex) {
            throw new IOException("BadGGEPBlockException parsing contents:" + ex.getMessage());
        } catch (KeyStoreException ex) {
            throw new IOException("KeyStoreException parsing contents:" + ex.getMessage());
        } catch (BadGGEPPropertyException ex) {
            throw new IOException("BadGGEPPropertyException parsing contents:" + ex.getMessage());
        }
    }

}
