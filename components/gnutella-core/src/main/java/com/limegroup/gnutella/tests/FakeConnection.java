package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;

/** 
 * A connection with blistering fast read/write routines for benchmarking
 * purposes.  (We want the testers to be able to keep up with the router!)
 * See RouteServer and RouteClient.
 */
public class FakeConnection extends Connection {
    private byte[] buf;
    

    public FakeConnection(String host, int port) {
	super(host, port);
    }

    public void sendRaw(byte[] buf) throws IOException {
	out.write(buf);
	out.flush();

	//	Assert.that(buf[16]==(byte)0x81, "Wrong func: "+buf[16]);
    }

    public void receiveRaw(int n) throws IOException {
	if (buf==null || buf.length!=n)
	    buf=new byte[n];	
    
	for (int i=0; i<n; ) {
	    int got=in.read(buf, i, n-i);
	    if (got==-1) throw new IOException("Connection closed.");
	    i+=got;
	}
	
	//	Assert.that(buf[16]==(byte)0x81, "Wrong func: "+buf[16]);
    }
}
