package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.net.*;
import java.io.*;

/**
 * Dummy test class that wait's for an incoming gnutella connection and responds
 * to pings with pongs of its own address.
 */
public class PongCacheDummyClient
{
    private ServerSocket acceptSocket;
    private byte[] myIP;
    private int port;
    
    public PongCacheDummyClient(int port)
    {
        try
        {
            acceptSocket = new ServerSocket(port);
            myIP = InetAddress.getByName("127.0.0.1").getAddress();
        }
        catch(IOException ioe)
        {
            System.out.println("Unable to startup.  Exiting ......");
            System.exit(1);
        }
        this.port = port;
        System.out.println("Dummy client listening on port: " + port);
    }

    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("Usage: PongCacheDummyClient [port]");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        PongCacheDummyClient dummyTestClient = new PongCacheDummyClient(port);
        dummyTestClient.run();
    }

    public void run()
    {
        while (true) 
        {
            Connection c = null;
            try
            {
                Socket client = acceptSocket.accept();
                if (!connectFirstWordOkay(client))
                {
                    client.close();
                    continue;
                }
                Thread connectionThread = 
                    new Thread(new ConnectionThread(client));
                connectionThread.start();
            }
            catch(IOException ie)
            {
                //no problem, just wait for another connection
                continue;
            }
        }
    }

    /**
     * Makes sure the first word read is "GNUTELLA"
     */
    private boolean connectFirstWordOkay(Socket sock)
    {
        InputStream in = null;
        try
        {
            in = sock.getInputStream();
        }
        catch(IOException ie)
        {
            return false;
        }

        int length = SettingsManager.instance().getConnectString().length();
        char buf[] = new char[length];
        int i;

        for (i = 0; i < length; i++)
        {
            try
            {
                int read = in.read();
                if ((char)read == ' ')
                    break;
                else
                    buf[i] = (char)read;
            }
            catch(IOException ioe)
            {
                return false;
            }
        }

        String receivedString = new String(buf, 0, i);
        if (receivedString.equals(
            SettingsManager.instance().getConnectStringFirstWord()))
            return true;
        else
            return false;
    }

    /**
     * Thread to handle an incoming connection, wait for pings, and respond
     * with a pong of our own address.
     */
    private class ConnectionThread implements Runnable
    {
        private Connection conn;

        public ConnectionThread(Socket client)
        {
            conn = new Connection(client);
        }

        public void run()
        {
            //first try to initialize connection
            try
            {
                conn.initialize();
            }
            catch (IOException ioe)
            {
                conn.close();
                return;
            }
            
            System.out.println("Incoming connection established");
            //handshake ping
            PingRequest handshake = new PingRequest((byte)1);
            try 
            {
                conn.send(handshake);
                conn.flush();
            }
            catch (IOException ie) 
            {
                System.out.println("IOException sending handshake.  Closing " +
                    "connection");
                conn.close();
                return;
            }

            while (true)
            {
                try 
                {
                    Message m = conn.receive();
                    if (!(m instanceof PingRequest))
                        continue;
                    System.out.println("Received ping");
                    PingReply pr = new PingReply(m.getGUID(), (byte)3, port,
                                                 myIP, 0, 0);
                    System.out.println("Sending pong with own address");
                    conn.send(pr);
                    conn.flush();
                }
                catch(IOException ie)
                {
                    System.out.println("IO Exception: closing connection");
                    conn.close();
                    return;
                }
                catch(BadPacketException ie)
                {
                    System.out.println("BadPacketException: closing connection");
                    conn.close();
                    return;
                }
            }
        }
    }
}




