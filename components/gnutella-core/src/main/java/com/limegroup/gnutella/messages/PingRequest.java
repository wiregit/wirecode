package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.LinkedList;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.DroppedSentMessageStatHandler;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.NameValue;

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
        addBasicGGEPs();
    }
    
    /**
     * Creates a normal ping with a specified GUID
     *
     * @param ttl the ttl of the new Ping
     */
    public PingRequest(byte [] guid,byte ttl) {
        super(guid,(byte)0x0, ttl, (byte)0,0);
        addBasicGGEPs();
    }
    
    /**
     * Creates a ping with the specified GUID, ttl, and GGEP fields.
     */
    private PingRequest(byte[] guid, byte ttl, List /* of NameValue */ ggeps) {
        super(guid, (byte)0x0, ttl, (byte)0, 0);
        addGGEPs(ggeps);
    }

    public static PingRequest createQueryKeyRequest() {
        List l = new LinkedList();
        l.add(new NameValue(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT));
        return new PingRequest(GUID.makeGuid(), (byte)1, l);
    }
    
    public static PingRequest createUDPPing() {
        GUID guid;
        List l = new LinkedList();
        if(ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue()) {
            guid = new GUID();
        } else {
            l.add(new NameValue(GGEP.GGEP_HEADER_IPPORT));
            guid = UDPService.instance().getSolicitedGUID();
        }
        byte[] data = new byte[1];
        if(RouterService.isSupernode())
            data[0] = 0x1;
        else
            data[0] = 0x0;
        l.add(new NameValue(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS, data));
        return new PingRequest(guid.bytes(), (byte)1, l);
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
        List l = new LinkedList();
        l.add(new NameValue(GGEP.GGEP_HEADER_IPPORT));
        addGGEPs(l);
    }

    /**
     * Adds all basic GGEP information to the outgoing ping.
     * Currently adds a Locale field.
     */
    private void addBasicGGEPs() {
        List l = new LinkedList();
        l.add(new NameValue(GGEP.GGEP_HEADER_CLIENT_LOCALE, 
                            ApplicationSettings.LANGUAGE.getValue()));
        addGGEPs(l);
    }
    
    /**
     * Adds the specified GGEPs.
     */
     private void addGGEPs(List /* of NameValue */ ggeps) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            if (_ggeps==null)
                _ggeps=new GGEP[1];
            if (_ggeps[0]==null)
                _ggeps[0]=new GGEP(true);

            _ggeps[0].putAll(ggeps);
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
     * Gets the data value for the SCP field, if one exists.
     * If none exist, null is returned.  Else, a byte[] of some
     * size is returned.
    */
    public byte[] getSupportsCachedPongData() {
        if(payload == null)
            return null;
        
        try {
            if(_ggeps == null)
                _ggeps = GGEP.read(payload, 0);
            for(int i = 0; i < _ggeps.length; i++) {
                if(_ggeps[i].hasKey(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS)) {
                    byte[] data = _ggeps[i].getBytes(GGEP.GGEP_HEADER_SUPPORT_CACHE_PONGS);
                    if(data == null)
                        return DataUtils.EMPTY_BYTE_ARRAY;
                    else
                        return data;
                }
            }
        }
        catch(BadGGEPBlockException ignored) {}
        catch(BadGGEPPropertyException ignored) {}
        return null;
    }

    public boolean isQueryKeyRequest() {
        if (!(getTTL() == 0) || !(getHops() == 1))
            return false;

        if (payload == null)
            return false;

        try {
            if(_ggeps == null)
                _ggeps = GGEP.read(payload, 0);
            for(int i = 0; i < _ggeps.length; i++)
                if (_ggeps[i].hasKey(GGEP.GGEP_HEADER_QUERY_KEY_SUPPORT)) 
                    return true;
        } catch (BadGGEPBlockException ignored) {}
        
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
