package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.IOException;
import java.util.*;

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
        
        Random r=new Random();
        //Send a ping every second...
        while (true) {
//              try {
//                  Thread.sleep(1000);
//              } catch (InterruptedException e) {}

            try {
                char[] buf=new char[20];
                for (int i=0; i<buf.length; i++) {
                    buf[i]=(char)('A'+r.nextInt(52));
                }
                String query=new String(buf);
                c.send(new QueryRequest((byte)3, 0, query));
                c.flush();
            } catch (IOException e) {
                break;
            }
        }
    }
}
