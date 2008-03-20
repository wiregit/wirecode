package org.limewire.activation;

import java.security.PrivateKey;

import org.limewire.activation.exception.ActivationException;

public interface ActivationKeyParser {
    /**
     * Parses the encodedKey and outputs an instance of an ActivationKey, or
     * throws an {@link ActivationException} if there is a problem parsing, or
     * if the key's signature does not validate. NOTE: You must still check the
     * key's validity dates and other attributes before you actually activate
     * the key. This method just ensures the activation key was well-formed and
     * has a valid signature.
     */
    ActivationKey parse(String encodedKey) throws ActivationException;

    /**
     * @return a human-readable LimeWire Pro activation key in a generated form
     *         that could be sent to a user for activation. PrivateKey must be a
     *         valid LimeWire key or else the generated activation key will not
     *         work.
     */
    String generate(String header, ActivationKey activationKey, PrivateKey privateKey) throws ActivationException;
}
