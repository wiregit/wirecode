package com.limegroup.gnutella.downloader;

import java.util.Arrays;
import java.util.Date;

import com.limegroup.gnutella.GUID;

/** 
 * A file that we requested via a push message.  Used for "authenticating"
 * incoming push connections.  This is similar to a RemoteFileDesc, but it has
 * a time stamp and some fields are removed, e.g., file length.
 */
class PushRequestedFile {
    byte[] clientGUID;
    String filename;
    long index;
    Date time;

    /**
     * Creates a new PushRequestedFile.  The time stamp is equal to the current
     * time.
     *     @param clientGUID the client GUID of the uploader.
     *     @param filename the name of the requested file
     *     @param index the index of the file on the uploader
     */
    public PushRequestedFile(byte[] clientGUID, String filename, long index) {
        this.clientGUID=clientGUID;
        this.filename=filename;
        this.index=index;
        this.time=new Date();
    }

    /** 
     * Returns true if this request was made before the given time.
     */
    public boolean before(Date time) {
        return this.time.before(time);
    }

    /** 
     * True iff o is a PushRequestedFile with the same clientGUID, filename, and
     * index.  Note that the time stamp is ignored!
     */
    public boolean equals(Object o) {
        if (! (o instanceof PushRequestedFile))
            return false;

        PushRequestedFile prf=(PushRequestedFile)o;
        return Arrays.equals(clientGUID, prf.clientGUID)
            && filename.equals(prf.filename)
            //If the following line is uncommented,
            //the IP address on the socket must match that
            //of the query reply.  But this will almost
            //always fail if the remote host is behind a firewall--
            //which is the whole reason to use pushes in the
            //first place!  Yes, this is a potential security
            //flaw.  TODO: We should really allow users to adjust
            //whether they want to take the risk.
//          && Arrays.equals(ip, prf.ip)
            && index==prf.index;
    }


    public int hashCode() {
        //This is good enough since we'll rarely request the
        //same file over and over.
        return filename.hashCode();
    }

    public String toString() {
        return "<"+filename+", "+index+", "
            +(new GUID(clientGUID).toString())+">";
    }
}
