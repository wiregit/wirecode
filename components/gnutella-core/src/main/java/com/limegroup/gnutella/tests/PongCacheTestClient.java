package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.net.*;
import java.io.*;

public class PongCacheTestClient
{
    private ServerSocket acceptSocket;
    private PongCacheMessageRouter router;
    private HostCatcher catcher;

    public PongCacheTestClient(int port)
    {
        byte[] myIP = null;
        try
        {
            acceptSocket = new ServerSocket(port);
            myIP = InetAddress.getByName("127.0.0.1").getAddress();
        }
        catch(IOException ie)
        {
            System.out.println("Unable to start up.  Exiting ......");
            System.exit(1);
        }
        router = new PongCacheMessageRouter(myIP, port);
        catcher = new HostCatcher(null);
        //we're not using an acceptor, connection manager, download manager,
        //or uploadmanager, but still need to initialize to instantiate the
        //pong cache.
        router.initialize(null, null, catcher, null);

        //create the outgoing connections (in seperate threads)
        Thread connect1 = 
            new Thread(new OutgoingConnectionThread("127.0.0.1", 
                (port+1), router));
        connect1.start();
        Thread connect2 = 
            new Thread(new OutgoingConnectionThread("127.0.0.1", 
                (port+2), router));
        connect2.start();
        Thread connect3 =
            new Thread(new OutgoingConnectionThread("127.0.0.1", 
                (port+3), router));
        connect3.start();

        System.out.println("Listening on port: " + port);
    }

    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("Usage: PongCacheTestClient [port] ");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        PongCacheTestClient testClient = new PongCacheTestClient(port);
        testClient.run();
    }

    public void run()
    {
        while (true)
        {
            try
            {
                Socket client = acceptSocket.accept();
                if (!connectFirstWordOkay(client))
                {
                    client.close();
                    continue;
                }
                Thread incoming = 
                    new Thread(new IncomingConnectionThread(client, router));
                incoming.start();
            }
            catch(IOException ioe)
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
     * Thread which creates a managed connection to a host:port and forwards
     * pings (if necessary) on the connection and waits for pongs before
     * forwarding it on.
     */
    private class OutgoingConnectionThread implements Runnable
    {
        private PongCacheTestManagedConnection conn;
        private PongCacheMessageRouter router;

        public OutgoingConnectionThread(String host, int port,
                                        PongCacheMessageRouter router)
        {
            conn = new PongCacheTestManagedConnection(host, port, router);
            this.router = router;
        }

        public void run()
        {
            try
            {
                conn.initialize();
            }
            catch(IOException ie)
            {
                ie.printStackTrace();
                conn.close();
                return;
            }
            router.addOutgoingConnection(conn);
            System.out.println("Created outgoing connection to " + 
                               conn.getOrigHost() + ":" + conn.getOrigPort());
            
            //wait for messages
            while (true)
            {
                try 
                {
                    conn.loopForMessages();
                }
                //we have to catch this, since we are not using a connection 
                //manager and if during the message.receive call the connection
                //is closed remotely, then the connection manager will be null.
                //Hence, we need to catch this exception.
                catch(NullPointerException ne)
                {
                    System.out.println("Closing outgoing connection to " + 
                                       conn.getOrigHost() + ":" + 
                                       conn.getOrigPort());
                    conn.close();
                    return;
                }
                 catch(IOException ioe)
                {
                    System.out.println("Closing outgoing connection to " + 
                                       conn.getOrigHost() + ":" + 
                                       conn.getOrigPort());
                    conn.close();
                    return;
                }
            }
        }
    }

    /**
     * Thread which accepts and initializes an incoming connection and responds
     * to pings with cached pongs.
     *
     * @requires - the string "GNUTELLA" has already been read from the socket.
     */
    private class IncomingConnectionThread implements Runnable
    {
        private PongCacheTestManagedConnection conn;
        private PongCacheMessageRouter router;

        public IncomingConnectionThread(Socket client, 
                                        PongCacheMessageRouter router)
        {
            conn = new PongCacheTestManagedConnection(client, router);
            this.router = router;
        }

        public void run()
        {
            try
            {
                conn.initialize();
            }
            catch(IOException ie)
            {
                ie.printStackTrace();
                conn.close();
                return;
            }
            router.addIncomingConnection(conn);
            System.out.println("Incoming connection established"); 
            
            while (true)
            {
                try
                {
                    conn.loopForMessages();
                }
                //we have to catch this, since we are not using a connection 
                //manager and if during the message.receive call the connection
                //is closed remotely, then the connection manager will be null.
                //Hence, we need to catch this exception.
                catch(NullPointerException ne)
                {
                    System.out.println("Closing incoming connection");
                    conn.close();
                    return;
                }
                catch(IOException ioe)
                {
                    System.out.println("Closing incoming connection");
                    conn.close();
                    return;
                }
            }

        }

    }                                                      
}




