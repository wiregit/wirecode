package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;

/**
 * Tests whether a Gnutella client routes properly.
 */
public class RoutingTester {
    /**  c1 -> host:port <- c2 */
    static String host="localhost";
    //static String host="192.168.0.45";
    static int port=6346;
    static Connection c1;
    static Connection c2;
    
    static int timeout=5000;

    public static void main(String args[]) {
        try {
            //It's necessary to make a new connection each time because
            //of the contract of Connection.receive(timeout).
            configure();
            testRouteHighTTL(new PingRequest((byte)2));
            configure();
            testRouteHighTTL(new QueryRequest((byte)2, 0, "*.*"));
            configure();
            testResponseWithoutForward(new PingRequest((byte)1));
            configure();
            testResponseWithoutForward(new QueryRequest((byte)1, 0, "*.*"));
            configure();
            testResponseWithoutForward(new PingRequest((byte)0));
            configure();
            testResponseWithoutForward(new QueryRequest((byte)0, 0, "*.*"));

            System.out.println("All tests passed.");
        } catch (IOException e) {
            System.out.println("Couldn't run test.");
            e.printStackTrace();
        } catch (Throwable e) {
            System.out.println("Test failed.");
            e.printStackTrace();
        }
    }

    public static void configure() throws IOException {
        if (c1==null || c2==null) {
            System.out.println("Make sure you're running a client on "+host+" that is:");
            System.out.println("  +listening on port "+port);
            System.out.println("  +has no spam filters enabled");
            System.out.println("  +has a reasonably high max TTL");
            System.out.println("  +sharing at least one file");
            System.out.println("This may take a few seconds...");
        } else {
            c1.shutdown();
            c2.shutdown();
        }

        c1=new Connection(host, port);
        c1.connect();
        c2=new Connection(host, port);
        c2.connect();

        //Need to give host a bit of time to add c1 and c2, or they may not
        //broadcast properly right away.  (This took me a while to figure out!)
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) { }
    }

    public static void testRouteHighTTL(Message m) throws IOException {
        //Check that message is forwarded to c2 and a response is sent from c1.
        //Note that we don't look at the contents of the returned messages; we
        //probably should.
        //System.out.println("-----------------------------------");
        //System.out.println("\nSending "+m.toString()+" on c1");
        c1.send(m);
        c1.flush();

        try {
            Message m2;
            m2=c1.receive(timeout);
            //System.out.println("  Read from c1: "+m2.toString());            
            Assert.that(m2!=null);

            m2=c2.receive(timeout);            
            //System.out.println("  Read from c2: "+m2.toString());
            Assert.that(m2!=null);
        } catch (InterruptedIOException e) {
            Assert.that(false);
        } catch (BadPacketException e) {
            Assert.that(false);
        }        

        //System.out.println("\nSending same on c1");
        //Check that duplicate is dropped.
        c1.send(m);
        c1.flush();
        try {
            Message m2=c1.receive(timeout);
            //System.out.println("  Read from c1: "+m2.toString());
            Assert.that(false);
        } catch (InterruptedIOException e) {
            //System.out.println("  Read from c1: NOTHING");
        } catch (BadPacketException e) {
            Assert.that(false);
        }
        try {
            Message m2=c2.receive(timeout);
            //System.out.println("  Read from c2: "+m2.toString());
            Assert.that(false);
        } catch (InterruptedIOException e) {
            //System.out.println("  Read from c2: NOTHING");
        } catch (BadPacketException e) {
            Assert.that(false);
        }
    }

    public static void testResponseWithoutForward(Message m) throws IOException {
        //Check that a response is sent from c1...
        c1.send(m);
        c1.flush();
        try {
            Assert.that(c1.receive(timeout)!=null);
        } catch (InterruptedIOException e) {
            Assert.that(false);
        } catch (BadPacketException e) {
            Assert.that(false);
        }
        //...but was not forwarded to c2.
        try {
            c2.receive(timeout);
            Assert.that(false);
        } catch (InterruptedIOException e) {
        } catch (BadPacketException e) {
            Assert.that(false);
        }
    }
}
