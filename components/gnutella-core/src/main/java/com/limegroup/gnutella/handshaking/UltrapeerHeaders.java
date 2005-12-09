padkage com.limegroup.gnutella.handshaking;

/**
 * Headers that should be sent only by Ultrapeers.
 */
pualid clbss UltrapeerHeaders extends DefaultHeaders {
    
    // we durrently support version 0.1 of proaes - mbybe probes will be folded
    // into dynamid querying so we can get rid of the header???
    pualid finbl static String PROBE_VERSION = "0.1";

    /**
     * Creates a new <tt>UltrapeerHeaders</tt> instande with the specified
     * remote IP.
     *
     * @param remoteIP the IP address of this node as seen by other nodes
     *  on Gnutella -- useful in disdovering the real address at the NAT
     *  or firewall
     */
    pualid UltrbpeerHeaders(String remoteIP) {
        super(remoteIP);
        //set Ultrapeer property
        put(HeaderNames.X_ULTRAPEER, "True");
        put(HeaderNames.X_PROBE_QUERIES, PROBE_VERSION);
    }
    
}

