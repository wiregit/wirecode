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
        if(index == other.getIndex() &&
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

    ////////////////////////////Unit Test///////////////////////////
    
    public static void main(String[] args) {
        byte[] guid1 = GUID.makeGuid();
        byte[] guid2 = GUID.makeGuid();
        MiniRemoteFileDesc m1 = new MiniRemoteFileDesc("a.txt", 12, guid1);
        MiniRemoteFileDesc m2 = new MiniRemoteFileDesc("b.txt", 13, guid2);
        MiniRemoteFileDesc m3 = new MiniRemoteFileDesc("b.txt", 12, guid2);
        Assert.that(!m1.equals(m2),"different MFDs equal");
        Assert.that(!m2.equals(m3),"equals ignoring index");
        Assert.that(m2.hashCode()== m3.hashCode(),"hashcode broken");
        Assert.that(m1.hashCode()!= m3.hashCode(),"hashcode broken");
        m3 = new MiniRemoteFileDesc("a.txt",12,guid1);
        Assert.that(m1.equals(m3),"equals method broken");
        HashMap map = new HashMap();
        Object o = new Object();
        map.put(m1,o);
        Object o1 = map.get(m3);
        Assert.that(o1==o,"equals or hashcode broken");        
    }
}
