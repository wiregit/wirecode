package com.limegroup.gnutella.updates;

import java.security.*;
import java.io.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.bitzi.util.*;
import org.xml.sax.*;
import com.limegroup.gnutella.security.*;
import com.sun.java.util.collections.*;

/**
 * Provides static methods, which accept an InputStream and use the 
 * LimeWire public key to verify that the contents are authentic.
 */
public class UpdateMessageVerifier {

    private byte[] data;
    private byte[] signature;
    private byte[] xmlMessage;
    private boolean fromDisk;
    private static boolean testing118 = false;
    
    /**
     * @param fromDisk true if the byte are being read from disk, false is the
     * bytes are being read from the network
     */
    public UpdateMessageVerifier(byte[] fromStream, boolean fromDisk) {
        if(fromStream == null)
            throw new IllegalArgumentException();
        this.data = fromStream;
        this.fromDisk = fromDisk;
    }
    
    
    public boolean verifySource() {        
        //read the input stream and parse it into signature and xmlMessage
        boolean parsed = parse(); 
        if(!parsed)
            return false;
        if(CommonUtils.isJava118()) {
            //Java118 installs have trouble w/ publickey
            if(fromDisk)
                return true;
            return checkVersionForJava118();
        }
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
        byte b = (byte)-1;
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

    /**
     *  Checks if the version file is correct, loosely based on all the
     *  connections machines running java118 have made with their UPs.
     */
    private boolean checkVersionForJava118() {
        ConnectionManager connManager = RouterService.getConnectionManager();
        //If we are a UP running java 118 this is bad, and we dont really want
        //to iterate over all our connections, we simply return false, so
        //machines that are running Java 118 and are UPs will not get the update
        //message
        if(!testing118 && !connManager.isShieldedLeaf())//I am a UP w/ java118?
            return false;
        //Let's see what the advertised message in the file is. 

        //Note: Java118 machines will have to parse the xml twice, but that's
        //not too bad.
        UpdateFileParser parser = null;
        String xml = null;
        try {
            xml = new String(getMessageBytes(), "UTF-8");
        } catch(UnsupportedEncodingException uex) {
            return false;
        } 
        try {
            parser = new UpdateFileParser(xml);
        } catch (SAXException sx) {
            return false;
        } catch (IOException iox) {
            return false;
        } 
        String version = parser.getVersion();
        if(version==null || version.equals(""))
            return false;
        
        //Now iterate over all UP connections and see how many agree
        Iterator iter = connManager.getConnections().iterator();
        int count = 0;
        while(iter.hasNext()) { 
            Connection c = (Connection)iter.next();
            String v = c.getVersion();
            if(version.equals(v))
                count++;
        }
        return (count>=3);
    }


    public byte[] getMessageBytes() throws IllegalStateException {
        if(xmlMessage==null)
            throw new IllegalStateException();
        return xmlMessage;
    }
}
