package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.*;

/** The server end of the Gnutella routing benchmark.  See RouteClient. */
public class RouteServer {
    /** The number of messages to read per time quantum. */
    public static int MEASURE=100;

    private static void syntaxError() {
	System.err.println("Syntax: java com.limegroup.gnutella.tests.RouteServer "
			   +"<host> <port> <connections>");
	System.exit(1);
    }

    public static void main(String args[]) {
	try {
	    String host=args[0];
	    int port=Integer.parseInt(args[1]);
	    int connections=Integer.parseInt(args[2]);
	    if (connections<=0) syntaxError();
	    RouteServer tester=new RouteServer();
	    tester.doTest(host, port, connections);	    
	} catch (NumberFormatException e) {
	    syntaxError();
	} catch (ArrayIndexOutOfBoundsException e) {
	    syntaxError();
	} catch (IOException e) {
	    System.err.println("Connections terminated; test is not valid.");
	    System.exit(1);
	} catch (BadPacketException e) {
	    e.printStackTrace();
	    System.err.println("Got bad packet;  test is not valid.");
	    System.exit(1);
	}
    }

    public void doTest(String host, int port, int n) 
	throws IOException, BadPacketException {
	//1. Create outgoing connections.
	System.out.print("Connecting to router...");
	FakeConnection connections[]=new FakeConnection[n];
	for (int i=0; i<n; i++) {
	    connections[i]=new FakeConnection(host, port);
	    connections[i].connect();
	}
	Random random=new Random();
	StressTester.randomlyPermute(connections, random);
	
	//2. Wait for query request on each connection...	
	System.out.print("done.\nWaiting for initial query request...");
	QueryRequest request=null;
	for (int i=0; i<n; i++) {
	    while (true) {
		Message m=connections[i].receive();
		if (m instanceof QueryRequest) {
		    request=(QueryRequest)m;
		    break;
		}
	    }
	}

	//3. Now write replies as fast as possible from each of the connections.
	//   Because the connections are randomized, the replies appear to come in
	//   random order to the router.  Note that the message is only decoded
	//   once.
	System.out.println("done.\nSending replies.  Press Ctrl-C to end.");
	byte[] ip={(byte)127, (byte)0, (byte)0, 1};
	Response[] responses={new Response(0, 20, "file.mp3")};
	//A very bogus reply packet!  (Yes, that is the wrong ID at the end.)
	QueryReply reply=new QueryReply(request.getGUID(), (byte)4, 6346,
					ip, 56, responses, request.getGUID());
					
	ByteArrayOutputStream baos=new ByteArrayOutputStream();
	reply.write(baos);
	byte[] replyBytes=baos.toByteArray();
	for (int i=0; ;i=(i+1) % n) {
	    connections[i].sendRaw(replyBytes); 	    //c.send(reply);
	}	
    }

}

