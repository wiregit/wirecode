package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;

/**
 * Class to run simple tests to test the implementation of pong-caching.
 */
public class PongCacheTest
{
    private int port; //port used to connect
    //connect to processes running on the same machine
    private static final String HOST = "127.0.0.1";

    public PongCacheTest(int port)
    {
        this.port = port;

        System.out.println("\nStaring normal test ...... ");
        //run all the tests
        if (!normalPingTest())
            System.out.println("\nNormal test: failure!");
        else
            System.out.println("\nNormal test: success!");
        
        System.out.println("\nStarting throttle test ...... ");
        if (!throttlePingTest())
            System.out.println("\nThrottle test: failure!");
        else
            System.out.println("\nThrottle test: success!");
    }

    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("Usage: PongCacheTest [connectPort]");
            System.exit(1);
        }
        
        System.out.println("\nFor proper testing, make sure the following four " 
                           + "seperate processes are currently running on this " 
                           + "machine:\n");
        System.out.println("One process listening on [connectPort]");
        System.out.println("One process listening on [connectPort+1]");
        System.out.println("One process listening on [connectPort+2]");
        System.out.println("One process listening on [connectPort+3]");
        System.out.println("And process listening on [connectPort] has " + 
                           "outgoing connections to all the other processes\n");
        
        PongCacheTest test = new PongCacheTest(Integer.parseInt(args[0]));
    }

    /**
     * Tests when a client sends a ping twice, the first time, the ping
     * is broadcasted to all connections and the second time, the cached 
     * pongs are returned.
     */
    private boolean normalPingTest()
    {
        //create a connection to local host, port specified.
        Connection conn1 = new Connection(HOST, port);
        try 
        {
            conn1.initialize();
        }
        catch (IOException ioe)
        {
            System.out.println("Couldn't connect to " + HOST + ":" + port);
            System.exit(1);
        }

        //send out ping request and wait for pongs
        if (!waitForPingReplies(conn1, false))
        {
            conn1.close();
            return false;
        }

        conn1.close(); //close the connection 

        //create another connection to the same host and see if we receive the
        //cached pongs.
        Connection conn2 = new Connection(HOST, port);
        try
        {
            conn2.initialize();
        }
        catch(IOException ioe)
        {
            System.out.println("Couldn't connect to " + HOST + ":" + port);
            System.exit(1);
        }
 
        //send out ping request again and wait for pongs 
        if (!waitForPingReplies(conn2, false))
        {
            conn2.close();
            return false;
        }
        
        conn2.close();
        return true;
    }

    /**
     * Tests to see if ping requests are throttled correctly.
     */
    private boolean throttlePingTest()
    {
        //create a connection to local host, port specified.
        Connection conn = new Connection(HOST, port);
        try 
        {
            conn.initialize();
        }
        catch (IOException ioe)
        {
            System.out.println("Couldn't connect to " + HOST + ":" + port);
            System.exit(1);
        }
      
        //send out ping request and wait for pongs
        if (!waitForPingReplies(conn, false))
        {
            conn.close();
            return false;
        }

        //send out ping request again and wait for timeout
        if (!waitForPingReplies(conn, true))
        {
            conn.close();
            return false;
        }
        
        conn.close();
        return true;
     }

    /**
     * Sends out a ping request and waits for four replies.  If first request
     * is true, then waits for a timeout.
     */
    private boolean waitForPingReplies(Connection c, boolean waitForTimeout)
    {
        int count = 0;
        PingRequest pr = new PingRequest((byte)7);

        try
        {
            System.out.println("\nSending ping request");
            c.send(pr);
            c.flush();
        }
        catch(IOException ioe)
        {
            return false;
        }

        while (count < 4)
        {
            try 
            {
                Message m = c.receive(SettingsManager.instance().getTimeout());
                if (!(m instanceof PingReply))
                    return false;
                count++;
                PingReply reply = (PingReply)m;
                System.out.println("Ping Reply received: " + reply.getIP() + 
                    ":" + reply.getPort());
            }
            catch (InterruptedIOException e)
            {
                //we received a socket timeout
                if (waitForTimeout)
                {
                    System.out.println("Ping Request throttled");
                    return true;
                }
                else 
                {
                    break;
                }
            }
            catch(IOException ioe)
            {
                break;
            }
            catch(BadPacketException bpe)
            {
                break;
            }
        }
        if (count < 4)
            return false;
        else
            return true;
    }
}




