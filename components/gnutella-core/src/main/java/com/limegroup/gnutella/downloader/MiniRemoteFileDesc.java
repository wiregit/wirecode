package com.limegroup.gnutella.downloader;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;

/**
 * keeps file, clientGUID, and index of the file we are getting.
 */

public class MiniRemoteFileDesc {

    private String file;
    private long index;
    private byte[] clientGUID;

    MiniRemoteFileDesc(String f, long i, byte[] g) {
        this.file = f;
        this.index = i;
        this.clientGUID = g;
    }
    
    //accessors
    public String getFile() {
        return file;
    }
    
    public long getIndex() {
        return index;
    }

    public byte[] getGUID() {
        return clientGUID;
    }

    ///////////////////method for Hashtable/////////////////

    public boolean equals(Object o) {
        MiniRemoteFileDesc other = (MiniRemoteFileDesc)o;
        // for push proxy fulfillment, the index is 0 and the filename is
        // null/"", so be a little lenient - all we hope for is the clientGUID
        // is what we expect
        // if either this miniRFD or the other is a push proxy RFD, just check
        // clientGUIDs
        if((
            ((other.getIndex() == 0) && 
             ((other.getFile() == null) || (other.getFile().equals("")))) ||
            ((getIndex() == 0) && 
             ((getFile() == null) || (getFile().equals(""))))
            ) &&
           Arrays.equals(clientGUID,other.getGUID()))
            return true;
        else if(index == other.getIndex() &&
                file.equals(other.getFile()) &&           
                Arrays.equals(clientGUID,other.getGUID())
                )
            return true;
        return false;
    }

    public int hashCode() {
        GUID guid = new GUID(clientGUID);
        return guid.hashCode();
    }

    public String toString() {
        return "<"+file+", "+index+", "+(new GUID(clientGUID))+" >";
    }
    
}
