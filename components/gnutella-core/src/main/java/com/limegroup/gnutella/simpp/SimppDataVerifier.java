package com.limegroup.gnutella.simpp;

import java.security.*;
import java.io.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.security.*;
import com.limegroup.gnutella.util.*;
import com.bitzi.util.*;
import org.xml.sax.*;

public class SimppDataVerifier {
    
    private static final byte SEP = (byte)124;

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
            ErrorService.error(uex);
        }
        byte[] signature = Base32.decode(base32);
        byte[] propsData = new byte[simppPayload.length-1-sepIndex];//TODO check
        System.arraycopy(simppPayload, sepIndex+1, propsData, 
                                           0, simppPayload.length-1-sepIndex);
        //TODO1: Find a better way to store public keys 
        PublicKey pk = null;
        //TODO1: Choose a strong encryption Algorith, and now we are not
        //constrained by java118
        String algo = null;
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
