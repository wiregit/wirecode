package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.IOException;

/** A bad client that receives data without sending. */
public class ReceivesOnly {
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
        
        while (true) {
            try {
                Message m=c.receive();
            } catch (BadPacketException e) { 
            } catch (IOException e) {
                break;
            }
        }
    }
}
