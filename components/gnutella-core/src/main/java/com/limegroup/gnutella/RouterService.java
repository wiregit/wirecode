package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;

/**
 * The External interface into the router world.
 */
public class RouterService
{	
    private ConnectionManager manager;
    private ResponseVerifier verifier = new ResponseVerifier();
    
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
    private void initManager()
    {
	manager.setKeepAlive(SettingsManager.instance().getKeepAlive());
	Thread t=new Thread(manager);
	t.setDaemon(true);
	t.start();	
	// FileManager.getFileManager().addDirectory("C:/rjs/src");
	//FileManager.getFileManager().addDirectory("E:/My Music");
        //new LimeProperties("Neutella.props",true);

        manager.propertyManager();
	//Now if quick connecting, try hosts.
	if (SettingsManager.instance().getUseQuickConnect()) {
	    Thread t2=new Thread() {
		public void run() {
		    quickConnect();
		}
	    };
	    t2.setDaemon(true);
	    t2.start();
	}	
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
     * Connect to remote host (establish outgoing connection).
     * Blocks until connection established.
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
     * Connects to hosts using the quick connect list. 
     * Blocks until connected.
     */
    public void quickConnect() {
	SettingsManager settings=SettingsManager.instance();
	//Ensure the keep alive is at least 1.
	if (settings.getKeepAlive()<1)
	    settings.setKeepAlive(SettingsInterface.DEFAULT_KEEP_ALIVE);
	adjustKeepAlive(settings.getKeepAlive());
	//Clear host catcher.  Note that if we already have outgoing
	//connections the host catcher will fill up after clearing it.
	//This means we won't really be trying those hosts.
	clearHostCatcher();

	//Try the quick connect hosts one by one.
	String[] hosts=SettingsManager.instance().getQuickConnectHosts();       
	for (int i=0; i<hosts.length; i++) {
	    //Extract hostname+port
	    Endpoint e;
	    try {
		e=new Endpoint(hosts[i]);
	    } catch (IllegalArgumentException exc) {
		continue;
	    }

	    //Connect...or try to.
	    Connection c=new Connection(e.getHostname(), e.getPort());
	    try {
		connectToHost(c);
	    } catch (IOException exc) {
		continue;
	    }
	    
	    //Wait some time.  If we still need more, try others.
	    synchronized(this) {
		try {
		    wait(4000);
		} catch (InterruptedException exc) { }		
	    }
	    if (manager.catcher.getNumHosts()>=settings.getKeepAlive()) {
		break;
	    }
	}
    }

    /** 
     * @modifies this
     * @effects removes all connections.
     */
    public void disconnect() {
	SettingsManager settings=SettingsManager.instance();	
	int oldKeepAlive=settings.getKeepAlive();

	//1. Prevent any new threads from starting.       
	adjustKeepAlive(0);
	//2. Remove all connections.
	for (Iterator iter=manager.connections(); iter.hasNext(); ) {
	    Connection c=(Connection)iter.next();
	    removeConnection(c);
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
     * Clear the hostcatcher if requested
     */
    public void clearHostCatcher()
    {
	manager.catcher.clear();
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
     * Notify the backend that spam filters settings have changed, and that
     * extra work must be done.
     */
    public void adjustSpamFilters() {
	//Just replace all the spam filters.  No need to do anything
	//fancy like incrementally updating them.
	for (Iterator iter=manager.connections(); iter.hasNext(); ) {
	    Connection c=(Connection)iter.next();
	    c.setPersonalFilter(SpamFilter.newPersonalFilter());
	    c.setRouteFilter(SpamFilter.newRouteFilter());
	}
    }

    /**
     * @modifies this
     * @effects sets the port on which to listen for incoming connections. 
     *  If that fails, this is <i>not</i> modified and IOException is thrown.
     *  If port==0, tells this to stop listening to incoming connections.
     */
    public void setListeningPort(int port) throws IOException {
	manager.setListeningPort(port);
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
     * Return the size of all files in my horizon, in kilobytes.
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
	manager.sendToAll(pr);
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
	verifier.record(qr); //record the sent query with verifier to be able to find accuracy of replies.
	manager.sendToAll(qr);
	return qr.getGUID();
    }

    public int score(byte[] Guid, Response resp){
	return verifier.score(Guid,resp);
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

    /**
     * Returns an instance of a SettingsInterface
     */
    public SettingsInterface getSettings() {
	return SettingsManager.instance();
    }

    /**
     * Create a download request
     */
    public void tryDownload(String ip, int port, int index, String fname, 
      byte[] bguid) {
	//String file = "/get/" + String.valueOf(index) + "/" + fname;

        HTTPDownloader down = new
            HTTPDownloader("http", ip, port, index, fname, manager, bguid);

	Thread t = new Thread(down);

	t.setDaemon(true);

	t.start();
	
    }

    /**
     * Try to resume a download request
     */
    public void resumeDownload( HTTPDownloader mgr )
    {
	mgr.resume();

	Thread t = new Thread(mgr);
	t.setDaemon(true);
	t.start();
    }
	
}









