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
	FileManager.getFileManager().addDirectory("c:/rjs/src");
    }

    /**
     * Dump the routing table
     */
    public void dumpRouteTable()
    {
        System.out.println(manager.routeTable.toString());
    }

    /**
     * Dump the puch routing table
     */
    public void dumpPushRouteTable()
    {
        System.out.println(manager.pushRouteTable.toString());
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
	    System.out.println("connectToHost trying...");
	    Connection c=new Connection(manager, host, port);
	    System.out.println("Connection established.");
	    Thread tc=new Thread(c);
	    tc.setDaemon(true);
	    tc.start();
	} catch (IOException e) {
	    System.out.println("Couldn't establish connection.");
	    manager.failedToConnect(host, port);
	    throw e;
	}
    }

    /**
     * Remove a connection based on the host/port
     */
    public void removeConnection( String host, int port )
    {
	Connection conn = findConnection( host, port );
System.out.println("removeConnection("+host+","+port+"): "+conn);
	conn.shutdown();
	if ( conn != null )
	    manager.remove(conn);
    }

    /**
     *  Find a Connection given a host/port
     */
    private Connection findConnection(String host, int port)
    {
	Iterator    iter = manager.connections();
	Connection  conn;

	while ( iter.hasNext() )
	{
	    conn = (Connection) iter.next();
	    if ( conn.getPort() != port )
		continue;
System.out.println("iter :"+conn.getInetAddress().getHostAddress());
	    if ( host.equals( conn.getInetAddress().getHostAddress() ) )
		return( conn );
	}
	return(null);
    }

    /**
     * Tell the system to send connection activity to this callback interface
     */
    public void setActivityCallback( ActivityCallback callback )
    {
        manager.setActivityCallback( callback );
System.out.println("RouterService init");
    }

    public void shutdown()
    {
	manager.shutdown(); //write gnutella.net
    }

    /**
     *  Return the total number of messages sent and received
     */
    public int getTotalMessages() {
	return( manager.getTotalMessages() );
    }

    /**
     *  Return the number of good hosts
     */
    public int getNumHosts() {
	return( manager.catcher.getNumHosts() );
    }

    /**
     *  Return an iterator on the hostcatcher hosts
     */
    public Iterator getHosts() {
	return( manager.catcher.getHosts() );
    }


    /**
     *  Return the number of good hosts
     */
    public int getNumConnections() {
	return( manager.getNumConnections() );
    }

}
