package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.net.*;
import java.io.*;
import java.util.*;

/** 
 * A Gnutella servent benchmarking program.  Tests broadcasting and
 * routing capabilities, but not store capabilities.  To reduce
 * overhead, it is implemented with only a single thread of control.<p>
 *
 * Here's how it works.  Let the Gnutella servent we are testing be S.
 * The tester first creates a single outgoing "client" connection C to
 * S.  Then it creates N outgoing "server" connections to S.  Next the
 * tester measures the time to do the following M times:<p>
 *
 * <ol> 
 * <li>Send ping request M on C
 * <li>For each "server connection" S'
 *     <ol>
 *     <li>Receive the request M from S'
 *     <li>Write ping reply M' to S'
 *     <li>Read reply M' on C'
 *     </ol> 
 * </ol> 
 *
 * In this way, we test broadcasting and routing capabilities of the
 * server S equally.  We do it in an efficient manner (no threads) to
 * ensure that the tester is not the bottleneck.  We also guarantee
 * that network buffers will not fill up.<p>
 *
 * To avoid interleaving the loop above with the server's
 * sendToAllExcept(..)  method, we actually randomly permute the
 * connection list before starting the test.  This is done in a
 * pseudorandom manner, so tests are repeatable. <p>
 *
 * Two problems with this benchmark.  First, it only tests latency, not
 * bandwidth.  Secondly, it does not allow parallelism on multiple 
 * processor machines.  Despite these drawbacks, I think it's pretty good.
 */
public class StressTester {
    public static byte TTL=(byte)4;

    private static void syntaxError() {
	System.err.println("Syntax: java com.limegroup.gnutella.tests.StressTester "
			   +"<host> <port> <threads> <messages>, where threads>1");
	System.exit(1);
    }

    static Date startTime;

    static void start() {
	startTime=new Date();
    }

    static void finish() {
	Date endTime=new Date();
	float milliseconds=(float)(endTime.getTime()-startTime.getTime());
	float seconds=milliseconds/1000f;
	System.out.println("Benchmark finished in "+seconds+" seconds.");
	System.exit(0);
    }

    static void doTest(String host, int port, int testers, int messages)
    throws IOException, BadPacketException{
	//Establish client and servers.
	Connection client=new Connection(host, port);
	client.connect();
	Connection[] servers=new Connection[testers];
	for (int i=0; i<servers.length; i++) {
	    servers[i]=new Connection(host, port);
	    servers[i].connect();
	}
	//Randomize connections.  Note this is repeatable.
	Random rand=new Random(123456);
	randomlyPermute(servers, rand);

	start();
	for (int i=0; i<messages; i++) {
	    //1. Send request.  It's always a PingRequest because that avoids the store.
	    PingRequest pr=new PingRequest(TTL);
	    byte[] guid=pr.getGUID();
	    client.send(pr);
	    
	    //2. For each server...
	    for (int j=0; j<testers; j++) {
		Connection server=servers[j];
		//a) On the server side, wait for my ping request...
		Message m=null;
		while (true) {
		    m=server.receive();
		    if (m==null) continue;
		    else if (! (m instanceof PingRequest)) continue;
		    else if (! (Arrays.equals(m.getGUID(), guid))) continue;
		    else break;
		}

		//   ...and reply.
		byte[] myIP=new byte[4]; //I'm lazy
		PingReply reply=new PingReply(m.getGUID(), TTL,
					      (byte)0, myIP, //I'm lazy
					      0, 0);
		server.send(reply);

		//b) On the client side, wait for reply to my message.
		m=null;
		while (true) {
		    m=client.receive();
		    if (m==null) continue;
		    else if (! (m instanceof PingReply)) continue;
		    else if (! (Arrays.equals(m.getGUID(), guid))) continue;
		    else break;
		}
	    }
	}
	finish();
    }

    /** 
     * Syntax: 
     * <pre>
     *      java com.limegroup.gnutella.tests.StressTester <host> <port> <N> [ <delay> ]
     * where
     *      host -- the name of the host to connect to
     *      port -- the port to connect to
     *      N -- the number of testers to instantiate. Must be at least one.
     *      M -- the number of messages to send per tester.  Must be at least 0.
     * </pre>
     */     
    public static void main(String args[]) {
	try {
	    String host=args[0];
	    int port=Integer.parseInt(args[1]);
	    int testers=Integer.parseInt(args[2]);
	    if (testers<2)
		syntaxError();
	    int messages=Integer.parseInt(args[3]);
	    if (messages<0)
		syntaxError();
	    
	    doTest(host, port, testers, messages);	    
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

    /**
     * @modifies array, rand
     * @effects randomly permutes array given the random number generator.
     */
    static void randomlyPermute(Object[] array, Random rand) {
	//Algorithm: randomly swap n items
	int n=array.length;
	for (int count=0; count<n; count++) {
	    int i=rand.nextInt(n);
	    int j=rand.nextInt(n);
	    Object tmp=array[i];
	    array[i]=array[j];
	    array[j]=tmp;
	}
    }
}
