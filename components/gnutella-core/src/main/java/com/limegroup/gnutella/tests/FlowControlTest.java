package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.routing.*;
import java.util.Properties;
import com.sun.java.util.collections.*;
import java.io.*;

/**
 * Out-of-process test to check whether nodes implement the SACHRIFC flow
 * control algorithm properly.  Complements unit test of ManagedConnection.<p>
 *
 * These tests are somewhat fragile and subjective.  The difficulty comes from
 * trying to fill up a Socket's send buffer 
 */
public class FlowControlTest {
    private static String address;
    private static int port;
    private static Connection c1;
    private static Connection c2;

    public static void main(String args[]) {
        try {
            address=args[0];
            port=Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.out.println("Syntax: FlowControlTest <address> <port>");
            System.exit(1);
        }
        
        System.out.println(
            "Please make sure your client is listening on port "+port+"\n"
            +"of "+address+" and with at least two incoming messaging slots\n"
            +"and connection watchdogs disabled.  You may want to turn off\n"
            +"filters and unshare all files for performance.\n");        

        try {
            connect();
            testRequestLIFO();
        } catch (Exception e) {            
            System.err.println("Unexpected exception:");
            e.printStackTrace();
        } finally {
            disconnect();
        }

        try {
            connect();
            testReplyPriorities();
        } catch (Exception e) {            
            System.err.println("Unexpected exception:");
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }


    /////////////////////////////////// Actual Tests //////////////////////////

    private static void testRequestLIFO() 
            throws IOException, BadPacketException {
        startTest("Testing LIFO on broadcast to minimize latency");
        int QUERIES=10000;
        //Send huge amount of crap on c1, but don't read from c2.
        //comment("sending queries");
        String lastQuery=null;
        for (int i=0; i<QUERIES; i++) {
            lastQuery=randomString();
            c1.send(new QueryRequest((byte)5, 0, lastQuery));
            c1.flush();
        }
        
        //Now measure time until last query read from c2.        
        //comment("receiving queries");
        long start=System.currentTimeMillis();
        while (true) {
            Message m=c2.receive(500);
            if ((m instanceof QueryRequest) 
                    && ((QueryRequest)m).getQuery().equals(lastQuery))
                break;                
        }
        long stop=System.currentTimeMillis();
        long elapsed=stop-start;
        endTest(elapsed<2000);  //TODO: this is really a matter of opinion
        comment("elapsed time: "+elapsed);
    }            

    private static void testReplyPriorities() 
            throws IOException, BadPacketException {
        startTest("Prioritizing low volume reply");

        //Send two queries on c1
        QueryRequest q1=new QueryRequest((byte)5, 0, "abcd");
        QueryRequest q2=new QueryRequest((byte)5, 0, "efgh");        
        c1.send(q1);
        c1.send(q2);
        c1.flush();
        
        //Send a ton of replies for q1 along c2 
        for (int i=0; i<200000; i++) {
            QueryReply reply=new QueryReply(q1.getGUID(), (byte)5, 
                                            6341, new byte[4], 
                                            0, new Response[0], new byte[16]);
            c2.send(reply);
            c2.flush();
        }

        //Now send reply for q2 along c2...
        QueryReply reply=new QueryReply(q2.getGUID(), (byte)5, 
                                            6341, new byte[4], 
                                            0, new Response[0], new byte[16]);
        c2.send(reply);
        c2.flush();

        //...and measure time to receive along c1.
        long start=System.currentTimeMillis();
        while (true) {
            Message m=c1.receive(500);
            if ((m instanceof QueryReply) 
                    && Arrays.equals(q2.getGUID(), m.getGUID()))
                break;
        }
        long stop=System.currentTimeMillis();
        long elapsed=stop-start;
        endTest(elapsed<2000);  //TODO: this is really a matter of opinion

        Message m=c1.receive(500);
        Assert.that(m instanceof QueryReply);
        Assert.that(Arrays.equals(q1.getGUID(), m.getGUID()));
            
        comment("elapsed time: "+elapsed);
    }    




    //////////////////////////////// Support Code /////////////////////////

    
    private static void connect() throws IOException {
        c1=new Connection(address, port);
        c1.initialize();
        c2=new Connection(address, port);
        c2.initialize();
    }

    private static void disconnect() {
        c1.close();
        c2.close();
    }
    
    private static void comment(String msg) {
        System.out.println("    "+msg);
    }
    
    private static void startTest(String name) {
        System.out.print("-"+name+"...");
    }

    private static void endTest(boolean ok) {
        if (ok)
            System.out.println("passed.");
        else
            System.out.println("FAILED!");
    }

    private static Random rand=new Random();
    private static String randomString() {
        char[] buf=new char[rand.nextInt(100)+1];
        for (int i=0; i<buf.length; i++) 
            buf[i]=(char)('A'+rand.nextInt(52));
        return new String(buf);
    }
}
