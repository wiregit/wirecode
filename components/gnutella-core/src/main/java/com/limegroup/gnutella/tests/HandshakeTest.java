package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.Properties;

/**
 * The driver for the Gnutella 0.6 compliance tests.  Syntax is:
 * <pre>
 *     java com.limegroup.gnutella.tests.HandshakeTest <address> <port>
 *           where <address> and <port> are the address and port
 *           of the servent instance you wish to test
 * </pre>
 */
public class HandshakeTest {
    private static int TIMEOUT=1000;

    private static void syntaxError() {
        System.err.println("Usage:");
        System.err.println("      java com.limegroup.gnutella.tests."
                         +"HandshakeTest <address> <port>");
        System.exit(1);
    }

    public static void main(String args[]) {
        try {
            if (args.length==2)
                testOutgoing(args[0], Integer.parseInt(args[1]));
            else
                syntaxError();
        } catch (NumberFormatException e) {
            syntaxError();
        }
    }
    
    ////////////////////// Actual Tests /////////////////////////////

    private static void testOutgoing(String address, int port) {        
        TestConnection c=null;
        //Test #1
        try {
            System.out.print("1) testing Gnutella 0.4...");
            c=new TestConnection(address, port, false);
            c.sendLine("GNUTELLA CONNECT/0.4");
            c.sendLine("");
            c.expectLine("GNUTELLA OK"); 
            c.expectLine("");            
            c.testBinary();
            c.close();
            System.out.println("passed.");
        } catch (IOException e) {
            System.out.println("FAILED! Stack trace:");
            e.printStackTrace();
        } finally {
            if (c!=null) 
                c.close();
        }
        
        //Test #2
        try {
            System.out.print("2) testing Gnutella 0.6 with no headers...");
            c=new TestConnection(address, port, true);
            c.sendLine("GNUTELLA CONNECT/0.6");
            c.sendLine("");
            c.expectLine("GNUTELLA/0.6 200 OK");
            c.expectHeaders();
            c.sendLine("GNUTELLA/0.6 200 OK");
            c.sendLine("");
            c.testBinary();
            c.close();
            System.out.println("passed.");
        } catch (IOException e) {
            System.out.println("FAILED! Stack trace:");
            e.printStackTrace();
        } finally {
            if (c!=null) 
                c.close();
        }

        //Test #3
        try {
            System.out.print("3) testing Gnutella 0.6 with two headers...");
            c=new TestConnection(address, port, true);
            c.sendLine("GNUTELLA CONNECT/0.6");
            c.sendLine("User-Agent: LimeWire-Test");
            c.sendLine("Pong-Caching:0.1");
            c.sendLine("");
            c.expectLine("GNUTELLA/0.6 200 OK");
            c.expectHeaders();
            c.sendLine("GNUTELLA/0.6 200 OK");
            c.sendLine("LimeWire-Garbage:21342934");
            c.sendLine("LimeWire-Trash: adsfnse");
            c.sendLine("");            
            c.testBinary();
            c.close();
            System.out.println("passed.");
        } catch (IOException e) {
            System.out.println("FAILED! Stack trace:");
            e.printStackTrace();
        } finally {
            if (c!=null) 
                c.close();
        }

        //Test #4
        try {
            System.out.print("4) testing Gnutella 0.7 (with 0.6 response)...");
            c=new TestConnection(address, port, true);
            c.sendLine("GNUTELLA CONNECT/0.7");
            c.sendLine("");
            c.expectLine("GNUTELLA/0.6 200 OK");
            c.expectHeaders();
            c.sendLine("GNUTELLA/0.6 200 OK");
            c.sendLine("");
            c.testBinary();
            System.out.println("passed.");
        } catch (IOException e) {
            System.out.println("FAILED! Stack trace:");
            e.printStackTrace();
        } finally {
            if (c!=null) 
                c.close();
        }

        try {
            System.out.print("5) testing Gnutella 1.0 (with 0.6 response)...");
            c=new TestConnection(address, port, true);
            c.sendLine("GNUTELLA CONNECT/1.0");
            c.sendLine("");
            c.expectLine("GNUTELLA/0.6 200 OK");
            c.expectHeaders();
            c.sendLine("GNUTELLA/0.6 200 OK");
            c.sendLine("");
            c.testBinary();
            System.out.println("passed.");
        } catch (IOException e) {
            System.out.println("FAILED! Stack trace:");
            e.printStackTrace();
        } finally {
            if (c!=null) 
                c.close();
        }

        //Note that we specifically do NOT test the following, since a servent's
        //behavior is undefined: 
        //  -Sending a malformed version number (e.g., "XP", "0.5")
        //  -Sending a version number greater than "0.6" in the SECOND header
        //  -Sending response codes other than "200 OK"
        //  -Sending malformed headers (e.g., missing '\r')
    }
}

