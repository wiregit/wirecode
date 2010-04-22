package org.limewire.security.id;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.util.StringUtils;

public class IdentityImpl implements Identity {

    protected final GUID id;
    protected final PublicKey signaturePublicKey;
    protected final BigInteger dhPublicComponent;
    protected final byte[] signature;
    
    public IdentityImpl(GUID id, PublicKey signatureKey, BigInteger dhPublicComponent, byte[] signature){
        this.id = id;
        this.signaturePublicKey = signatureKey;
        this.dhPublicComponent = dhPublicComponent;
        this.signature = signature;
    }
        
    public IdentityImpl(byte[] data) throws InvalidDataException {
        try{
            // reading from stored data
            GGEP ggep = new GGEP(data);
            id = new GUID(ggep.getBytes("ID"));            
            KeyFactory factory = KeyFactory.getInstance(SecureIdManager.SIG_KEY_ALGO);
            signaturePublicKey = factory.generatePublic(new X509EncodedKeySpec(ggep.getBytes("SPU")));
            factory = KeyFactory.getInstance(SecureIdManager.AGREEMENT_ALGO);
            dhPublicComponent = new BigInteger(ggep.getBytes("DHPU"));
            signature = ggep.getBytes("SIG");
        } catch (BadGGEPBlockException e) {
            throw new InvalidDataException(e);
        } catch (BadGGEPPropertyException e) {
            throw new InvalidDataException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidDataException(e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidDataException(e);
        }
    }
    
    public GUID getGuid(){
        return id;
    }
    
    public PublicKey getPublicSignatureKey(){
        return signaturePublicKey;
    }
    
    public BigInteger getPublicDiffieHellmanComponent(){
        return dhPublicComponent;
    }
    
    public byte[] getSignature(){
        return signature;
    }
    
    public byte[] toByteArray() {
        GGEP ggep = new GGEP();
        ggep.put("ID", id.bytes());
        ggep.put("SPU", signaturePublicKey.getEncoded());
        ggep.put("DHPU", dhPublicComponent.toByteArray());
        ggep.put("SIG", signature);
        return ggep.toByteArray();
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
