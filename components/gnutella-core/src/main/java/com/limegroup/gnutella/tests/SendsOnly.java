package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.IOException;

/** A bad client that sends data without receiving. */
public class SendsOnly {
    public static void main(String args[]) {
        String host=null;
        int port=0;
        try {
            host=args[0];
            port=Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.err.println("Syntax: ReceivesOnly <host> <port>");
            System.exit(1);
        }

        Connection c=null;
        try {
            c=new Connection(host, port);
            c.initialize();
        } catch (IOException e) {
            System.err.println("Couldn't connect.");
            System.exit(1);
        }
        
        //Send a ping every second...
        while (true) {
//              try {
//                  Thread.sleep(1000);
//              } catch (InterruptedException e) {}

            try {
                c.send(new PingRequest((byte)3));
                c.flush();
            } catch (IOException e) {
                break;
            }
        }
    }
}
