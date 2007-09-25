package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.limewire.collection.NameValue;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.DataUtils;

/**
 * A Gnutella ping message.
 */

public class PingRequest extends Message {

    /* various flags related to the SCP ggep field */
    /** Mask for where leaf/ultrapeer requests are. */
    public static final byte SCP_ULTRAPEER_OR_LEAF_MASK = 0x1;
    /** If we're requesting leaf hosts. */
    public static final byte SCP_LEAF = 0x0;
    /** If we're requesting ultrapeer hosts. */
    public static final byte SCP_ULTRAPEER = 0x1;
    /** If we support incoming TLS. */
    public static final byte SCP_TLS = 0x2;
    
    /**
     * GUID to send out for UDP pings.
     */
    public static final GUID UDP_GUID = new GUID();

    /**
     * With the Big Ping and Big Pong extensions pings may have a payload
     */
    private byte[] payload = null;
    
    /**
     * The GGEP blocks carried in this ping - parsed when necessary
     */
    private GGEP _ggep;
    
    /////////////////Constructors for incoming messages/////////////////
    /**
     * Creates a normal ping from data read on the network
     */
    PingRequest(byte[] guid, byte ttl, byte hops) {
        super(guid, Message.F_PING, ttl, hops, 0);
    }

    /**
     * Creates an incoming group ping. Used only by boot-strap server
     */
    PingRequest(byte[] guid, byte ttl, byte hops, byte length) {
        super(guid, Message.F_PING, ttl, hops, length);
    }

    /**
     * Creates a big ping request from data read from the network
     * 
     * @param payload the headers etc. which the big pings contain.
     */
    PingRequest(byte[] guid, byte ttl, byte hops, byte[] payload) {
        super(guid, Message.F_PING, ttl, hops, payload.length);
        this.payload = payload;
    }

    //////////////////////Constructors for outgoing Pings/////////////
    /**
     * Creates a normal ping with a new GUID
     *
     * @param ttl the ttl of the new Ping
     */
    PingRequest(byte ttl) {
        super((byte)0x0, ttl, (byte)0);
        addBasicGGEPs();
    }
    
    /**
     * Creates a normal ping with a specified GUID
     *
     * @param ttl the ttl of the new Ping
     */
    PingRequest(byte [] guid,byte ttl) {
        super(guid,(byte)0x0, ttl, (byte)0,0);
        addBasicGGEPs();
    }
    
    /**
     * Creates a ping with the specified GUID, ttl, and GGEP fields.
     */
    PingRequest(byte[] guid, byte ttl, List<NameValue<?>> ggeps) {
        super(guid, (byte)0x0, ttl, (byte)0, 0);
        addGGEPs(ggeps);
    }

    /////////////////////////////methods///////////////////////////

    protected void writePayload(OutputStream out) throws IOException {
        if(payload != null) {
            out.write(payload);
        }
        // the ping is still written even if there's no payload
        SentMessageStatHandler.TCP_PING_REQUESTS.addMessage(this);
        //Do nothing...there is no payload!
    }

	// inherit doc comment
	public void recordDrop() {
		DroppedSentMessageStatHandler.TCP_PING_REQUESTS.addMessage(this);
	}

    public String toString() {
        return "PingRequest("+super.toString()+")";
    }

    /**
     * Accessor for whether or not this ping meets the criteria for being a
     * "heartbeat" ping, namely having ttl=0 and hops=1.
     * 
     * @return <tt>true</tt> if this ping apears to be a "heartbeat" ping,
     *  otherwise <tt>false</tt>
     */
    public boolean isHeartbeat() {
        return (getHops() == 1 && getTTL() == 0);
    }
    
    /**
     * Marks this ping request as requesting a pong carrying
     * an ip:port info.
     */
    public void addIPRequest() {
        List<NameValue<?>> l = new LinkedList<NameValue<?>>();
        l.add(new NameValue(GGEP.GGEP_HEADER_IPPORT));
        addGGEPs(l);
    }

    /**
     * Adds all basic GGEP information to the outgoing ping.
     * Currently adds a Locale field.
     */
    private void addBasicGGEPs() {
        List<NameValue<?>> l = new LinkedList<NameValue<?>>();
        l.add(new NameValue<String>(GGEP.GGEP_HEADER_CLIENT_LOCALE, 
                            ApplicationSettings.LANGUAGE.getValue()));
        addGGEPs(l);
    }
    
    /**
     * Adds the specified GGEPs.
     */
     private void addGGEPs(List<? extends NameValue<?>> ggeps) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            if (_ggep == null)
                _ggep = new GGEP();

            _ggep.putAll(ggeps);
            _ggep.write(baos);
            baos.write(0);            
            payload = baos.toByteArray();
            updateLength(payload.length);
        } catch(IOException e) {
            ErrorService.error(e);
        }
    }

    /**
     * get locale of this PingRequest 
     */
    public String getLocale() {
        if(payload != null) {
            try {
                parseGGEP();
                if(_ggep.hasKey(GGEP.GGEP_HEADER_CLIENT_LOCALE))
                	return _ggep.getString(GGEP.GGEP_HEADER_CLIENT_LOCALE);
            } catch(BadGGEPBlockException ignored) {
            } catch(BadGGEPPropertyException ignoredToo) {}
        }
        
        return ApplicationSettings.DEFAULT_LOCALE.getValue();
    }
    
    /**
     * Determines if this PingRequest has the 'supports cached pongs'
     * marking.
     */
    public boolean supportsCachedPongs() {
        if(payload != null) {
            try {
                parseGGEP();
                return _ggep.hasKey(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS);
            } catch(BadGGEPBlockException ignored) {}
        }
        return false;
    }
    
    /**
     * Gets the data value for the SCP field, if one exists.
     * If none exist, null is returned.  Else, a byte[] of some
     * size is returned.
    */
    public byte[] getSupportsCachedPongData() {
        byte[] ret = null;

        if(payload != null) {
            try {
                parseGGEP();
                if(_ggep.hasKey(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS)) {
                    ret = DataUtils.EMPTY_BYTE_ARRAY;
                    // this may throw, which is why we first set it to an empty value.
                    return _ggep.getBytes(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS);
                }
            } catch(BadGGEPBlockException ignored) {
            } catch(BadGGEPPropertyException ignored) {
            }
        }

        return ret;
    }

    public boolean isQueryKeyRequest() {
        if (!(getTTL() == 0) || !(getHops() == 1))
            return false;

        if(payload != null) {
            try {
                parseGGEP();
                return _ggep.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
            } catch (BadGGEPBlockException ignored) {}
        }

        return false;
    }
    
    /**
     * @return whether this ping wants a reply carrying IP:Port info.
     */
    public boolean requestsIP() {
       if(payload != null) {
           try {
               parseGGEP();
               return _ggep.hasKey(GGEP.GGEP_HEADER_IPPORT);
           } catch(BadGGEPBlockException ignored) {}
        }

       return false;
    } 
    
    /**
     * @return whether this ping wants a reply carrying DHT IPP info
     */
    public boolean requestsDHTIPP() {
       if(payload != null) {
           try {
               parseGGEP();
               return _ggep.hasKey(GGEP.GGEP_HEADER_DHT_IPPORTS);
           } catch(BadGGEPBlockException ignored) {}
        }

       return false;
    } 
    
    private void parseGGEP() throws BadGGEPBlockException {
        if(_ggep == null)
            _ggep = new GGEP(payload, 0, null);
    }
}
