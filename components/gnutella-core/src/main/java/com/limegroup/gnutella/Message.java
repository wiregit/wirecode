package com.limegroup.gnutella;

import java.util.Random;
import java.io.*;

/**
 * A Gnutella message (packet).  All messages have message IDs, function IDs,
 * TTLs, hops taken, and data length.  Messages come in two flavors: requests
 * (ping, search) and replies (pong, search results). Messages are mutable.
 *
 * This class is abstract; subclasses implement specific messages such as
 * search requests.
 */

//TODO2: I feel like this is a terrible way of doing things.  It's much easier
//in C because you just declare a struct and fill in the bytes. Perhaps this 
//can be optimized by keeping the raw bytes around when calling read.

//TODO1: sign problems!?  What if, for example, the number of hits N in
//a query response is 0xFF?  Will this be erroneously interpreted as negative?


public abstract class Message {       
    //Functional IDs defined by Gnutella protocol.
    protected static final byte F_PING=(byte)0x0;
    protected static final byte F_PING_REPLY=(byte)0x1;
    protected static final byte F_PUSH=(byte)0x40;
    protected static final byte F_QUERY=(byte)0x80;
    protected static final byte F_QUERY_REPLY=(byte)0x81;
    
    //Temporary code for managing GUIDs
    private static Random rand=new Random();
    private static byte[] makeGuid() {
	byte[] ret=new byte[16];
	rand.nextBytes(ret); //TODO2: not guaranteed unique
	return ret;
    }
    
    ////////////////////////// Instance Data //////////////////////

    private byte[] guid;
    private byte func; //TODO: sign problems?
    private byte ttl;
    private byte hops;
    private int length; //not necessarily the same byte order as protocol
    
    protected void repOk() {
	Assert.that(guid.length==16);
	Assert.that(func==F_PING || func==F_PING_REPLY
		    || func==F_PUSH
		    || func==F_QUERY || func==F_QUERY_REPLY);
	if (func==F_PING) Assert.that(length==0);
	if (func==F_PING_REPLY) Assert.that(length==14);
	if (func==F_PUSH) Assert.that(length==26);
    }

    ////////////////////// Constructors and Producers /////////////////
	
    /**
     * @requires func is a valid functional id (e.g., 0, 1, 64, etc),
     *  length is the valid payload length
     * @effects Creates a new message with the following data.
     *  The GUID is set appropriately, and the number of hops is set to 0.
     */
    protected Message(byte func, byte ttl, int length) {	
	this(makeGuid(), func, ttl, (byte)0, length);
    }

    /** 
     * Same as above, but caller specifies TTL and number of hops.
     */
    protected Message(byte[] guid, byte func, byte ttl, 
		      byte hops, int length) {
	this.guid=guid; this.func=func;	this.ttl=ttl; 
	this.hops=hops; this.length=length;
    }

    /** 
     * @modifies in
     * @effects reads a packet from the network and returns it as an 
     *  instance of a subclass of Message, unless one of the following happens:
     *    <ul>
     *    <li>No data is available: returns null
     *    <li>A bad packet is read: BadPacketException.  The client should be
     *      able to recover from this.
     *    <li>A major problem occurs: IOException.  The client is not expected 
     *      to recover from this.
     *    </ul>        
     */
    public static Message read(InputStream in) 
	throws BadPacketException, IOException {	
	//1. Read header bytes from network
	//TODO3: it's probably possible to optimize this by buffering	
	//TODO1: add timeouts
	byte[] buf=new byte[23]; //23=size of header (in bytes)
	for (int i=0; i<23; ) {
	    int got=in.read(buf, i, 23-i);
	    if (got==-1) throw new IOException("Connection closed.");
	    i+=got;
	}
	
	//2. Unpack.
	byte[] guid=new byte[16];
	for (int i=0; i<16; i++) //TODO3: can optimize
	    guid[i]=buf[i];
	byte func=buf[16]; 
	byte ttl=buf[17];
	byte hops=buf[18];	   
	int length=ByteOrder.leb2int(buf,19);
	if (length<0 || length>Const.MAX_LENGTH)
	    throw new IOException("Unreasonable message length: "+length);

	//3. Read rest of payload.  It's important to do this before
	//   checking other fields, so the network can recover.
	byte[] payload=null;		    
	if (length!=0) {
	    payload=new byte[length];
	    for (int i=0; i<length; i++) { //TODO3: optimize
		int got=in.read();
		if (got==-1) throw new IOException("Connection closed.");
		payload[i]=(byte)got;
	    }
	}
	
	//4. Check values.
	if (ttl<0 || ttl>Const.MAX_TTL) 
	    throw new BadPacketException("Unreasonable TTL: "+ttl);
	if (hops<0 || hops>Const.MAX_TTL) 
	    throw new BadPacketException("Unreasonable hops: "+hops);	 

	//Dispatch based on opcode. 
	//TODO1: push request, handle bad packets more robustly
	switch (func) {
	case F_PING:
	    if (length!=0) break;
	    return new PingRequest(guid,ttl,hops);
	case F_PING_REPLY:
	    if (length!=14) break;
	    return new PingReply(guid,ttl,hops,payload);
	case F_QUERY:
	    if (length<3) break;
	    return new QueryRequest(guid,ttl,hops,payload);	    
	case F_QUERY_REPLY:
	    if (length<26) break;
	    return new QueryReply(guid,ttl,hops,payload);
	case F_PUSH:
	    if (length!=26) break;
	    return new PushRequest(guid,ttl,hops,payload);
	}
	throw new BadPacketException("Unrecognized function code: "+func);
    }
	    
    /**
     * @modifies out
     * @effects Writes an encoding of this to out.  Does NOT flush out. 
     */
    public void write(OutputStream out) throws IOException {
	byte[] buf=new byte[23];
	for (int i=0; i<16; i++) //TODO3: can optimize
	    buf[i]=guid[i];
	buf[16]=func;
	buf[17]=ttl;
	buf[18]=hops;
	ByteOrder.int2leb(length, buf, 19);
	out.write(buf);
	writePayload(out);
    }
    
    /** @modifies out
     *  @effects writes the payload specific data to out (the stuff
     *   following the header).  Does NOT flush out.
     */
    protected abstract void writePayload(OutputStream out) throws IOException;


    ////////////////////////////////////////////////////////////////////

    public byte[] getGUID() {
	return guid;
    }

    public byte getFunc() {
	return func;
    }
	
    public byte getTTL() {
	return ttl;
    }
    
    public byte getHops() {
	return hops;
    }

    public int getLength() {
	return length;
    }

    /** Returns the ip (in LITTLE-endian) format as standard
     *  dotted-decimal, e.g., 192.168.0.1 */
    protected static String ip2string(byte[] ip) {
	StringBuffer buf=new StringBuffer();
	buf.append(ByteOrder.ubyte2int(ip[3])+".");
	buf.append(ByteOrder.ubyte2int(ip[2])+".");
	buf.append(ByteOrder.ubyte2int(ip[1])+".");
	buf.append(ByteOrder.ubyte2int(ip[3])+"");
	return buf.toString();
    }

    /**
     * A convenience routine: returns true iff this should be forwarded
     * to other servents.
     */
    public boolean isRequest() {
	return func==F_PING || func==F_PUSH || func==F_QUERY;
    }

    /** @modifies: this
     *  @effects: increments hops, decrements TTL, and returns the
     *   OLD value of TTL.
     */
    public byte hop() {
	hops++;
	return ttl--;
    }

    public String toString() {
	return "{guid="+(new GUID(guid)).toString()+", ttl="+ttl+"}";
    }    
}
