package com.limegroup.gnutella;

import java.io.*;

public class QueryRequest extends Message {    
    private short minSpeed;
    /** The query string itself, WITHOUT null terminator. */
    private byte[] query;

    /** Build a new query from scratch */
    public QueryRequest(byte ttl, short minSpeed, byte[] query) {
	//2 bytes for min speed plus query string and null terminator
	super(Message.F_QUERY, ttl, 2+query.length+1);
	this.minSpeed=minSpeed;
	int n=query.length+1;
	this.query=query;
    }

    /*
     * @effects Build a new query with given data
     */
    public QueryRequest(byte[] guid, byte ttl, byte hops,
			byte[] payload) {
	super(guid, Message.F_QUERY, ttl, hops, payload.length);
	this.minSpeed=ByteOrder.leb2short(payload,0);
	int n=payload.length-3;
	this.query=new byte[n];
	for (int i=0; i<n; i++)
	    this.query[i]=payload[i+2];
    }

    protected void writePayload(OutputStream out) throws IOException {
	byte[] buf=new byte[2+query.length+1];
	ByteOrder.short2leb(minSpeed, buf, 0);
	for (int i=0; i<query.length; i++)
	    buf[i+2]=query[i];
	buf[buf.length-1]=(byte)0; //null terminate
	for (int i=0; i<buf.length; i++) { //TODO3: buffer and send in batch.
	    out.write(buf[i]);	
	}
    }
    
    public boolean isRequest() {
	return true;
    }

    public String toString() {
	return "QueryRequest("+new String(query)+", "+super.toString()+")";
    }
}
