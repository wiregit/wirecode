package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.net.*;
import java.io.*;
import java.util.*;

/** Tests whether a Gnutella client has anti-spam technology. */
public class SpamTester {
    private static void syntaxError() {
	System.err.println("Syntax: java com.limegroup.gnutella.tests.SpamTester "
			   +"<host> <port>, where threads>1");
	System.exit(1);
    }

    public static void main(String args[]) {
	try {
	    String host=args[0];
	    int port=Integer.parseInt(args[1]);
	    doTest(host, port);
	} catch (NumberFormatException e) {
	    syntaxError();
	} catch (ArrayIndexOutOfBoundsException e) {
	    syntaxError();
	} catch (IOException e) {
	    System.out.println("Connections terminated; test may or may not be valid.");	    
	} catch (BadPacketException e) {
	    Assert.that(false, "Got bad packet.");
	}
    }

    public static void doTest(String host, int port) 
	throws IOException, BadPacketException {
	Connection c1=new Connection(host, port); c1.connect();
	Connection c2=new Connection(host, port); c2.connect();
	PingRequest pr=null;

	/* Does it drop spam? */
	pr=new PingRequest((byte)51);
	c1.send(pr);
	for (int i=0; i<10; i++) {
	    Message m=null;
	    try {
		m=c2.receive(200);
	    } catch (InterruptedIOException e) {
		continue;
	    }
	    if (Arrays.equals(m.getGUID(), pr.getGUID()))
		Assert.that(m.getTTL()<16, "Servent broadcasts unreasonable TTL of "+m.getTTL());
	}
	
	/* Does it adjust high (but not ridiculously so) TTLS. */
	pr=new PingRequest((byte)16);
	c1.send(pr);
	for (int i=0; i<10; i++) {
	    Message m=null;
	    try {
		m=c2.receive(200);
	    } catch (InterruptedIOException e) {
		continue;
	    }
	    if (Arrays.equals(m.getGUID(), pr.getGUID())) {
		Assert.that(m.getHops()+m.getTTL()<16);
	    }
	}		
    }
}
