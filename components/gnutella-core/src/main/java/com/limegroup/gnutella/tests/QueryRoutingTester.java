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
 * written in any language--at least in theory.  In reality, this test is
 * a bit fragile and makles several assumptions about the client being tested:
 *
 * <ul>
 *    <li>Table propogations are triggered by receiving messages, not by timers
 *    <li>Updates are sent for all table TTLs.  (This lets us tell when the
 *        table propogation is done.)
 * </ul>
 */
public class QueryRoutingTester {
    public static final String HOST="localhost";
    //TODO: this is required for now
    public static final int TABLE_SIZE=QueryRouteTable.DEFAULT_TABLE_SIZE;
    public static final int TABLE_TTLS=QueryRouteTable.DEFAULT_TABLE_TTL;
    /** The default time to wait between propogations, in seconds. 
     *  Must be more than the table propogation time. */
    public static final int DEFAULT_DELAY_SECONDS=20;

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


        ////////////////////////// Test Table Propogation //////////////////////

        /*
         *  C1 -\
         *       TEST_CLIENT     
         */
        System.out.println("\nTesting one connection.  Please wait...");
        Connection c1=connect(port);
        QueryRouteTable c1T=new QueryRouteTable(TABLE_TTLS, TABLE_SIZE);
        c1T.add("c1x0", 0);  c1T.add("c1x2", 2);
        System.out.println("    Sending route table of "+c1T);
        send(c1, c1T);
        System.out.println("    Checking that I received an empty table\n");
        ensureReceived(c1, propogate(null, null));


        /*
         *  C1 -\
         *       TEST_CLIENT - C2 
         */
        System.out.println("\nTesting two connections.  Please wait...");
        Connection c2=connect(port);
        QueryRouteTable c2T=new QueryRouteTable(TABLE_TTLS, TABLE_SIZE);
        c2T.add("c2x0", 0);  c2T.add("cNx2", 2);
        System.out.println("    Sending route table of "+c2T);
        send(c2, c2T);
        System.out.println("    Waiting for table propogation to be allowed...");
        delay(delayTime);
        System.out.println("    Ensuring table was sent");
        ensureReceived(c1, propogate(c2T, null)); //fails: gets { }
        ensureReceived(c2, propogate(c1T, null)); //fails: gets { c2x0/1 }


        /*
         *  C1 -\
         *       TEST_CLIENT - C2
         *  C3 -/
         */
        System.out.println("\nTesting three connections.  Please wait...");
        Connection c3=connect(port);
        QueryRouteTable c3T=new QueryRouteTable(TABLE_TTLS, TABLE_SIZE);
        c3T.add("c3x0", 0);  c3T.add("cNx2", 2);
        System.out.println("    Sending route table of "+c3T);
        send(c3, c3T);
        System.out.println("    Waiting for table propogation to be allowed...");
        delay(delayTime);
        System.out.println("    Ensuring table was sent");
        ensureReceived(c1, propogate(c2T, c3T));
        ensureReceived(c2, propogate(c1T, c3T));
        ensureReceived(c3, propogate(c1T, c2T));


        ////////////////////////// Test Query Routing ////////////////////////
        System.out.println("Testing query along one path");
        send(c3, "c1x0", 2);         
        ensureReceived(c1, "c1x0"); 
        //The timeouts below don't work, resulting in BadPacketException.
        //But they're not really needed because of the tests in the following
        //paragraph.
        //  ensureReceived(c2, (String)null);
        //  ensureReceived(c3, (String)null);

        System.out.println("Testing query along two paths");
        send(c1, "cNx2", 5);       
        //ensureReceived(c1, (String)null);
        ensureReceived(c2, "cNx2");           
        ensureReceived(c3, "cNx2");
        
        System.out.println("Testing query TTLs");
        //Again, note how we setup the test to avoid using timeouts.
        send(c2, "c1x2", 3); //fails
        send(c2, "c1x2", 4); //fails???!
        send(c2, "c1x2", 5);
        send(c2, "c1x0", 2);
        ensureReceived(c1, "c1x2");
        ensureReceived(c1, "c1x0");
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

    private static void delay(int seconds) {
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) { }
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
        try {
            c.send(new PingRequest((byte)1));
            c.flush();
        } catch (IOException e) {
            System.out.println("Couldn't send ping");
        }

        QueryRouteTable received=new QueryRouteTable(TABLE_TTLS, TABLE_SIZE);
        while (true) {
            try {
                //TODO: we ignore the RESET message
                Message m=c.receive();
                //System.out.println("        Received "+m);
                if (m instanceof RouteTableMessage) {
                    received.update((RouteTableMessage)m);  
                    //TODO: we're not really guaranteed to get this.
                    if (((RouteTableMessage)m).getTableTTL()==(TABLE_TTLS-1))
                        break;
                }
            } catch (BadPacketException e) {
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



    /** Queries the given connection with the given TTL */
    private static void send(Connection c, String query, int ttl) {
        try {
            c.send(new QueryRequest((byte)ttl, (byte)0, query));
            c.flush();
        } catch (IOException e) {
            Assert.that(false, "Couldn't query connection.");
        }
    }

    /** Ensures that the given query--and only that query--was received along
     *  c.  If query==null, ensures no query was received within timeout. */
    public static void ensureReceived(Connection c, String query) {
        while (true) {
            try {
                //TODO: we ignore the RESET message
                Message m=c.receive(1500);
                //System.out.println("        Received "+m);
                if (m instanceof QueryRequest) {
                    String mQuery=((QueryRequest)m).getQuery();
                    Assert.that(query!=null, "Unexpected query: "+mQuery);
                    Assert.that(mQuery.equals(query),
                       "Wrong query: expected "+query+" but got "+mQuery);
                    return;
                }
            } catch (InterruptedIOException e) {
                Assert.that(query==null, "Missing query: "+query);
            } catch (BadPacketException e) {
                Assert.that(false, "Received bad packet from connection.");
            } catch (IOException e) {
                Assert.that(false, "Connection closed.");
            }
        }
    }

}
