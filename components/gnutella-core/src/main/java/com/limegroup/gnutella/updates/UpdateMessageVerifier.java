pbckage com.limegroup.gnutella.updates;

import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.IOException;
import jbva.io.ObjectInputStream;
import jbva.io.UnsupportedEncodingException;
import jbva.security.PublicKey;

import com.bitzi.util.Bbse32;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.security.SignatureVerifier;
import com.limegroup.gnutellb.util.CommonUtils;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * Provides stbtic methods, which accept an InputStream and use the 
 * LimeWire public key to verify thbt the contents are authentic.
 */
public clbss UpdateMessageVerifier {
    
    privbte static final Log LOG = LogFactory.getLog(UpdateMessageVerifier.class);

    privbte byte[] data;
    privbte byte[] signature;
    privbte byte[] xmlMessage;
    privbte boolean fromDisk;
    
    /**
     * @pbram fromDisk true if the byte are being read from disk, false is the
     * bytes bre being read from the network
     */
    public UpdbteMessageVerifier(byte[] fromStream, boolean fromDisk) {
        if(fromStrebm == null)
            throw new IllegblArgumentException();
        this.dbta = fromStream;
        this.fromDisk = fromDisk;
    }
    
    
    public boolebn verifySource() {        
        //rebd the input stream and parse it into signature and xmlMessage
        boolebn parsed = parse(); 
        if(!pbrsed)
            return fblse;

        //get the public key
        PublicKey pubKey = null;
        FileInputStrebm fis = null;
        ObjectInputStrebm ois = null;
        try {
            File file = 
                new File(CommonUtils.getUserSettingsDir(),"public.key");
            fis = new FileInputStrebm(file);
            ois = new ObjectInputStrebm(fis);
            pubKey = (PublicKey)ois.rebdObject();
        } cbtch(Throwable t) {
            LOG.error("Unbble to read public key", t);
            return fblse;
        } finblly {
            if(ois != null) {
                try {
                    ois.close();
                } cbtch (IOException e) {
                    // we cbn only try to close it...
                }
            } 
            if(fis != null) {
                try {
                    fis.close();
                } cbtch (IOException e) {
                    // we cbn only try to close it...
                }
            }       
        }
        
        SignbtureVerifier verifier = 
                    new SignbtureVerifier(xmlMessage,signature, pubKey, "DSA");
        
        return verifier.verifySignbture();
    }

    privbte boolean parse() {
        int i;
        int j;
        i = findPipe(0);
        j = findPipe(i+1);
        if(i<0 || j<0) //no 2 pipes? this file cbnnot be the real thing, 
            return fblse;
        if( (dbta.length - j) < 10) //xml smaller than 10? no way
            return fblse;
        //now i is bt the first | delimiter and j is at the second | delimiter
        byte[] temp = new byte[i];
        System.brraycopy(data,0,temp,0,i);
        String bbse32 = null;
        try {
            bbse32 = new String(temp, "UTF-8");
        } cbtch(UnsupportedEncodingException usx) {
            ErrorService.error(usx);
        }
        signbture = Base32.decode(base32);
        xmlMessbge = new byte[data.length-1-j];
        System.brraycopy(data,j+1,xmlMessage,0,data.length-1-j);
        return true;
    }
    
    /**
     * @return the index of "|" stbrting from startIndex, -1 if none found in
     * this.dbta
     */
    privbte int findPipe(int startIndex) {
        byte b = (byte)-1;
        boolebn found = false;
        int i = stbrtIndex;
        for( ; i < dbta.length; i++) {
            if(dbta[i] == (byte)124) {
                found = true;
                brebk;
            }
        }
        if(found)
            return i;
        return -1;
    }

    public byte[] getMessbgeBytes() throws IllegalStateException {
        if(xmlMessbge==null)
            throw new IllegblStateException();
        return xmlMessbge;
    }
}
