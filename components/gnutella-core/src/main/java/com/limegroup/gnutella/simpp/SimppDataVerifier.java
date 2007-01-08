package com.limegroup.gnutella.simpp;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.security.SignatureVerifier;
import org.limewire.util.Base32;

import com.limegroup.gnutella.util.LimeWireUtils;

public class SimppDataVerifier {
    
    private static final Log LOG = LogFactory.getLog(SimppDataVerifier.class);
    
    
    private static final byte SEP = (byte)124;

    //We use DSA keys since they are fast, secure and the standard for
    //signatures
    public final String DSA_ALGORITHM = "DSA";

    private byte[] simppPayload;    
    
    private byte[] verifiedData;

    /**
     * Constructor. payload contains bytes with the following format:
     * [Base32 Encoded signature bytes]|[versionnumber|<props in xml format>]
     */
    public SimppDataVerifier(byte[] payload) {
        this.simppPayload = payload;
    }
    
    public boolean verifySource() {
        int sepIndex = findSeperator(simppPayload);
        if(sepIndex < 0) //no separator? this cannot be the real thing.
            return false;
        byte[] temp = new byte[sepIndex];
        System.arraycopy(simppPayload, 0, temp, 0, sepIndex);
        String base32 = null;
        try {
            base32 = new String(temp, "UTF-8");
        } catch (UnsupportedEncodingException uex) {
            return false;
        }

        byte[] signature = Base32.decode(base32);
        byte[] propsData = new byte[simppPayload.length-1-sepIndex];
        System.arraycopy(simppPayload, sepIndex+1, propsData, 
                                           0, simppPayload.length-1-sepIndex);
        
        PublicKey pk = getPublicKey();
        if(pk == null)
            return false;

        String algo = DSA_ALGORITHM;
        SignatureVerifier verifier = 
                         new SignatureVerifier(propsData, signature, pk, algo);
        boolean ret = verifier.verifySignature();
        if(ret)
           verifiedData = propsData;
        return ret;
    }
    

    /**
     * @return the verified bytes. Null if we were unable to verify
     */
    public byte[] getVerifiedData() {
        return verifiedData;
    }

    
    ////////////////////////////helpers/////////////////////

    private PublicKey getPublicKey() {
        //1. Get the file that has the public key 
        //File pubKeyFile =
        File pubKeyFile=new File(LimeWireUtils.getUserSettingsDir(), "pub1.key");
        //TODO: work this out with the setting telling us which public key to
        //use

        String base32Enc = null;
        RandomAccessFile raf = null;
        //2. read the base32 encoded string of the public key
        try {
            raf = new RandomAccessFile(pubKeyFile,"r");
            byte[] bytes = new byte[(int)raf.length()];
            raf.readFully(bytes);
            base32Enc = new String(bytes, "UTF-8");
        } catch (IOException iox) {
            LOG.error("IOX reading file", iox);
            return null;
        } finally {
            try {
                if(raf!=null)
                    raf.close();
            } catch (IOException iox) {}
        }
        //3. convert the base32 encoded String into the original bytes
        byte[] pubKeyBytes = Base32.decode(base32Enc);
        //4. Make a public key out of it
        PublicKey ret = null;
        try {
            KeyFactory factory = KeyFactory.getInstance(DSA_ALGORITHM);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
            ret = factory.generatePublic(keySpec);
        } catch(NoSuchAlgorithmException nsax) {
            LOG.error("no algorithm", nsax);
        } catch(InvalidKeySpecException iksx) {
            LOG.error("invalid key", iksx);
        }
        return ret;
    }

    static int findSeperator(byte[] data) {
        boolean found = false;
        int i = 0;
        for( ; i< data.length; i++) {
            if(data[i] == SEP) {
                found = true;
                break;
            }
        }
        if(found)
            return i;
        return -1;
    }


}
