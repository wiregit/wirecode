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

        System.out.println("\nStaring new client test ...... ");
        //run all the tests
        if (!newClientPingTest())
            System.out.println("\nNew Client ping request: failure!");
        else
            System.out.println("\nNew Client ping request: success!");
        
        System.out.println("\nStarting old client test ...... ");
        if (!oldClientPingTest())
            System.out.println("\nOld Client ping request: failure!");
        else
            System.out.println("\nOld Client ping request: success!");
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
     * Tests when a new client sends a ping twice, the first time, the ping
     * is broadcasted to all connections and the second time, the cached 
     * pongs are returned.
     */
    private boolean newClientPingTest()
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
        if (!waitForPingReplies(conn))
        {
            conn.close();
            return false;
        }

        //send out ping request again and wait for pongs 
        if (!waitForPingReplies(conn))
        {
            conn.close();
            return false;
        }
        
        conn.close();
        return true;
    }

    private boolean oldClientPingTest()
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
        if (!waitForPingReplies(conn, true, true))
        {
            conn.close();
            return false;
        }

        //send out ping request again and wait for timeout
        if (!waitForPingReplies(conn, true, false))
        {
            conn.close();
            return false;
        }
        
        conn.close();
        return true;
     }

    /**
     * Overloaded function for new clients to use and not worry about whether
     * first request, subsequent request, old client, new client, etc.
     */
    private boolean waitForPingReplies(Connection c)
    {
        return waitForPingReplies(c, false, false);
    }

    /**
     * Send a ping request and wait for 4 ping replies.  oldClient param is
     * used when changing the GUID to reflect an old client, and setting
     * a timeout, if an old client.  
     *
     * @requires - if oldclient, firstRequest should be use to indicate whether
     *             sending the first PingRequest or the second.
     */
    private boolean waitForPingReplies(Connection c, boolean oldClient, 
                                       boolean firstRequest)
    {
        int count = 0;
        //first, send ping request, and if older client, then we need to 
        //generate a GUID and then change the lasy byte to be an old version
        //number
        PingRequest pr = null;
        if (oldClient)
        {
            byte[] guid = GUID.makeGuid();
            guid[15] = (byte)0x00;
            pr = new PingRequest(guid, (byte)7, (byte)0);
        }
        else
        {
            pr = new PingRequest((byte)7);
        }
        
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
                //if (m.getGUID() != pr.getGUID())
                //    return false;
                count++;
                PingReply reply = (PingReply)m;
                System.out.println("Ping Reply received: " + reply.getIP() + 
                    ":" + reply.getPort());
            }
            catch (InterruptedIOException e)
            {
                //we received a socket timeout
                if (oldClient)
                {
                    //if first request, then exception is bad
                    if (firstRequest)
                    {
                        System.out.println("Ping Request throttled");
                        break;
                    }
                    //if subsequent request, then we were throttled (correctly)
                    else
                    {
                        System.out.println("Ping Request throttled");
                        return true;
                    }
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




