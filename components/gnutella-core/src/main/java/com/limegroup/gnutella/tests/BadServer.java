package com.limegroup.gnutella.tests;

import java.net.*;
import java.io.*;
import com.limegroup.gnutella.*;

/** A bad Gnutella server.  For testing purposes client-side timeouts. */
public class BadServer {
    static String host="127.0.0.1";
    static int port=3333;
    static int timeout=16000;

    public static void main(String[] args) {
        try {
            testGnutellaTimeout();
            testPushTimeout();
        } catch (IOException e) {
            System.out.println("Unexpected error.");
            e.printStackTrace();
        }
    }
	
    public static void testGnutellaTimeout() throws IOException {
        System.out.println("Waiting for incoming connections on port 3333.");
        System.out.println("Please connect your client to me now.");            
        ServerSocket sock=new ServerSocket(port);
        Socket client=sock.accept();
        
        expectString(client, "GNUTELLA CONNECT/0.4\n\n");
        client.getOutputStream().write(("GNUTEL").getBytes());
        
        System.out.println("I have sent a partial reply.");
        System.out.println("Waiting for your connection to timeout...");
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) { }       
        System.out.println("If your connection did not timeout, you failed the test.");
        System.out.println("Closing the connection\n\n");
        sock.close();
    }

    public static void testPushTimeout() throws IOException {
        System.out.println("Now make sure your client is listening on port 6346");
        System.out.println("and serving a file in slot 0. "+
                           "I'm going to send a push request.");
        ServerSocket server=new ServerSocket(port);

        //Get other guy to connect to me.
        Connection c=new Connection("localhost", 6346);
        c.connect();
        byte[] myIP=new byte[] {(byte)127, (byte)0, (byte)0, (byte)1};
        QueryRequest query=new QueryRequest((byte)5, 0, "*.*");
        c.send(query);
        c.flush();
        byte[] clientID=null;
        while (true) {
            try {
                Message m=c.receive();
                if (m!=null && (m instanceof QueryReply)) {
                    clientID=((QueryReply)m).getClientGUID();
                    break;
                }
            } catch (BadPacketException e) { }
        }                   
        PushRequest push=new PushRequest(new byte[16], (byte)5, clientID, 0,
                                       myIP, port);
        c.send(push);

        Socket sock=server.accept();
        expectString(sock, "GIV");  //don't bother reading the rest
        OutputStream out=sock.getOutputStream();
        out.write(("G").getBytes());

        System.out.println("I wrote a partial GET request.  Waiting for timeout...");
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) { }       
        System.out.println("If your upload did not timeout, you failed the test.");
        System.out.println("Closing the connection.");
        sock.close();                
    }

    private static void expectString(Socket sock, String s) throws IOException {       	
        byte[] bytes=s.getBytes();
        InputStream in=sock.getInputStream();	
        for (int i=0; i<bytes.length; i++) {
            int got=in.read();  //Could be optimized, but doesn't matter here.
            if (got==-1)
                throw new IOException();
            if (bytes[i]!=(byte)got)
                throw new IOException();
        }
    }
}
