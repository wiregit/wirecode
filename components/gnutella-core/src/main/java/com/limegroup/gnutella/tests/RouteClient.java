package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.*;

/** 
 * The client part of a benchmark for testing Gnutella routing speed.  See
 * also RouteServer.<p>
 *
 * This benchmark is designed to test the ability of a Gnutella servent
 * to read replies from many connections and route them to a single
 * connection.  The output is the number of messages the servent can route
 * per sec.  The assumption is that the testers (RouteServer and RouteClient)
 * are much faster than the server, and the network bandwidth is not
 * a limiting factor.<p>
 *
 * The benchmark works as follows.  First you connect RouteServer to the
 * servent to test.  For example:
 * <pre>
 *     java com.limegroup.gnutella.tests.RouteServer lime28 6347 10
 * </pre>
 *
 * This will make 10 connections between the RouteServer and the servent.
 * If working correctly, the RouteServer will print something like:
 * <pre>
 *    Connecting to router...done.
 *    Waiting for initial query request...
 * </pre>
 *
 * Now you connect a RouteClient to the same servent.  For example:
 * <pre>
 *    java com.limegroup.gnutella.tests.RouteClient lime28 6347
 * </pre>
 * 
 * This will make a single connection between the RouteClient and the servent.
 * If all is working correctly, the RouteClient will send a query request
 * to the servent, and the servent will forward it to each of the RouteServer
 * connections.  Then the RouteServer will send repeated replies from each
 * of its connections as fast as it can.  The servent should route these to
 * the RouteClient connection.  The RouteClient reads these replies as fast as
 * it can and prints the time to read them.
 */
public class RouteClient {
    /** The number of messages to read per time quantum. */
    public static final int MEASURE=200;
    /** A string highly unlikely to match on the tester. */
    public static final String QUERY_STRING="alskdjfloqa";

    private static void syntaxError() {
	System.err.println("Syntax: java com.limegroup.gnutella.tests.RouteClient "
			   +"<host> <port> [- nopings]");
	System.exit(1);
    }

    public static void main(String args[]) {
	try {
	    String host=args[0];
	    int port=Integer.parseInt(args[1]);
	    RouteClient rc=new RouteClient();
	    if (args.length==3) {
		if (! args[2].equals("-nopings"))
		    syntaxError();
		rc.doTest(host, port, true);
	    } else {
		rc.doTest(host, port, false);
	    }
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

    public void doTest(String host, int port, boolean nopings) 
	throws IOException, BadPacketException {
	//1. Create outgoing connection.
	System.out.print("Connecting to router...");
	FakeConnection c=new FakeConnection(host, port);
	c.connect();
	
	//2. Send initial query request.  Wait for query reply.
	System.out.print("done.\nSending initial query request "
			 +"and waiting for reply...");
	QueryRequest qr=new QueryRequest((byte)4,0,QUERY_STRING);
	c.send(qr);
	while (true) {
	    Message m=c.receive();
	    if ((m instanceof QueryReply)
		&& Arrays.equals(m.getGUID(), qr.getGUID()))
		break;
	}


	//3. Now read replies as fast as possible (without checking what
	//   they are) and measure the message bandwidth.
	if (nopings) {
	    System.out.println("done.\nReading replies quickly.  "
			       +"Press Ctrl-C to end.");
	    while (true) {
		Date startTime=new Date();
		for (int i=0; i<MEASURE; i++) {
		    //23 is size of header. cf below
		    //c.receiveRaw(23+RouteServer.REPLY_PAYLOAD_SIZE);
		    c.receive();
		}
		Date endTime=new Date();
		float milliseconds=(float)(endTime.getTime()-startTime.getTime());
		report(MEASURE, milliseconds);
	    }	
	} else {
	    System.out.println("done.\nReading replies carefully.  "
			       +"Press Ctrl-C to end.");
	    while (true) {
		Date startTime=new Date();
		for (int i=0; i<MEASURE; i++) {
		    c.receive();  //cf above
		}
		Date endTime=new Date();
		float milliseconds=(float)(endTime.getTime()-startTime.getTime());
		report(MEASURE, milliseconds);
	    }	
	}
    }

    protected void report(int messages, float milliseconds) {
	float bandwidth=(float)messages/milliseconds*1000.f;
	System.out.println("Reply bandwidth: "+bandwidth+" replies/sec");
    }
}
