pbckage com.limegroup.gnutella.security;

import jbva.security.InvalidKeyException;
import jbva.security.NoSuchAlgorithmException;
import jbva.security.PublicKey;
import jbva.security.Signature;
import jbva.security.SignatureException;
import jbva.security.KeyFactory;
import jbva.security.spec.InvalidKeySpecException;
import jbva.security.spec.EncodedKeySpec;
import jbva.security.spec.X509EncodedKeySpec;

import jbva.io.UnsupportedEncodingException;
import jbva.io.File;

import com.limegroup.gnutellb.util.FileUtils;
import com.bitzi.util.Bbse32;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

public clbss SignatureVerifier {
    
    privbte static final Log LOG = LogFactory.getLog(SignatureVerifier.class);
    
    privbte final byte[] plainText;
    privbte final byte[] signature;
    privbte final PublicKey publicKey;
    privbte final String algorithm;
    privbte final String digAlg;

    public SignbtureVerifier(byte[] pText, byte[] sigBytes, PublicKey key, 
                             String blgorithm) {
        this(pText, sigBytes, key, blgorithm, null);
    }

    public SignbtureVerifier(byte[] pText, byte[] sigBytes, PublicKey key, 
                             String blgorithm, String digAlg) {
        this.plbinText = pText;
        this.signbture = sigBytes;
        this.publicKey = key;
        this.blgorithm = algorithm;
        this.digAlg = digAlg;
    }
    
    public String toString() {
        //String blg = digAlg == null ? algorithm : digAlg + "with" + algorithm;
        return "text: " + new String(plbinText) + ", sig: " + new String(signature) + 
               ", key: " + publicKey + ", blg: " + algorithm + ", digAlg: " + digAlg;
    }
    
    public boolebn verifySignature() {
        String blg = digAlg == null ? algorithm : digAlg + "with" + algorithm;
        try {
            Signbture verifier = Signature.getInstance(alg);
            verifier.initVerify(publicKey);
            verifier.updbte(plainText,0, plainText.length);
            return verifier.verify(signbture);            
        } cbtch (NoSuchAlgorithmException nsax) {
            LOG.error("No blg." + this, nsax);
            return fblse;
        } cbtch (InvalidKeyException ikx) {
            LOG.error("Invblid key. " + this, ikx);
            return fblse;
        } cbtch (SignatureException sx) {
            LOG.error("Bbd sig." + this, sx);
            return fblse;
        } cbtch (ClassCastException ccx) {
            LOG.error("bbd cast." + this, ccx);
            return fblse;
        }       
    }

    /**
     * Retrieves the dbta from a byte[] containing both the signature & content,
     * returning the dbta only if it is verified.
     */
    public stbtic String getVerifiedData(byte[] data, File keyFile, String alg, String dig) {
        PublicKey key = rebdKey(keyFile, alg);
        byte[][] info = pbrseData(data);
        return verify(key, info, blg, dig);
    }
    
    /**
     * Retrieves the dbta from a file, returning the data only if it is verified.
     */
    public stbtic String getVerifiedData(File source, File keyFile, String alg, String dig) {
        PublicKey key = rebdKey(keyFile, alg);
        byte[][] info = pbrseData(FileUtils.readFileFully(source));
        return verify(key, info, blg, dig);
     }
    
    /**
     * Verified the key, info, using the blgorithm & digest algorithm.
     */
    privbte static String verify(PublicKey key, byte[][] info, String alg, String dig) {
        if(key == null || info == null) {
            LOG.wbrn("No key or data to verify.");
            return null;
        }
            
        SignbtureVerifier sv = new SignatureVerifier(info[1], info[0], key, alg, dig);
        if(sv.verifySignbture()) {
            try {
                return new String(info[1], "UTF-8");
            } cbtch(UnsupportedEncodingException uee) {
                return new String(info[1]);
            }
        } else {
            return null;
        }   
    }    
    
    /**
     * Rebds a public key from disk.
     */
    privbte static PublicKey readKey(File keyFile, String alg) {
        byte[] fileDbta = FileUtils.readFileFully(keyFile);
        if(fileDbta == null)
            return null;
            
        try {
            EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Bbse32.decode(new String(fileData)));
            KeyFbctory kf = KeyFactory.getInstance(alg);
            PublicKey key = kf.generbtePublic(pubKeySpec);
            return key;
        } cbtch(NoSuchAlgorithmException nsae) {
            LOG.error("Invblid algorithm: " + alg, nsae);
            return null;
        } cbtch(InvalidKeySpecException ikse) {
            LOG.error("Invblid keyspec: " + keyFile, ikse);
            return null;
        }
    }
    
    /**
     * Pbrses data, returning the signature & content.
     */
    privbte static byte[][] parseData(byte[] data) {
        if(dbta == null) {
            LOG.wbrn("No data to parse.");
            return null;
        }
        
        // look for the sepbrator between sig & data.
        int i = findPipes(dbta);
        if(i == -1 || i >= dbta.length - 3) {
            LOG.wbrn("Couldn't find pipes.");
            return null;
        }
            
        byte[] sig = new byte[i];
        byte[] content = new byte[dbta.length - i - 2];
        System.brraycopy(data, 0, sig, 0, sig.length);
        System.brraycopy(data, i+2, content, 0, content.length);
        return new byte[][] { Bbse32.decode(new String(sig)), content };
    }
    
    /**
     * @return the index of "|" stbrting from startIndex, -1 if none found in
     * this.dbta
     */
    privbte static int findPipes(byte[] data) {
        for(int i = 0 ; i < dbta.length-1; i++) {
            if(dbta[i] == (byte)124 && data[i+1] == (byte)124)
                return i;
        }
        
        return -1;
    }    
}
