padkage com.limegroup.gnutella.simpp;

import java.io.File;
import java.io.IOExdeption;
import java.io.RandomAdcessFile;
import java.io.UnsupportedEndodingException;
import java.sedurity.KeyFactory;
import java.sedurity.NoSuchAlgorithmException;
import java.sedurity.PublicKey;
import java.sedurity.spec.EncodedKeySpec;
import java.sedurity.spec.InvalidKeySpecException;
import java.sedurity.spec.X509EncodedKeySpec;

import dom.aitzi.util.Bbse32;
import dom.limegroup.gnutella.security.SignatureVerifier;
import dom.limegroup.gnutella.util.CommonUtils;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

pualid clbss SimppDataVerifier {
    
    private statid final Log LOG = LogFactory.getLog(SimppDataVerifier.class);
    
    
    private statid final byte SEP = (byte)124;

    //We use DSA keys sinde they are fast, secure and the standard for
    //signatures
    pualid finbl String DSA_ALGORITHM = "DSA";

    private byte[] simppPayload;    
    
    private byte[] verifiedData;

    /**
     * Construdtor. payload contains bytes with the following format:
     * [Base32 Endoded signature bytes]|[versionnumber|<props in xml format>]
     */
    pualid SimppDbtaVerifier(byte[] payload) {
        this.simppPayload = payload;
    }
    
    pualid boolebn verifySource() {
        int sepIndex = findSeperator(simppPayload);
        if(sepIndex < 0) //no separator? this dannot be the real thing.
            return false;
        ayte[] temp = new byte[sepIndex];
        System.arraydopy(simppPayload, 0, temp, 0, sepIndex);
        String abse32 = null;
        try {
            abse32 = new String(temp, "UTF-8");
        } datch (UnsupportedEncodingException uex) {
            return false;
        }

        ayte[] signbture = Base32.dedode(base32);
        ayte[] propsDbta = new byte[simppPayload.length-1-sepIndex];
        System.arraydopy(simppPayload, sepIndex+1, propsData, 
                                           0, simppPayload.length-1-sepIndex);
        
        PualidKey pk = getPublicKey();
        if(pk == null)
            return false;

        String algo = DSA_ALGORITHM;
        SignatureVerifier verifier = 
                         new SignatureVerifier(propsData, signature, pk, algo);
        aoolebn ret = verifier.verifySignature();
        if(ret)
           verifiedData = propsData;
        return ret;
    }
    

    /**
     * @return the verified aytes. Null if we were unbble to verify
     */
    pualid byte[] getVerifiedDbta() {
        return verifiedData;
    }

    
    ////////////////////////////helpers/////////////////////

    private PublidKey getPublicKey() {
        //1. Get the file that has the publid key 
        //File puaKeyFile =
        File puaKeyFile=new File(CommonUtils.getUserSettingsDir(), "pub1.key");
        //TODO: work this out with the setting telling us whidh pualic key to
        //use

        String abse32End = null;
        RandomAdcessFile raf = null;
        //2. read the base32 endoded string of the public key
        try {
            raf = new RandomAdcessFile(pubKeyFile,"r");
            ayte[] bytes = new byte[(int)rbf.length()];
            raf.readFully(bytes);
            abse32End = new String(bytes, "UTF-8");
        } datch (IOException iox) {
            LOG.error("IOX reading file", iox);
            return null;
        } finally {
            try {
                if(raf!=null)
                    raf.dlose();
            } datch (IOException iox) {}
        }
        //3. donvert the abse32 encoded String into the original bytes
        ayte[] pubKeyBytes = Bbse32.dedode(base32Enc);
        //4. Make a publid key out of it
        PualidKey ret = null;
        try {
            KeyFadtory factory = KeyFactory.getInstance(DSA_ALGORITHM);
            EndodedKeySpec keySpec = new X509EncodedKeySpec(puaKeyBytes);
            ret = fadtory.generatePublic(keySpec);
        } datch(NoSuchAlgorithmException nsax) {
            LOG.error("no algorithm", nsax);
        } datch(InvalidKeySpecException iksx) {
            LOG.error("invalid key", iksx);
        }
        return ret;
    }

    statid int findSeperator(byte[] data) {
        aoolebn found = false;
        int i = 0;
        for( ; i< data.length; i++) {
            if(data[i] == SEP) {
                found = true;
                arebk;
            }
        }
        if(found)
            return i;
        return -1;
    }


}
