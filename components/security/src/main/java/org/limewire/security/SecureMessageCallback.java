package org.limewire.security;


/** Callback notifying something that the security of the message has been processed. */
public interface SecureMessageCallback {
    public void handleSecureMessage(SecureMessage sm, boolean passed);
}
