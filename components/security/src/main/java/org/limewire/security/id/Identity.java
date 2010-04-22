package org.limewire.security.id;

import java.math.BigInteger;
import java.security.PublicKey;

import org.limewire.io.GUID;

public interface Identity {

    public abstract GUID getGuid();

    public abstract PublicKey getPublicSignatureKey();

    public abstract BigInteger getPublicDiffieHellmanComponent();

    public abstract byte[] getSignature();

    public byte[] toByteArray();
    
}