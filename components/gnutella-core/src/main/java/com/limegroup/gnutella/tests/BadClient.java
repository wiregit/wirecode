package com.limegroup.gnutella.tests;

import java.net.*;
import java.io.*;

/** A bad Gnutella client.  Tests how robustly a client
 *  handles timeouts, etc. */
public class BadClient {
	static String host="127.0.0.1";
	static int port=6346;
    static int timeout=10000;
    
    public static void main(String[] args) {
        System.out.println("Starting timeout tests.  These may take a while.");
        System.out.println("This assumes you have a gnutella listening on 6346");
        System.out.println("and sharing a file named simple.txt at index 0.");
        testTimeout("");
        testTimeout("G");
        testTimeout("GNUTELLA");
        testTimeout("GNUTELLA ");
        testTimeout("GNUTELLA C");
        testTimeout("GIV 0:A");
        testTimeout("GET /");
        testTimeout("GET /get/0/simple.txt HTTP/1.0\r\nR"); //partial header
    }

    private static void testTimeout(String connectString) {
        System.out.print("Timeout test on \""+connectString+"\": ");
        try {
            boolean ok=timedOutAfter(connectString);
            System.out.println(ok ? "pass" : "FAIL");
        } catch (IOException e) {
            System.out.println("COULDN'T CONNECT");
        }
    }

    /** 
     * Opens a connection to the remote host and sends connectString.
     * (Throws IOException if the connection could not be established.)
     * Then waits a period of time and returns true iff the connection
     * is no longer alive, i.e., the remote host closed the connection.
     */
    private static boolean timedOutAfter(String connectString) 
    throws IOException {
        Socket s=new Socket(host, port);
        OutputStream out=s.getOutputStream();
        out.write(connectString.getBytes());
        out.flush();

        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) { }

        return !isOpen(s);
    }

    /** 
     * @modifies s
     * @effects returns true if s has not been closed. 
     *  Modifies s in weird s; it is assumed that s
     *  will not be used aftewards.
     */
    public static boolean isOpen(Socket s) {
        //There must be a better way.
        try {
            InputStream in=s.getInputStream();
            s.setSoTimeout(1000);
            int i=in.read();
            if (i==-1)
                return false;
            else
                return true;
        } catch (InterruptedIOException e) {
            return true;
        } catch (IOException e) {
            return false;
        }        
    }
}
