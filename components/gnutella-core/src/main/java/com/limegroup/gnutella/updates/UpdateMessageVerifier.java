package com.limegroup.gnutella.updates;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.security.SignatureVerifier;
import org.limewire.service.ErrorService;
import org.limewire.util.Base32;
import org.limewire.util.CommonUtils;


/**
 * Provides static methods, which accept an InputStream and use the 
 * LimeWire public key to verify that the contents are authentic.
 */
public class UpdateMessageVerifier {
    
    private static final Log LOG = LogFactory.getLog(UpdateMessageVerifier.class);

    private byte[] data;
    private byte[] signature;
    private byte[] xmlMessage;
    
    /**
     * @param fromDisk true if the byte are being read from disk, false is the
     * bytes are being read from the network
     */
    public UpdateMessageVerifier(byte[] fromStream, boolean fromDisk) {
        if(fromStream == null)
            throw new IllegalArgumentException();
        this.data = fromStream;
    }
    
    
    public boolean verifySource() {        
        //read the input stream and parse it into signature and xmlMessage
        boolean parsed = parse(); 
        if(!parsed)
            return false;

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
        } catch(Throwable t) {
            LOG.error("Unable to read public key", t);
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
        
        SignatureVerifier verifier = 
                    new SignatureVerifier(xmlMessage,signature, pubKey, "DSA");
        
        return verifier.verifySignature();
    }

    private boolean parse() {
        int i;
        int j;
        i = findPipe(0);
        j = findPipe(i+1);
        if(i<0 || j<0) //no 2 pipes? this file cannot be the real thing, 
            return false;
        if( (data.length - j) < 10) //xml smaller than 10? no way
            return false;
        //now i is at the first | delimiter and j is at the second | delimiter
        byte[] temp = new byte[i];
        System.arraycopy(data,0,temp,0,i);
        String base32 = null;
        try {
            base32 = new String(temp, "UTF-8");
        } catch(UnsupportedEncodingException usx) {
            ErrorService.error(usx);
        }
        signature = Base32.decode(base32);
        xmlMessage = new byte[data.length-1-j];
        System.arraycopy(data,j+1,xmlMessage,0,data.length-1-j);
        return true;
    }
    
    /**
     * @return the index of "|" starting from startIndex, -1 if none found in
     * this.data
     */
    private int findPipe(int startIndex) {
        boolean found = false;
        int i = startIndex;
        for( ; i < data.length; i++) {
            if(data[i] == (byte)124) {
                found = true;
                break;
            }
        }
        if(found)
            return i;
        return -1;
    }

    public byte[] getMessageBytes() throws IllegalStateException {
        if(xmlMessage==null)
            throw new IllegalStateException();
        return xmlMessage;
    }
}
