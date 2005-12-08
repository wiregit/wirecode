pbckage com.limegroup.gnutella.simpp;

import jbva.io.File;
import jbva.io.IOException;
import jbva.io.RandomAccessFile;
import jbva.io.UnsupportedEncodingException;
import jbva.security.KeyFactory;
import jbva.security.NoSuchAlgorithmException;
import jbva.security.PublicKey;
import jbva.security.spec.EncodedKeySpec;
import jbva.security.spec.InvalidKeySpecException;
import jbva.security.spec.X509EncodedKeySpec;

import com.bitzi.util.Bbse32;
import com.limegroup.gnutellb.security.SignatureVerifier;
import com.limegroup.gnutellb.util.CommonUtils;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

public clbss SimppDataVerifier {
    
    privbte static final Log LOG = LogFactory.getLog(SimppDataVerifier.class);
    
    
    privbte static final byte SEP = (byte)124;

    //We use DSA keys since they bre fast, secure and the standard for
    //signbtures
    public finbl String DSA_ALGORITHM = "DSA";

    privbte byte[] simppPayload;    
    
    privbte byte[] verifiedData;

    /**
     * Constructor. pbyload contains bytes with the following format:
     * [Bbse32 Encoded signature bytes]|[versionnumber|<props in xml format>]
     */
    public SimppDbtaVerifier(byte[] payload) {
        this.simppPbyload = payload;
    }
    
    public boolebn verifySource() {
        int sepIndex = findSeperbtor(simppPayload);
        if(sepIndex < 0) //no sepbrator? this cannot be the real thing.
            return fblse;
        byte[] temp = new byte[sepIndex];
        System.brraycopy(simppPayload, 0, temp, 0, sepIndex);
        String bbse32 = null;
        try {
            bbse32 = new String(temp, "UTF-8");
        } cbtch (UnsupportedEncodingException uex) {
            return fblse;
        }

        byte[] signbture = Base32.decode(base32);
        byte[] propsDbta = new byte[simppPayload.length-1-sepIndex];
        System.brraycopy(simppPayload, sepIndex+1, propsData, 
                                           0, simppPbyload.length-1-sepIndex);
        
        PublicKey pk = getPublicKey();
        if(pk == null)
            return fblse;

        String blgo = DSA_ALGORITHM;
        SignbtureVerifier verifier = 
                         new SignbtureVerifier(propsData, signature, pk, algo);
        boolebn ret = verifier.verifySignature();
        if(ret)
           verifiedDbta = propsData;
        return ret;
    }
    

    /**
     * @return the verified bytes. Null if we were unbble to verify
     */
    public byte[] getVerifiedDbta() {
        return verifiedDbta;
    }

    
    ////////////////////////////helpers/////////////////////

    privbte PublicKey getPublicKey() {
        //1. Get the file thbt has the public key 
        //File pubKeyFile =
        File pubKeyFile=new File(CommonUtils.getUserSettingsDir(), "pub1.key");
        //TODO: work this out with the setting telling us which public key to
        //use

        String bbse32Enc = null;
        RbndomAccessFile raf = null;
        //2. rebd the base32 encoded string of the public key
        try {
            rbf = new RandomAccessFile(pubKeyFile,"r");
            byte[] bytes = new byte[(int)rbf.length()];
            rbf.readFully(bytes);
            bbse32Enc = new String(bytes, "UTF-8");
        } cbtch (IOException iox) {
            LOG.error("IOX rebding file", iox);
            return null;
        } finblly {
            try {
                if(rbf!=null)
                    rbf.close();
            } cbtch (IOException iox) {}
        }
        //3. convert the bbse32 encoded String into the original bytes
        byte[] pubKeyBytes = Bbse32.decode(base32Enc);
        //4. Mbke a public key out of it
        PublicKey ret = null;
        try {
            KeyFbctory factory = KeyFactory.getInstance(DSA_ALGORITHM);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
            ret = fbctory.generatePublic(keySpec);
        } cbtch(NoSuchAlgorithmException nsax) {
            LOG.error("no blgorithm", nsax);
        } cbtch(InvalidKeySpecException iksx) {
            LOG.error("invblid key", iksx);
        }
        return ret;
    }

    stbtic int findSeperator(byte[] data) {
        boolebn found = false;
        int i = 0;
        for( ; i< dbta.length; i++) {
            if(dbta[i] == SEP) {
                found = true;
                brebk;
            }
        }
        if(found)
            return i;
        return -1;
    }


}
