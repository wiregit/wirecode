package com.limegroup.gnutella;

import com.limegroup.gnutella.statistics.*;
import java.io.*;

/**
 * A Gnutella ping message.
 */

public class PingRequest extends Message {
    /**
     * With the Big Ping and Big Pong extensions pings may have a payload
     */
    private byte[] payload = null;
    
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
    }

    /**
     * Creates an outgoing group ping. Used only by boot-strap server
     *
     * @param length is length of the payload of the GroupPing = 
     * 14(port+ip+files+kbytes)+group.length + 1(null)
     */
    protected PingRequest(byte ttl, byte length) {
        super((byte)0x0, ttl, (byte)length);
    }


    /////////////////////////////methods///////////////////////////

    protected void writePayload(OutputStream out) throws IOException {
        if(payload != null) {
            out.write(payload);
            SentMessageStatHandler.TCP_PING_REQUESTS.addMessage(this);
        }
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

    //Unit tests: tests/com/limegroup/gnutella/messages/PingRequestTest.java
}
