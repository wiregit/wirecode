package com.limegroup.gnutella.updates;

import java.security.*;
import java.io.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.bitzi.util.*;

/**
 * Provides static methods, which accept an InputStream and use the 
 * LimeWire public key to verify that the contents are authentic.
 */
public class UpdateMessageVerifier {

    private byte[] data;
    private byte[] signature;
    private byte[] xmlMessage;
    
    public UpdateMessageVerifier(byte[] fromStream) {
        if(fromStream == null)
            throw new IllegalArgumentException();
        this.data = fromStream;
    }
    
    
    public boolean verifySource() {        
        //read the input stream and parse it into signature and xmlMessage
        parse(); 
        if(CommonUtils.isJava118()) //Java118 installs have trouble w/ publickey
            return true;
        //get the public key
        PublicKey pubKey = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            File file = 
                new File(CommonUtils.getUserSettingsDir(),"public.key");
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            pubKey = (PublicKey)ois.readObject();
        } catch (ClassNotFoundException cnfx) {
            return false;
        } catch (IOException iox) { //could not read public key?
            return false;
        } finally {
            if(ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    // we can only try to close it...
                }
            } 
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // we can only try to close it...
                }
            }       
        }
        try {            
            //initialize the verifier
            Signature verifier = Signature.getInstance("DSA");
            verifier.initVerify(pubKey);//initialize the signaure
            verifier.update(xmlMessage,0,xmlMessage.length);
            //verify
            return verifier.verify(signature);
        } catch (NoSuchAlgorithmException nsax) {
            ErrorService.error(nsax);
            return false;
        } catch (InvalidKeyException ikx) {
            return false;
        } catch (SignatureException sx) {
            return false;
        }
    }

    private void parse() {
        byte b;
        int i;
        int j;
        for(i=0, b=-1; b!=124; i++)
            b = data[i];
        i--;
        //now i is at the first | delimiter
        for(j=i+1, b=-1; b!=124; j++)
            b = data[j];
        j--;
        //now j is at the second | delimiter
        byte[] temp = new byte[i];
        System.arraycopy(data,0,temp,0,i);
        String base32 = new String(temp);
        signature = Base32.decode(base32);
        xmlMessage = new byte[data.length-1-j];
        System.arraycopy(data,j+1,xmlMessage,0,data.length-1-j);       
    }

    public byte[] getMessageBytes() throws IllegalStateException {
        if(xmlMessage==null)
            throw new IllegalStateException();
        return xmlMessage;
    }
}
