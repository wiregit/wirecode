package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * A Gnutella ping message.
 */

public class PingRequest extends Message {
    /**
     * With the Big Ping and Big Pong extensions pings may have a payload
     */
    private byte[] payload = null;
    
    /**
     * The GGEP blocks carried in this ping - parsed when necessary
     */
    private GGEP [] _ggeps;
    
    /////////////////Constructors for incoming messages/////////////////
    /**
     * Creates a normal ping from data read on the network
     */
    public PingRequest(byte[] guid, byte ttl, byte hops) {
        super(guid, Message.F_PING, ttl, hops, 0);
    }

    /**
     * Creates an incoming group ping. Used only by boot-strap server
     */
    protected PingRequest(byte[] guid, byte ttl, byte hops, byte length) {
        super(guid, Message.F_PING, ttl, hops, length);
    }

    /**
     * Creates a big ping request from data read from the network
     * 
     * @param payload the headers etc. which the big pings contain.
     */
    public PingRequest(byte[] guid, byte ttl, byte hops, byte[] payload) {
        super(guid, Message.F_PING, ttl, hops, payload.length);
        this.payload = payload;
    }

    //////////////////////Constructors for outgoing Pings/////////////
    /**
     * Creates a normal ping with a new GUID
     *
     * @param ttl the ttl of the new Ping
     */
    public PingRequest(byte ttl) {
        super((byte)0x0, ttl, (byte)0);
        _ggeps=new GGEP[1];
        addBasicGGEPs();
    }
    
    /**
     * Creates a normal ping with a specified GUID
     *
     * @param ttl the ttl of the new Ping
     */
    public PingRequest(byte [] guid,byte ttl) {
        super(guid,(byte)0x0, ttl, (byte)0,0);
        _ggeps=new GGEP[1];
        addBasicGGEPs();
    }

    public static PingRequest createQueryKeyRequest() {
        return new PingRequest();
    }

    /**
     * Creates a QueryKey Request ping with a new GUID and TTL of 1
     */
    private PingRequest() {
        this((byte)1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            // write the GGEP block as the payload
            GGEP ggepBlock = new GGEP(false);
            ggepBlock.put(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT);
            ggepBlock.write(baos);
            baos.write(0);
        }
        catch (IOException why) {
            ErrorService.error(why);
        }
        
		payload=baos.toByteArray();
		updateLength(payload.length); 
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

    public Message stripExtendedPayload() {
        if (payload==null)
            return this;
        else
            return new PingRequest(this.getGUID(), 
                                   this.getTTL(), 
                                   this.getHops());
    }

    public boolean isQueryKeyRequest() {
        if (!(getTTL() == 0) || !(getHops() == 1))
            return false;
        if (payload == null)
            return false;
        return parseGGEP(payload);
    }


    // handles parsing of all GGEP blocks.  may need to change return sig
    // if new things are needed....
    // TODO: technically there may be multiple GGEP blocks here - we tried to
    // get all but encountered a infinite loop so just try to get one for now.
    private final boolean parseGGEP(byte[] ggepBytes) {
        try {
            GGEP ggepBlock = new GGEP(ggepBytes, 0, null);
            if (ggepBlock.hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) 
                return true;
        }        
        catch (BadGGEPBlockException ignored) {}
        return false;
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
     * Adds all basic GGEP information to the outgoing ping.
     * Currently adds a Locale field and a
     * "I understand packed ip/ports & UDP Host Caches" field.
     */
    private void addBasicGGEPs() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            GGEP ggep = new GGEP(true);
            ggep.put(GGEP.GGEP_HEADER_CLIENT_LOCALE,
                     ApplicationSettings.LANGUAGE.getValue());
            ggep.put(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS);
                     
            _ggeps[0]=ggep;
            ggep.write(baos);
            baos.write(0);
            
            payload = baos.toByteArray();
            updateLength(payload.length);
        }
        catch(IOException e) {
            ErrorService.error(e);
        }
    }
    
    /**
     * marks this ping request as requesting a pong carrying ip:port
     * info.
     */
    public void addIPRequest() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            // Since existing clients are hard-coded to expect the locale GGEP
            // at offset 0, we add the request to that block
            if (_ggeps==null)
                _ggeps=new GGEP[1];
            if (_ggeps[0]==null)
                _ggeps[0]=new GGEP(false);
            
            _ggeps[0].put(GGEP.GGEP_HEADER_IPPORT);
            _ggeps[0].write(baos);
            baos.write(0);
            
            payload = baos.toByteArray();
            updateLength(payload.length);
        }
        catch(IOException e) {
            ErrorService.error(e);
        }
    }

    /**
     * get locale of this PingRequest 
     */
    public String getLocale() {
        if(payload != null) { //payload can be null
            try {
                
                if (_ggeps==null)
                    _ggeps = GGEP.read(payload,0);
                
                for (int i = 0;i<_ggeps.length;i++) 
                    if(_ggeps[i].hasKey(GGEP.GGEP_HEADER_CLIENT_LOCALE))
                    	return _ggeps[i].getString(GGEP.GGEP_HEADER_CLIENT_LOCALE);
                    
                
                return ApplicationSettings.DEFAULT_LOCALE.getValue();
            }
            catch(BadGGEPBlockException ignored) {}
            catch(BadGGEPPropertyException ignoredToo) {}
            return ApplicationSettings.DEFAULT_LOCALE.getValue();
        }
        else 
            return ApplicationSettings.DEFAULT_LOCALE.getValue();
    }
    
    /**
     * Determines if this PingRequest has the 'supports cached pongs'
     * marking.
     */
    public boolean supportsCachedPongs() {
        if(payload != null) {
            try {
                if(_ggeps == null)
                    _ggeps = GGEP.read(payload, 0);
                for(int i = 0; i < _ggeps.length; i++) {
                    if(_ggeps[i].hasKey(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS))
                        return true;
                }
            } catch(BadGGEPBlockException ignored) {}
        }
        return false;
    }
    
    /**
     * @return whether this ping wants a reply carrying IP:Port info.
     */
    public boolean requestsIP() {
       if (payload==null)
           return false;
       
       try{
           if (_ggeps==null)
               _ggeps = GGEP.read(payload,0);
           for (int i=0;i<_ggeps.length;i++) 
               if (_ggeps[i].hasKey(GGEP.GGEP_HEADER_IPPORT))
                   return true;
           
       }catch(BadGGEPBlockException ignored) {}
       
       return false;
       
    }
    //Unit tests: tests/com/limegroup/gnutella/messages/PingRequestTest.java
}
