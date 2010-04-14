package org.limewire.security.id;

import java.math.BigInteger;
import java.security.PublicKey;

import org.limewire.io.GUID;
import org.limewire.util.StringUtils;

public class IdentityImpl implements Identity {

    private final GUID id;
    private final PublicKey signatureKey;
    private final BigInteger dhPublicComponent;
    private final byte[] signature;
    
    public IdentityImpl(GUID id, PublicKey signatureKey, BigInteger dhPublicComponent, byte[] signature){
        this.id = id;
        this.signatureKey = signatureKey;
        this.dhPublicComponent = dhPublicComponent;
        this.signature = signature;
    }
        
    public GUID getGuid(){
        return id;
    }
    
    public PublicKey getSignatureKey(){
        return signatureKey;
    }
    
    public BigInteger getDHPublicComponent(){
        return dhPublicComponent;
    }
    
    public byte[] getSignature(){
        return signature;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
