package com.limegroup.gnutella.tests;

import java.util.*;
import java.net.*;
import java.io.*;

import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.LocateRegistry;

import com.limegroup.gnutella.*;

/**
 * Test client which is part of the test framework.  It talks Gnutella, as well
 * as RMI (so that an external controller can control the Gnutella actions).  It
 * binds itself to an RMI Registry (for external controller access) and via the
 * Remote interface connects and disconnects from other test clients.  
 * 
 * The main purpose of this class is to simulate a "few" nodes that are limewire
 * clients and monitor certain characteristics, pings, pongs, cached hosts, 
 * reserve cache hosts, etc ..
 */
public class TestFrameworkClient extends UnicastRemoteObject 
    implements TestFrameworkInterface, ActivityCallback
{
    private String myIP; //ip address of client.
    private int port; //gnutella port of client
    private int rmiPort; //rmi port of client
    private RouterService rs; 

    public TestFrameworkClient(int port) throws RemoteException
    {
        //initialize rmi.  RMI port is 1000 less than Gnutella port.
        super(port-1000);
        try
        {
            myIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch(java.net.UnknownHostException uhe)
        {
            System.out.println("Can't get local IP Address");
            System.exit(1);
        }
        this.port = port;
        this.rmiPort = port-1000;

        //bind object to rmi registry
        rmiBind();

        StandardMessageRouter messageRouter = new StandardMessageRouter(this);
        rs = new RouterService(this, messageRouter);
        rs.initialize();
        try
        {
            rs.setListeningPort(port);
        }
        catch(IOException ie)
        {
            System.out.println("Cannot create listening port");
            System.exit(1);
        }
    }

    /**
     * Tries to bind the object in the RMI registry.  If it fails, then the
     * program is terminated.
     */
    private void rmiBind()
    {
        try
        {
            //create a rmiregistry
            LocateRegistry.createRegistry(rmiPort);

            //bind client to the rmiregistry
            Naming.rebind("rmi://" + myIP + ":" + rmiPort + "/" + 
                          "TestFrameworkClient", this);
            System.out.println("bound to registry");
        }
        catch(Exception e)
        {
            System.out.println("Could not bind client in registry");
            System.exit(1);
        }
    }


    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("Usage: TestFrameworkClient [port]");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]);
        try 
        {
            TestFrameworkClient client = new TestFrameworkClient(port);
        }
        catch(RemoteException re)
        {
            System.out.println("Cannot create remote object");
        }
    }

    //TestFrameworkInterface methods ------------------
    public void connect(String host, int port) throws RemoteException
    {
        rs.connectToHostAsynchronously(host, port);
    }

    public boolean disconnectFrom(String host, int port) throws RemoteException
    {
        return rs.removeAConnection(host, port);
    }

    public void disconnect() throws RemoteException
    {
        rs.disconnect();
    }
    
    public String[] getCurrentConnections() throws RemoteException
    {
        return rs.getConnectionsInfo();
    }

    public int getNumOfConnections() throws RemoteException
    {
        return rs.getNumConnections();
    }

    public int[] getNetworkStatistics() throws RemoteException
    {
        int[] stats = new int[4];
        stats[0] = rs.getRealNumHosts();
        stats[1] = rs.getNumReserveHosts();
        stats[2] = rs.getTotalProcessedPingRequests();
        stats[3] = rs.getTotalBroadcastPingRequests();

        return stats;
    }

    //No-oped Activity Callback methods --------------
    public void connectionInitializing(Connection c)
    {
    }

    public void connectionInitialized(Connection c)
    {
        System.out.println("Connection to " + c.toString() + " created");
    }

    public void connectionClosed(Connection c)
    {
        System.out.println("Connection to " + c.toString() + " dropped");
    }

    public void knownHost(Endpoint e)
    {
    }

    public void handleQueryReply( QueryReply qr )
    {
    }

    public void handleQueryString( String query )
    {
    }

    public void addDownload(Downloader d)
    {
    }

    public void removeDownload(Downloader d)
    {
    }

    public void addUpload(Uploader u)
    {
    }

    public void removeUpload(Uploader u)
    {
    }

    public int getNumUploads()
    {
        return 0;
    }
    
    public void addSharedDirectory(final File directory, final File parent)
    {
    }

    public void addSharedFile(final File file, final File parent)
    {
    }

	public void clearSharedFiles()
    {
    }          

    public void error(int errorCode)
    {
    }

    public void error(int errorCode, Throwable t)
    {
    }

}
                                                                                                                          
