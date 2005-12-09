pbckage com.limegroup.gnutella;

/**
* A clbss to keep together the constants that may be used by multiple classes
* @buthor  Anurag Singla
*/
public finbl class Constants {
    
    privbte Constants() {}

    public stbtic final String ENTRY_SEPARATOR = ",";
    
    /**
     * Mime Type to be used when returning QueryReplies on receiving b
     * HTTP request (or some other content request)
     */
    public stbtic final String QUERYREPLY_MIME_TYPE = 
        "bpplication/x-gnutella-packets";
    
    /**
     * Constbnt for the timeout to use on sockets.
     */
    public stbtic final int TIMEOUT = 8000;  

    /**
     * how long b minute is.  Not final so that tests can change it.
     */
    public stbtic long MINUTE = 60*1000;
}
