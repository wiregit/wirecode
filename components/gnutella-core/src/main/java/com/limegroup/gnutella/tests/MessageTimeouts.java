package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;

/** 
 * A client that tests the ability of a servent to timeout on 
 * half-completed messages.
 */
public class MessageTimeouts {
    private static void syntaxError() {
	System.err.println("Syntax: java com.limegroup.gnutella.tests.MessageTimeouts "
			   +"<host> <port>");
	System.exit(1);
    }

    public static void main(String args[]) {
	try {
	    String host=args[0];
	    int port=Integer.parseInt(args[1]);
	    MessageTimeouts mt=new MessageTimeouts(); 
	    mt.doTest(host, port);
	} catch (NumberFormatException e) {
	    syntaxError();
	} catch (ArrayIndexOutOfBoundsException e) {
	    syntaxError();
	} catch (IOException e) {
	    System.err.println("Connections terminated; test may or may not be valid.");
	    System.exit(1);
	}
    }

    public void doTest(String host, int port) throws IOException {	
	FakeConnection c=new FakeConnection(host,port);
	c.connect();

	//1. Send normal message.
	PingRequest pr=new PingRequest((byte)1);
	c.send(pr); //send normal ping

	//2. Wait a while.
	System.out.println("Waiting 16 seconds without sending anything.\n"
			   +"I should NOT be disconnected.");
	synchronized (this) {
	    try {
		wait(16000);
	    } catch (InterruptedException e) { }
	}
	
	//3. Send half-completed ping request.
	System.out.println("Now sending bad ping. I SHOULD be disconnected.");
	PingRequest pr2=new PingRequest((byte)1);
	ByteArrayOutputStream baos=new ByteArrayOutputStream();
	pr2.write(baos);
	byte[] pr2Bytes=baos.toByteArray();
	byte[] badBytes=new byte[2];
	System.arraycopy(pr2Bytes, 0, badBytes, 0, badBytes.length);
	c.sendRaw(badBytes);
	
	System.out.println("Looping forever...");
	while (true) { }
    }	
}
