package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.IOException;

/** 
 * Tests that a Gnutella client returns the right address.<br>
 * Syntax: AddressTest <host> <port> <expectedAddr>, where <host>:<port>
 * is the client to connect to, and <expectedAddr> is the address for
 * it to return, in dotted-quad format. 
 */
public class AddressTest {
    private static String host;
    private static int port;
    private static String expectedHost;
    private static int TIMEOUT=4000;

    public static void main(String args[]) {
        try {
            host=args[0];
            port=Integer.parseInt(args[1]);
            expectedHost=args[2];
        } catch (Exception e) {
            System.err.println(
                "Syntax: AddressTest <host> <port> <expectedAddr>");
            System.exit(1);
        }            

        try {
            testPong();
            testQueryReply();
        } catch (IOException e) {
            System.out.println("Couldn't run test.");
            e.printStackTrace();
        }
    }

    private static void testPong() throws IOException {
        Connection c=new Connection(host, port);
        c.initialize();
     
        //Send ping
        PingRequest pr=new PingRequest((byte)3);
        c.send(pr);
        c.flush();
        
        //Wait for ping reply...
        while (true) {
            try {
                Message m=c.receive(TIMEOUT);
                if (m!=null
                       && (m instanceof PingReply)
                       && eq(pr.getGUID(), m.getGUID())) {
                    //...and check the address.
                    String ip=((PingReply)m).getIP();
                    System.out.println("Returned IP: "+ip);
                    Assert.that(expectedHost.equals(ip));
                    break;                    
                }
            } catch (BadPacketException e) {
                throw new IOException();
            }
        }

        c.close();
    }

    private static void testQueryReply() throws IOException {
        Connection c=new Connection(host, port);
        c.initialize();
     
        //Send query
        System.out.println("(Please make sure your client is "
                           +"sharing at least one file.)");
        QueryRequest qr=new QueryRequest((byte)3, 0, "*.*");
        c.send(qr);
        c.flush();
        
        //Wait for query reply...
        while (true) {
            try {
                Message m=c.receive(TIMEOUT);
                if (m!=null
                       && (m instanceof QueryReply)
                       && eq(qr.getGUID(), m.getGUID())) {
                    //...and check the address.
                    String ip=((QueryReply)m).getIP();
                    System.out.println("Returned IP: "+ip);
                    Assert.that(expectedHost.equals(ip));
                    break;                    
                }
            } catch (BadPacketException e) {
                throw new IOException();
            }
        }

        c.close();
    }

    private static boolean eq(byte[] guid1, byte[] guid2) {
        return (new GUID(guid1)).equals(new GUID(guid2));
    }
}
