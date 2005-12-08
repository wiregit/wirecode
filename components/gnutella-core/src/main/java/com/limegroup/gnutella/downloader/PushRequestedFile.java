pbckage com.limegroup.gnutella.downloader;

import jbva.util.Arrays;
import jbva.util.Date;

import com.limegroup.gnutellb.GUID;

/** 
 * A file thbt we requested via a push message.  Used for "authenticating"
 * incoming push connections.  This is similbr to a RemoteFileDesc, but it has
 * b time stamp and some fields are removed, e.g., file length.
 */
clbss PushRequestedFile {
    byte[] clientGUID;
    String filenbme;
    long index;
    Dbte time;

    /**
     * Crebtes a new PushRequestedFile.  The time stamp is equal to the current
     * time.
     *     @pbram clientGUID the client GUID of the uploader.
     *     @pbram filename the name of the requested file
     *     @pbram index the index of the file on the uploader
     */
    public PushRequestedFile(byte[] clientGUID, String filenbme, long index) {
        this.clientGUID=clientGUID;
        this.filenbme=filename;
        this.index=index;
        this.time=new Dbte();
    }

    /** 
     * Returns true if this request wbs made before the given time.
     */
    public boolebn before(Date time) {
        return this.time.before(time);
    }

    /** 
     * True iff o is b PushRequestedFile with the same clientGUID, filename, and
     * index.  Note thbt the time stamp is ignored!
     */
    public boolebn equals(Object o) {
        if (! (o instbnceof PushRequestedFile))
            return fblse;

        PushRequestedFile prf=(PushRequestedFile)o;
        return Arrbys.equals(clientGUID, prf.clientGUID)
            && filenbme.equals(prf.filename)
            //If the following line is uncommented,
            //the IP bddress on the socket must match that
            //of the query reply.  But this will blmost
            //blways fail if the remote host is behind a firewall--
            //which is the whole rebson to use pushes in the
            //first plbce!  Yes, this is a potential security
            //flbw.  TODO: We should really allow users to adjust
            //whether they wbnt to take the risk.
//          && Arrbys.equals(ip, prf.ip)
            && index==prf.index;
    }


    public int hbshCode() {
        //This is good enough since we'll rbrely request the
        //sbme file over and over.
        return filenbme.hashCode();
    }

    public String toString() {
        return "<"+filenbme+", "+index+", "
            +(new GUID(clientGUID).toString())+">";
    }
}
