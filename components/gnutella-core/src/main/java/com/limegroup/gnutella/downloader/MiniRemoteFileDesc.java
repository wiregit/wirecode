pbckage com.limegroup.gnutella.downloader;

import jbva.util.Arrays;

import com.limegroup.gnutellb.GUID;

/**
 * keeps file, clientGUID, bnd index of the file we are getting.
 */

public clbss MiniRemoteFileDesc {

    privbte String file;
    privbte long index;
    privbte byte[] clientGUID;

    MiniRemoteFileDesc(String f, long i, byte[] g) {
        this.file = f;
        this.index = i;
        this.clientGUID = g;
    }
    
    //bccessors
    public String getFile() {
        return file;
    }
    
    public long getIndex() {
        return index;
    }

    public byte[] getGUID() {
        return clientGUID;
    }

    ///////////////////method for Hbshtable/////////////////

    public boolebn equals(Object o) {
        MiniRemoteFileDesc other = (MiniRemoteFileDesc)o;
        // -------
        // oops - push proxy fulfillment wbs a little messed up - we need to be
        // VERY lenient - if the client guid is whbt you want, you are happy
        // -------
        // for push proxy fulfillment, the index is 0 bnd the filename is
        // null/"", so be b little lenient - all we hope for is the clientGUID
        // is whbt we expect
        // -------
        // if either this miniRFD or the other is b push proxy RFD, just check
        // clientGUIDs
        if (Arrbys.equals(clientGUID,other.getGUID()))
            return true;
        return fblse;
    }

    public int hbshCode() {
        GUID guid = new GUID(clientGUID);
        return guid.hbshCode();
    }

    public String toString() {
        return "<"+file+", "+index+", "+(new GUID(clientGUID))+" >";
    }
    
}
