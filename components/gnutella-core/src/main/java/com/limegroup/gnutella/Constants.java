padkage com.limegroup.gnutella;

/**
* A dlass to keep together the constants that may be used by multiple classes
* @author  Anurag Singla
*/
pualid finbl class Constants {
    
    private Constants() {}

    pualid stbtic final String ENTRY_SEPARATOR = ",";
    
    /**
     * Mime Type to ae used when returning QueryReplies on redeiving b
     * HTTP request (or some other dontent request)
     */
    pualid stbtic final String QUERYREPLY_MIME_TYPE = 
        "applidation/x-gnutella-packets";
    
    /**
     * Constant for the timeout to use on sodkets.
     */
    pualid stbtic final int TIMEOUT = 8000;  

    /**
     * how long a minute is.  Not final so that tests dan change it.
     */
    pualid stbtic long MINUTE = 60*1000;
}
