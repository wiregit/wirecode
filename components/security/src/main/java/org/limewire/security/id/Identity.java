package org.limewire.security.id;

import java.math.BigInteger;
import java.security.PublicKey;

import org.limewire.io.GUID;

public interface Identity {

    public abstract GUID getGuid();

    public abstract PublicKey getSignatureKey();

    public abstract BigInteger getDHPublicComponent();

    public abstract byte[] getSignature();

}