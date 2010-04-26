package org.limewire.security.id;

import java.math.BigInteger;
import java.security.PublicKey;

import org.limewire.io.GUID;

/** 
 * An Identity of a node includes 4 fields:
 * the node's signature public key
 * the node's GUID generated using the public key
 * the node's Diffie-Hellman public component for key agreement
 * a signature covering the above fields. 
 * 
 * All the 4 fields are public information and should all be sent to 
 * remote nodes if requested. 
 */

public interface Identity {

    public abstract GUID getGuid();

    public abstract PublicKey getPublicSignatureKey();

    public abstract BigInteger getPublicDiffieHellmanComponent();

    public abstract byte[] getSignature();

    public byte[] toByteArray();
    
}