package org.limewire.security.id;

import java.math.BigInteger;
import java.security.PublicKey;

import org.limewire.io.GUID;
import org.limewire.util.StringUtils;

public class Identity {
    private GUID id;
    private PublicKey signatureKey;
    private BigInteger dhPublicComponent;
    private byte[] signature;
    
    public Identity(GUID id, PublicKey signatureKey, BigInteger dhPublicComponent, byte[] signature){
        this.id = id;
        this.signatureKey = signatureKey;
        this.dhPublicComponent = dhPublicComponent;
        this.signature = signature;
    }
        
    GUID getGuid(){
        return id;
    }
    PublicKey getSignatureKey(){
        return signatureKey;
    }
    
    BigInteger getDHPublicComponent(){
        return dhPublicComponent;
    }
    
    byte[] getSignature(){
        return signature;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
      }
}
