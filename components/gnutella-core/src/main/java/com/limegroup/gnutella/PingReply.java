package com.limegroup.gnutella;

import java.io.*;

/**
 * A ping reply message, aka, "pong".
 */

public class PingReply extends Message {
    /** All the data.  We extract the port, ip address, number of files,
     *  and number of kilobytes lazily. */
    private byte[] payload;
    
    /** 
     * Create a new PingReply from scratch 
     *
     * @requires ip.length==4 and ip is in <i>big-endian</i> byte order,
     *  0&<; port &<;2^16 (i.e., can fit in 2 unsigned bytes),
     *  0&<; files and kbytes &<;2^32 (i.e., can fit in 4 unsigned bytes)
     */
    public PingReply(byte[] guid, byte ttl, 
		     int port, byte[] ip, long files, long kbytes) {
	super(guid, Message.F_PING_REPLY, ttl, (byte)0, 14);
	this.payload=new byte[14];
	//It's ok if casting port, files, or kbytes turns negative.
	ByteOrder.short2leb((short)port, payload, 0);
	//Payload stores IP in LITTLE-ENDIAN
	payload[2]=ip[3];
	payload[3]=ip[2];
	payload[4]=ip[1];
	payload[5]=ip[0];
	ByteOrder.int2leb((int)files, payload, 6);
	ByteOrder.int2leb((int)kbytes, payload, 9);
    }

    /** 
     * Wrap a PingReply around stuff snatched from the network. 
     *
     * @requires payload.length==14
     */
    public PingReply(byte[] guid, byte ttl, byte hops, 
		     byte[] payload) {
	super(guid, Message.F_PING_REPLY, ttl, hops, 14);
	this.payload=payload;
    }

    protected void writePayload(OutputStream out) throws IOException {	
	out.write(payload);
    }

    public String toString() {
	return "PingReply("+getIP()+":"+getPort()
	    +", "+super.toString()+")";
    }

    public int getPort() {
	return	ByteOrder.ubytes2int(ByteOrder.leb2short(payload,0));
    }

    /** 
     * Returns the ip field in standard dotted decimal format, e.g., 
     * "127.0.0.1".  The most significant byte is written first.
     */
    public String getIP() {
	byte[] ip=new byte[4];
	ip[0]=payload[2];
	ip[1]=payload[3];
	ip[2]=payload[4];
	ip[3]=payload[5];
	return ip2string(ip); //takes care of byte order and signs
    }

    public long getFiles() {
	return ByteOrder.ubytes2long(ByteOrder.leb2int(payload,6));
    }
    
    public long getKbytes() {
	return ByteOrder.ubytes2long(ByteOrder.leb2int(payload,10));
    }

//      /** Unit test */
//      public static void main(String args[]) {
//  	long u4=0x00000000FFFFFFFFl;
//  	int u2=0x0000FFFF;
//  	byte[] ip={(byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x01};
//  	PingReply pr=new PingReply(new byte[16], (byte)0,
//  				   u2, ip, u4, u4);
//  	Assert.that(pr.getPort()==u2);
//  	Assert.that(pr.getFiles()==u4);
//  	Assert.that(pr.getKbytes()==u4);
//  	String ip2=pr.getIP();
//  	Assert.that(ip2.equals("255.0.0.1"), ip2);
//      }
}
