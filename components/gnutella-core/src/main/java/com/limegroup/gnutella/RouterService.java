package com.limegroup.gnutella;

import java.io.*;
import java.util.*;

/**
 * The External interface into the router world.
 */
public class RouterService
{	
    private ConnectionManager manager;

    /**
     * Create a connection manager using the default port
     */
    public RouterService( )
    {
	manager = new ConnectionManager();
        initManager();
    }

    /**
     * Create a connection manager using the specified port
     */
    public RouterService( int port )
    {
	manager = new ConnectionManager( port );
        initManager();
    }

    /**
     * Initialize the connection manager 
     */
    public void initManager()
    {

	manager.setKeepAlive(Const.KEEP_ALIVE);
	Thread t=new Thread(manager);
	t.setDaemon(true);
	t.start();	
    }

    /**
     * Dump the routing table
     */
    public void dumpRouteTable()
    {
        System.out.println(manager.routeTable.toString());
    }

    /**
     * Dump the list of connections
     */
    public void dumpConnections()
    {
	Iterator iter=manager.connections();
	while (iter.hasNext())
	    System.out.println(iter.next().toString());
    }

    /**
     * Connect to remote host (establish outgoing connection)
     */
    public void connectToHost( String host, int port ) throws IOException
    {
	try {
	    Connection c=new Connection(manager, host, port);
	    //System.out.println("Connection established.");
	    Thread tc=new Thread(c);
	    tc.setDaemon(true);
	    tc.start();
	} catch (IOException e) {
	    //System.out.println("Couldn't establish connection.");
	    throw e;
	}
    }

    public void shutdown()
    {
	manager.shutdown(); //write gnutella.net
    }

}
