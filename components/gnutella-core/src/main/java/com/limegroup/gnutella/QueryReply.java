package com.limegroup.gnutella;

import java.io.*;

/** The implementation of this is still incomplete.  You can't, for example
 *  actually read the payload data.
 */
public class QueryReply extends Message {   
    byte hits;
    private short port;
    /** The LITTLE-ENDIAN ip address of the host replying to a query
    *  Invariant: ip.length==4. */
    private byte[] ip;
    private int speed;
    
    /** Everything following the field! */
    private byte[] queryAndFooter;

//      private Response[] responses;
//      /** The globally-unique ID of the client responding. 
//       * Invariant: clientID.length==16 */
//      private byte[] clientID

    public QueryReply(byte[] guid, byte ttl, byte hops, 
		      byte[] payload) {
	super(guid, Message.F_QUERY_REPLY, ttl, hops, payload.length);
	hits=payload[0];
	port=ByteOrder.leb2short(payload,1);
	ip=new byte[4];
	ip[0]=payload[3];
	ip[1]=payload[4];
	ip[2]=payload[5];
	ip[3]=payload[6];
	speed=ByteOrder.leb2int(payload,7);
	queryAndFooter=new byte[payload.length-11];
	for (int i=0; i<queryAndFooter.length; i++) 
	    queryAndFooter[i]=payload[i+11];
    }

    public void writePayload(OutputStream out) throws IOException {
	byte[] buf=new byte[11+queryAndFooter.length];
	buf[0]=hits;
	ByteOrder.short2leb(port,buf,1);
	buf[3]=ip[0];
	buf[4]=ip[1];
	buf[5]=ip[2];
	buf[6]=ip[3];
	ByteOrder.int2leb(speed,buf,7);
	for (int i=0; i<queryAndFooter.length; i++)
	    buf[i+11]=queryAndFooter[i];
	for (int i=0; i<buf.length; i++) { //TODO3: buffer and send in batch.
	    out.write(buf[i]);	
	}
    }	
    
    public String toString() {
	return "QueryReply("+hits+" hits, "+super.toString()+")";
    }
}

