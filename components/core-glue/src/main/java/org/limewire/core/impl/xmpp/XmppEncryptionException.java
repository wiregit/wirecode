package org.limewire.core.impl.xmpp;

import org.limewire.xmpp.api.client.XMPPException;

/**
 * This exception is throw when an error occurs while encrypting/decrypting passwords used in xmpp,
 * such as when the key used to decrypt/encrypt is invalid.
 */
public class XmppEncryptionException extends XMPPException {

    public XmppEncryptionException(String msg) {
        super(msg);
    }

    public XmppEncryptionException(Throwable cause) {
        super(cause);
    }

    public XmppEncryptionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
