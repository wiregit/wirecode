package com.limegroup.gnutella;

import java.io.*;

/**
 * A Gnutella ping reply, aka, "pong".
 */

public class PingReply extends Message {
    private short port;
    /** The LITTLE-ENDIAN ip address of the host replying to a ping.
     *  Invariant: ip.length==4. */
    private byte[] ip;
    private int files;
    private int kbytes;
    
    /** Create a new PingReply from scratch */
    public PingReply(byte[] guid, byte ttl, 
		     short port, byte[] ip, int files, int kbytes) {
	super(guid, Message.F_PING_REPLY, ttl, (byte)0, 14);
	this.port=port;
	this.ip=ip;
	this.files=files;
	this.kbytes=kbytes;
    }

    /** Wrap a PingReply around stuff snatched from the network. */
    public PingReply(byte[] guid, byte ttl, byte hops, 
		     byte[] payload) {
	super(guid, Message.F_PING_REPLY, ttl, hops, 14);
	//TODO1: Is port, file count, and file size really little-endian?
	this.port=ByteOrder.leb2short(payload,0);
	this.ip=new byte[4];
	this.ip[0]=payload[2];
	this.ip[1]=payload[3];
	this.ip[2]=payload[4];
	this.ip[3]=payload[5];
	this.files=ByteOrder.leb2int(payload,6);
	this.kbytes=ByteOrder.leb2int(payload,10);
    }

    protected void writePayload(OutputStream out) throws IOException {	
	//TODO1: Is port, file count, and file size really little-endian?
	byte[] buf=new byte[14];
	ByteOrder.short2leb(port, buf, 0);
	buf[2]=ip[0];
	buf[3]=ip[1];
	buf[4]=ip[2];
	buf[5]=ip[3];
	ByteOrder.int2leb(files, buf, 6);
	ByteOrder.int2leb(kbytes, buf, 10);
	for (int i=0; i<14; i++) { //TODO3: buffer and send in batch.
	    out.write(buf[i]);	
	}
    }

    public String toString() {
	return "PingReply("+super.toString()+")";
    }

    /** 
     * Returns the ip field in dotted decimal format, e.g., 
     * "127.0.0.1".
     */
    public String getIP() {
	return ip2string(ip);
    }

    public int getFiles() {
	return files;
    }
    
    public int getKbytes() {
	return kbytes;
    }
}
