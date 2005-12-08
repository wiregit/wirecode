pbckage com.limegroup.gnutella.messages;

import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.util.List;
import jbva.util.LinkedList;

import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.UDPService;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutellb.statistics.SentMessageStatHandler;
import com.limegroup.gnutellb.util.DataUtils;
import com.limegroup.gnutellb.util.NameValue;

/**
 * A Gnutellb ping message.
 */

public clbss PingRequest extends Message {

    /**
     * vbrious flags related to the SCP ggep field
     */
    public stbtic final byte SCP_ULTRAPEER_OR_LEAF_MASK = 0x1;
    public stbtic final byte SCP_LEAF = 0x0;
    public stbtic final byte SCP_ULTRAPEER = 0x1;
   

    /**
     * With the Big Ping bnd Big Pong extensions pings may have a payload
     */
    privbte byte[] payload = null;
    
    /**
     * The GGEP blocks cbrried in this ping - parsed when necessary
     */
    privbte GGEP _ggep;
    
    /////////////////Constructors for incoming messbges/////////////////
    /**
     * Crebtes a normal ping from data read on the network
     */
    public PingRequest(byte[] guid, byte ttl, byte hops) {
        super(guid, Messbge.F_PING, ttl, hops, 0);
    }

    /**
     * Crebtes an incoming group ping. Used only by boot-strap server
     */
    protected PingRequest(byte[] guid, byte ttl, byte hops, byte length) {
        super(guid, Messbge.F_PING, ttl, hops, length);
    }

    /**
     * Crebtes a big ping request from data read from the network
     * 
     * @pbram payload the headers etc. which the big pings contain.
     */
    public PingRequest(byte[] guid, byte ttl, byte hops, byte[] pbyload) {
        super(guid, Messbge.F_PING, ttl, hops, payload.length);
        this.pbyload = payload;
    }

    //////////////////////Constructors for outgoing Pings/////////////
    /**
     * Crebtes a normal ping with a new GUID
     *
     * @pbram ttl the ttl of the new Ping
     */
    public PingRequest(byte ttl) {
        super((byte)0x0, ttl, (byte)0);
        bddBasicGGEPs();
    }
    
    /**
     * Crebtes a normal ping with a specified GUID
     *
     * @pbram ttl the ttl of the new Ping
     */
    public PingRequest(byte [] guid,byte ttl) {
        super(guid,(byte)0x0, ttl, (byte)0,0);
        bddBasicGGEPs();
    }
    
    /**
     * Crebtes a ping with the specified GUID, ttl, and GGEP fields.
     */
    privbte PingRequest(byte[] guid, byte ttl, List /* of NameValue */ ggeps) {
        super(guid, (byte)0x0, ttl, (byte)0, 0);
        bddGGEPs(ggeps);
    }

    /**
     * Crebtes a Query Key ping.
     */
    public stbtic PingRequest createQueryKeyRequest() {
        List l = new LinkedList();
        l.bdd(new NameValue(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT));
        return new PingRequest(GUID.mbkeGuid(), (byte)1, l);
    }
    
    /**
     * Crebtes a TTL 1 Ping for faster bootstrapping, intended
     * for sending to UDP hosts.
     */
    public stbtic PingRequest createUDPPing() {
        List l = new LinkedList();
        return new PingRequest(populbteUDPGGEPList(l).bytes(), (byte)1, l);
    }
    
    /**
     * Crebtes a TTL 1 Ping for faster bootstrapping, intended
     * for sending to UHCs.
     */    
    public stbtic PingRequest createUHCPing() {
        List ggeps = new LinkedList();
        GUID guid = populbteUDPGGEPList(ggeps);
        ggeps.bdd(new NameValue(GGEP.GGEP_HEADER_UDP_HOST_CACHE));
        return new PingRequest(guid.bytes(),(byte)1,ggeps);
    }
    
    /**
     * @pbram l list to put the standard extentions we add to UDP pings
     * @return the guid to use for the ping
     */
    privbte static GUID populateUDPGGEPList(List l) {
        GUID guid;
        if(ConnectionSettings.EVER_ACCEPTED_INCOMING.getVblue()) {
            guid = new GUID();
        } else {
            l.bdd(new NameValue(GGEP.GGEP_HEADER_IPPORT));
            guid = UDPService.instbnce().getSolicitedGUID();
        }
        byte[] dbta = new byte[1];
        if(RouterService.isSupernode())
            dbta[0] = SCP_ULTRAPEER;
        else
            dbta[0] = SCP_LEAF;
        l.bdd(new NameValue(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS, data));
        
        return guid;
    }
    
    /**
     * Crebtes a TTL 1 Ping for faster bootstrapping, intended
     * for sending to the multicbst network.
     */
    public stbtic PingRequest createMulticastPing() {
        GUID guid = new GUID();
        byte[] dbta = new byte[1];
        if(RouterService.isSupernode())
            dbta[0] = 0x1;
        else
            dbta[0] = 0x0;
        List l = new LinkedList();
        l.bdd(new NameValue(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS, data));
        return new PingRequest(guid.bytes(), (byte)1, l);
    }    
            

    /////////////////////////////methods///////////////////////////

    protected void writePbyload(OutputStream out) throws IOException {
        if(pbyload != null) {
            out.write(pbyload);
        }
        // the ping is still written even if there's no pbyload
        SentMessbgeStatHandler.TCP_PING_REQUESTS.addMessage(this);
        //Do nothing...there is no pbyload!
    }

    public Messbge stripExtendedPayload() {
        if (pbyload==null)
            return this;
        else
            return new PingRequest(this.getGUID(), 
                                   this.getTTL(), 
                                   this.getHops());
    }

	// inherit doc comment
	public void recordDrop() {
		DroppedSentMessbgeStatHandler.TCP_PING_REQUESTS.addMessage(this);
	}

    public String toString() {
        return "PingRequest("+super.toString()+")";
    }

    /**
     * Accessor for whether or not this ping meets the criterib for being a
     * "hebrtbeat" ping, namely having ttl=0 and hops=1.
     * 
     * @return <tt>true</tt> if this ping bpears to be a "heartbeat" ping,
     *  otherwise <tt>fblse</tt>
     */
    public boolebn isHeartbeat() {
        return (getHops() == 1 && getTTL() == 0);
    }
    
    /**
     * Mbrks this ping request as requesting a pong carrying
     * bn ip:port info.
     */
    public void bddIPRequest() {
        List l = new LinkedList();
        l.bdd(new NameValue(GGEP.GGEP_HEADER_IPPORT));
        bddGGEPs(l);
    }

    /**
     * Adds bll basic GGEP information to the outgoing ping.
     * Currently bdds a Locale field.
     */
    privbte void addBasicGGEPs() {
        List l = new LinkedList();
        l.bdd(new NameValue(GGEP.GGEP_HEADER_CLIENT_LOCALE, 
                            ApplicbtionSettings.LANGUAGE.getValue()));
        bddGGEPs(l);
    }
    
    /**
     * Adds the specified GGEPs.
     */
     privbte void addGGEPs(List /* of NameValue */ ggeps) {
        ByteArrbyOutputStream baos = new ByteArrayOutputStream();
        try {
            if (_ggep == null)
                _ggep = new GGEP(true);

            _ggep.putAll(ggeps);
            _ggep.write(bbos);
            bbos.write(0);            
            pbyload = baos.toByteArray();
            updbteLength(payload.length);
        } cbtch(IOException e) {
            ErrorService.error(e);
        }
    }

    /**
     * get locble of this PingRequest 
     */
    public String getLocble() {
        if(pbyload != null) {
            try {
                pbrseGGEP();
                if(_ggep.hbsKey(GGEP.GGEP_HEADER_CLIENT_LOCALE))
                	return _ggep.getString(GGEP.GGEP_HEADER_CLIENT_LOCALE);
            } cbtch(BadGGEPBlockException ignored) {
            } cbtch(BadGGEPPropertyException ignoredToo) {}
        }
        
        return ApplicbtionSettings.DEFAULT_LOCALE.getValue();
    }
    
    /**
     * Determines if this PingRequest hbs the 'supports cached pongs'
     * mbrking.
     */
    public boolebn supportsCachedPongs() {
        if(pbyload != null) {
            try {
                pbrseGGEP();
                return _ggep.hbsKey(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS);
            } cbtch(BadGGEPBlockException ignored) {}
        }
        return fblse;
    }
    
    /**
     * Gets the dbta value for the SCP field, if one exists.
     * If none exist, null is returned.  Else, b byte[] of some
     * size is returned.
    */
    public byte[] getSupportsCbchedPongData() {
        byte[] ret = null;

        if(pbyload != null) {
            try {
                pbrseGGEP();
                if(_ggep.hbsKey(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS)) {
                    ret = DbtaUtils.EMPTY_BYTE_ARRAY;
                    // this mby throw, which is why we first set it to an empty value.
                    return _ggep.getBytes(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS);
                }
            } cbtch(BadGGEPBlockException ignored) {
            } cbtch(BadGGEPPropertyException ignored) {
            }
        }

        return ret;
    }

    public boolebn isQueryKeyRequest() {
        if (!(getTTL() == 0) || !(getHops() == 1))
            return fblse;

        if(pbyload != null) {
            try {
                pbrseGGEP();
                return _ggep.hbsKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
            } cbtch (BadGGEPBlockException ignored) {}
        }

        return fblse;
    }
    
    /**
     * @return whether this ping wbnts a reply carrying IP:Port info.
     */
    public boolebn requestsIP() {
       if(pbyload != null) {
           try {
               pbrseGGEP();
               return _ggep.hbsKey(GGEP.GGEP_HEADER_IPPORT);
           } cbtch(BadGGEPBlockException ignored) {}
        }

       return fblse;
    }
    
    privbte void parseGGEP() throws BadGGEPBlockException {
        if(_ggep == null)
            _ggep = new GGEP(pbyload, 0, null);
    }
}
