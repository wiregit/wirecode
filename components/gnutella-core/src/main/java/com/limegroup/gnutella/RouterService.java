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
	manager.setKeepAlive(SettingsManager.instance().getKeepAlive());
	Thread t=new Thread(manager);
	t.setDaemon(true);
	t.start();	
	//FileManager.getFileManager().addDirectory("C:/rjs/src");
	//FileManager.getFileManager().addDirectory("E:/My Music");
        //new LimeProperties("Neutella.props",true);
        manager.propertyManager();
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
    public void connectToHost( Connection c ) throws IOException
    {
	try {
	    manager.tryingToConnect(c,false);
	    c.connect();
	    c.setManager(manager);
	    manager.add(c); //calls ActivityCallback.updateConnection
	    PingRequest pr=new PingRequest(SettingsManager.instance().getTTL());
	    manager.fromMe(pr);
	    c.send(pr);
	    Thread tc=new Thread(c);
	    tc.setDaemon(true);
	    tc.start();
	} catch (IOException e) {
	    manager.failedToConnect(c);
	    throw e;
	}
    }

    /**
     * Remove a connection based on the host/port
     */
    public void removeConnection( Connection c )
    {
	c.shutdown();
	manager.remove(c);
    }


    /**
     * Tell the system to send connection activity to this callback interface
     */
    public void setActivityCallback( ActivityCallback callback )
    {
        manager.setActivityCallback( callback );
    }

    /**
     * Shut stuff down and write the gnutella.net file
     */
    public void shutdown()
    {
	manager.shutdown(); //write gnutella.net
    }

    /**
     *  Reset how many connections you want and start kicking more off
     *  if required
     */
    public void adjustKeepAlive(int newKeep)
    {
        manager.adjustKeepAlive(newKeep);
    }

    /**
     *  Return the total number of messages sent and received
     */
    public int getTotalMessages() {
	return( manager.getTotalMessages() );
    }

    /**
     *  Return the number of good hosts in my horizon.
     */
    public long getNumHosts() {
	long ret=0;
	for (Iterator iter=manager.connections(); iter.hasNext() ; ) 
	    ret+=((Connection)iter.next()).getNumHosts();
	return ret;
    }

    /**
     * Return the number of files in my horizon. 
     */
    public long getNumFiles() {
	long ret=0;
	for (Iterator iter=manager.connections(); iter.hasNext() ; ) 
	    ret+=((Connection)iter.next()).getNumFiles();
	return ret;
    }

    /**
     * Return the size of all files in my horizon. 
     */
    public long getTotalFileSize() {
	long ret=0;
	for (Iterator iter=manager.connections(); iter.hasNext() ; ) 
	    ret+=((Connection)iter.next()).getTotalFileSize();
	return ret;
    }

    /**
     * Updates the horizon statistics.
     *
     * @modifies manager, network
     * @effects resets manager's horizon statistics and sends
     *  out a ping request.  Ping replies come back asynchronously
     *  and modify the horizon statistics.  Poll for them with
     *  getNumHosts, getNumFiles, and getTotalFileSize.
     */
    public void updateHorizon() {
	//Reset statistics first
	for (Iterator iter=manager.connections(); iter.hasNext() ; ) 
	    ((Connection)iter.next()).clearHorizonStats();
    
	//Send ping to everyone.  Call to fromMe() notes that replies
	//are to me.
	PingRequest pr=new PingRequest(SettingsManager.instance().getTTL());
	manager.fromMe(pr);
	try {
	    manager.sendToAll(pr);
	} catch (IOException e) { }
    }

    /**
     * Searches Gnutellanet with the given query string and minimum
     * speed.  Returns the GUID of the query request sent as a 16 byte
     * array, or null if there was a network error.  ActivityCallback
     * is notified asynchronously of responses.  These responses can
     * be matched with requests by looking at their GUIDs.  (You may
     * want to wrap the bytes with a GUID object for simplicity.)
     */
    public byte[] query(String query, int minSpeed) {
	QueryRequest qr=new QueryRequest(SettingsManager.instance().getTTL(), minSpeed, query);
	manager.fromMe(qr);
	try {
	    manager.sendToAll(qr);
	    return qr.getGUID();
	} catch (IOException e) { 
	    return null;
	}
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

    /**
     *  Return the number searches made locally ( QReq )
     */
    public int getNumLocalSearches() {
	return( manager.QReqCount );
    }

    /**
     *  Remove unwanted or used entries from host catcher 
     */
    public void removeHost(String host, int port) {
	manager.catcher.removeHost(host, port);
    }
}

