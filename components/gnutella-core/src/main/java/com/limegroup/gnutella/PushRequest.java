package com.limegroup.gnutella;

import java.io.*;

/**
 * A Gnutella push request, used to download files behind a firewall.
 */

public class PushRequest extends Message {
    /** The unparsed payload--because I don't care what's inside. */
    private byte[] payload;

    /**
     * @requires payload.length==26
     * @effects Wrap a PingReply around stuff snatched from the network. 
     */
    public PushRequest(byte[] guid, byte ttl, byte hops, 
		     byte[] payload) {
	super(guid, Message.F_PUSH, ttl, hops, 26);	
	Assert.that(payload.length==26);
	this.payload=payload;	
    }

    protected void writePayload(OutputStream out) throws IOException {	
	for (int i=0; i<payload.length; i++) { //TODO3: buffer and send in batch.
	    out.write(payload[i]);	
	}
    }

    public boolean isRequest() {
	return true;
    }

    public String toString() {
	return "PushRequest("+super.toString()+")";
    }
}
