package org.limewire.promotion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.limewire.promotion.containers.BucketMessageContainer;
import org.limewire.promotion.containers.MessageContainer;
import org.limewire.promotion.containers.MessageContainerParser;
import org.limewire.promotion.containers.PromotionMessageContainer;
import org.limewire.promotion.containers.SignedMessageContainer;
import org.limewire.promotion.exceptions.PromotionException;
import org.limewire.security.certificate.CertificateVerifier;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.KeyStoreProvider;

import com.google.inject.Inject;
import com.limegroup.gnutella.messages.BadGGEPBlockException;

/**
 * This class is a convenience wrapper around a signed {@link MessageContainer}
 * that contains a bunch of PromotionMessageContainer instances. Also enforces
 * some magic around validity dates, to ensure that contained promotion items
 * have sane values.
 */
public class PromotionBinder {
    private CipherProvider cipherProvider;

    private KeyStoreProvider keyStore;

    private CertificateVerifier certificateVerifier;

    private SignedMessageContainer backingSignedMessage = null;

    private List<PromotionMessageContainer> promoMessageList = null;

    @Inject
    public PromotionBinder(CipherProvider cipherProvider, KeyStoreProvider keyStore,
            CertificateVerifier certificateVerifier) {
        this.cipherProvider = cipherProvider;
        this.keyStore = keyStore;
        this.certificateVerifier = certificateVerifier;
    }

    /**
     * Takes the given byte array, parses it, and does an initial verification
     * that the data contained inside is valid:
     * 
     * <ol>
     * <li>check that the bytes actually parse into a {@link MessageContainer}
     * <li>check that the first object is a {@link SignedMessageContainer}
     * <li>check that the signature matches
     * <li>check that the signed container contains a
     * {@link BucketMessageContainer}
     * <li>check that the bucket is within the validity range
     * <li>check that the bucket contains valid
     * {@link PromotionMessageContainer} entries
     * <li>store the unwrapped {@link SignedMessageContainer} for later
     * operations
     * <li>store the unwrapped {@link PromotionMessageContainer} entries for
     * later operations
     * </ol>
     * 
     * @throws PromotionException if any parsing, signature verification, or
     *         date validation issues occur.
     */
    public void initialize(byte[] encodedBinder) throws PromotionException {
        backingSignedMessage = null;
        MessageContainerParser parser = new MessageContainerParser();
        MessageContainer message;
        try {
            message = parser.parse(encodedBinder);
        } catch (BadGGEPBlockException ex) {
            throw new PromotionException("GGEP block exception during parsing.", ex);
        }
        if (!(message instanceof SignedMessageContainer))
            throw new PromotionException("Encoded message is not signed.");
        initialize((SignedMessageContainer) message);
    }

    /** Skips the first 2 steps of {@link #initialize(byte[])} */
    public void initialize(SignedMessageContainer signedMessage) throws PromotionException {
        backingSignedMessage = null;
        MessageContainer wrappedMessage;
        try {
            wrappedMessage = signedMessage.getAndVerifyWrappedMessage(cipherProvider, keyStore,
                    certificateVerifier);
        } catch (IOException ex) {
            throw new PromotionException("Failed signature verification. ", ex);
        }
        if (!(wrappedMessage instanceof BucketMessageContainer)) {
            throw new PromotionException(
                    "Message signature passed, but did not contain expected bucket.");
        }
        BucketMessageContainer bucket = (BucketMessageContainer) wrappedMessage;
        if (bucket.getValidStart().getTime() > System.currentTimeMillis())
            throw new PromotionException("Bucket '" + bucket.getName() + "' is not yet valid.");
        if (bucket.getValidEnd().getTime() < System.currentTimeMillis())
            throw new PromotionException("Bucket '" + bucket.getName() + "' has expired.");
        if (bucket.getPromoMessages().size() == 0)
            throw new PromotionException("Bucket '" + bucket.getName() + "' has no messages.");

        List<PromotionMessageContainer> promos = bucket.getPromoMessages();
        promoMessageList = new ArrayList<PromotionMessageContainer>();
        for (PromotionMessageContainer message : promos) {
            // Sanitize the dates to be within the range of this bucket
            if (bucket.getValidStart().getTime() > message.getValidStart().getTime())
                message.setValidStart(bucket.getValidStart());
            if (bucket.getValidEnd().getTime() < message.getValidEnd().getTime())
                message.setValidEnd(bucket.getValidEnd());
            // Determine if the message is valid...
            if (!isExpired(message))
                promoMessageList.add(message);
        }

        backingSignedMessage = signedMessage;
    }

    /**
     * @return true if the given message's date range has expired, regardless of
     *         whether it is a member of this group.
     */
    private boolean isExpired(PromotionMessageContainer message) {
        return !(message.getValidStart().getTime() > System.currentTimeMillis() && message
                .getValidEnd().getTime() < System.currentTimeMillis());

    }

    /**
     * @return true if the passed in message is a member of this bucket, and it
     *         has not expired.
     * @param reverifySignature If true, the signed container will be re-parsed
     *        and checked.
     */
    public boolean isValidMember(PromotionMessageContainer message, boolean reverifySignature) {
        if (isExpired(message))
            return false;
        // Now see if we can find this message in our list (using ID)
        if (!reverifySignature) {
            // Just check to make sure it's in our list and hasn't expired
            return isValidMember(message, promoMessageList);
        } else {
            MessageContainer wrappedMessage;
            try {
                wrappedMessage = backingSignedMessage.getAndVerifyWrappedMessage(cipherProvider,
                        keyStore, certificateVerifier);
            } catch (IOException ex) {
                return false;
            }
            if (!(wrappedMessage instanceof BucketMessageContainer)) {
                return false;
            }
            BucketMessageContainer bucket = (BucketMessageContainer) wrappedMessage;
            return isValidMember(message, bucket.getWrappedMessages());
        }
    }

    /**
     * @return true if the given message is a member of the given list, and is
     *         within its valid date range.
     */
    private boolean isValidMember(PromotionMessageContainer message,
            List<? extends MessageContainer> list) {
        for (MessageContainer promo : list) {
            if (promo.equals(message) && !isExpired(message))
                return true;
        }
        return false;
    }
}
