package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Test client the makes a bunch of connections to a server running on port
 * 7777.  The number of connections to make is specified as a command line 
 * parameter.  This is useful for testing blocking vs. non-blocking server
 * performance with different numbers of connections - particularly for 
 * testing the scalability of the two implementations.
 */
public class NIOBIOTestClientManager {

    public static void main(String[] args) {
        if(args.length < 3) {
            System.out.println("Incorrect syntax.  Please specify the number " +                "of connections to maintain in the first argument and the " +                "delay between sends to the server as the second argument " +                "(in milliseconds).");
            System.exit(0);
        }
        
        int num = -1;
        int delay = -1;
        
        try {
            num = Integer.parseInt(args[0]);
            delay = Integer.parseInt(args[1]);
        } catch(NumberFormatException e) {
            System.out.println("Please specify the number of connections " +
                "to maintain in the first argument -- "+args[0]+" is not a " +
                "valid setting for the number of connections to maintain");
            System.exit(0);
        }

        InetAddress tempAddress = null;
        try {
            tempAddress = InetAddress.getByName(args[2]);
        } catch (UnknownHostException e1) {
            System.out.println("Please specify the host to connect to " +
                "as the third argument -- "+args[2]+" is not a " +
                "valid host to connect to.");
            System.exit(0);
        }
        
        SERVER_ADDRESS = tempAddress;
        
        new NIOBIOTestClientManager(num, delay);                    
    }
    
    private static InetAddress SERVER_ADDRESS;
    
    /**
     * Runs the desired number of clients.
     */
    private NIOBIOTestClientManager(int num, int delay) {
        System.out.println("Making "+num+" connections to "+SERVER_ADDRESS+
            " with each client sending a message every "+delay+" milliseconds.");
        
        for(int i=0; i<num; i++) {
            new TestClient(delay);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            if(i % 10 == 0) {
                System.out.println(i+" connected");
            }
        }
        System.out.println("ALL CLIENTS CONNECTED");
    }


    /**
     * Test client that continually sends data to the server.
     */
    private static final class TestClient implements Runnable {
        
        final byte[] READ_BYTES = new byte[12];
        
        final int DELAY;
         
        TestClient(int delay) {
            DELAY = delay;
            Thread clientThread = new Thread(this, "client thread");
            clientThread.start();
        }
        
        public void run() {
            try {
                Socket sock = new Socket(SERVER_ADDRESS, 7777);
                OutputStream os = sock.getOutputStream();
                InputStream is = sock.getInputStream();
                while(true) {
                    os.write("hello server".getBytes());
                    
                    try {
                        Thread.sleep(DELAY);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    is.read(READ_BYTES);
                    if(!Arrays.equals(READ_BYTES, "hello client".getBytes())) {
                        System.out.println("UNEXPECTED DATA READ FROM SERVER: " +                            new String(READ_BYTES));
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
