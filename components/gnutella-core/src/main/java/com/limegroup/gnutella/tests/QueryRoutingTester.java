package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.routing.*;
import java.io.*;
import com.sun.java.util.collections.Iterator;


/**
 * Tests whether a client handles query routing messages properly.  Like
 * RoutingTester, this is a semi-interactive test.  It requires that you
 * manually set up the client to listen on a socket, but then works
 * automatically.  The advantage is that this allows you to test any client
 * written in any language.
 */
public class QueryRoutingTester {
    public static final String HOST="localhost";
    public static final int TABLE_SIZE=1024;
    public static final int TABLE_TTLS=5;
    /** The default time to wait between propogations, in seconds. */
    public static final int DEFAULT_DELAY_SECONDS=15;
    /** Time to wait for messages, in milliseconds. */
    public static final int TIMEOUT=3000;

    public static void main(String[] args) {
        int port=0;
        int delayTime=DEFAULT_DELAY_SECONDS;
        try {
            port=Integer.parseInt(args[0]);
            if (args.length==2)
                delayTime=Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.err.println(
                "Syntax: QueryRoutingTester <port> [ <delaySeconds> ]");
            System.exit(1);
        }

        System.out.println("Please check that you have a client that:");
        System.out.println("   +Is listening on port "+port);
        System.out.println("   +Allows 3 incoming connections");
        System.out.println("   +Has no connections right now");
        System.out.println("   +Is sharing zero files");
        System.out.println("   +Will not kill connections that don't send data");
        System.out.println("I'm trying to connect now...");


        /*
         *  C1 -\
         *       TEST_CLIENT     
         */
        System.out.println("Testing one connection.  Please wait...");
        Connection c1=connect(6346);
        QueryRouteTable c1T=new QueryRouteTable(TABLE_TTLS, TABLE_SIZE);
        c1T.add("c1_0", 0);  c1T.add("c1_2", 2);
        send(c1, c1T);
        ensureReceived(c1, propogate(null, null));

        delay(delayTime);


        /*
         *  C1 -\
         *       TEST_CLIENT - C2 
         */
        System.out.println("Testing two connections.  Please wait...");
        Connection c2=connect(6346);
        QueryRouteTable c2T=new QueryRouteTable(TABLE_TTLS, TABLE_SIZE);
        c2T.add("c2_0", 0);  c2T.add("c2_2", 2);
        send(c2, c2T);
        ensureReceived(c2, propogate(c1T, null));
        ensureReceived(c1, propogate(c2T, null));

        delay(delayTime);


        /*
         *  C1 -\
         *       TEST_CLIENT - C2
         *  C3 -/
         */
        System.out.println("Testing three connections.  Please wait...");
        Connection c3=connect(6346);
        QueryRouteTable c3T=new QueryRouteTable(TABLE_TTLS, TABLE_SIZE);
        c3T.add("c3_0", 0);  c3T.add("c3_2", 2);
        send(c3, c3T);
        ensureReceived(c1, propogate(c2T, c3T));
        ensureReceived(c2, propogate(c1T, c3T));
        ensureReceived(c3, propogate(c1T, c2T));

        //TODO: test query routing
    }


    //////////////////////// Helper Methods ///////////////////////////

    private static Connection connect(int port) {
        try {
            Connection ret=new Connection(HOST, port);
            ret.initialize();
            return ret;
        } catch (IOException e) {
            Assert.that(false, "Couldn't connect to port "+port);
        }
        return null;  //never reached
    }

    /** Sends an encoding of qrt along c, flushing c.
     *      @modifies c */
    private static void send(Connection c, QueryRouteTable qrt) {
        try {
            for (Iterator iter=qrt.encode(null); iter.hasNext(); ) {
                c.send((RouteTableMessage)iter.next());
            }
            c.flush();
        } catch (IOException e) {
            System.out.println("Couldn't send message.");
        }
    }

    /** Checks that the route table messages sufficient to convey target
     *  are received from c in a timely manner.  Other messages are ignored. 
     *       @modifies c
     */
    private static void ensureReceived(Connection c, QueryRouteTable target) {
        //TODO: we ignore the RESET message
        QueryRouteTable received=new QueryRouteTable(TABLE_TTLS, TABLE_SIZE);
        for (int i=0; i<20; i++) {   //arbitrarily limit number of messages
            try {
                Message m=c.receive(TIMEOUT);
                if ((m instanceof RouteTableMessage))
                    received.update((RouteTableMessage)m);                
            } catch (InterruptedIOException e) {
                //TODO: according to the specification of Message, this may
                //result in a half-completed message, so we should reconnect the
                //connection.
                break;
            }catch (BadPacketException e) {
                Assert.that(false, "Received bad packet from connection.");
            } catch (IOException e) {
                Assert.that(false, "Connection closed.");
            }
        }
        Assert.that(received.equals(target),
                    "Expected to receive "+target+" but got "+received);
    }

    /** Returns a new table with the union of qrt1+1 and qrt2+1, where
     *  "x+1" refers to the table x with all TTLs increased by 1.  If
     *  qrt2 is null, then qrt1+1 is returned.  If both args are null,
     *  just returns an empty table. */
    private static QueryRouteTable propogate(QueryRouteTable qrt1,
                                      QueryRouteTable qrt2) {
        QueryRouteTable ret=new QueryRouteTable(TABLE_TTLS, TABLE_SIZE);
        if (qrt1!=null)
            ret.addAll(qrt1);
        if (qrt2!=null)
            ret.addAll(qrt2);
        return ret;
    }

    private static void delay(int seconds) {
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) { }
    }
}
