package com.limegroup.gnutella;

import java.net.*;
import java.io.IOException;
import java.util.*;

/**
 * The list of all connections.  Accepts new connections and creates
 * new threads to handle them.<p>
 *
 * The acceptor thread creates HostFetcher threads to create outgoing
 * connections as needed.  This does NOT guarantee that the acceptor
 * will have the desired number of connections.  That's kind of tricky,
 * as you have to consider three events:
 * <ul>
 * <li>Connections removed
 * <li>Startup
 * <li><b>New hosts available</b> that weren't before
 * </ul>
 *
 * You should call the shutdown() method when you're done to ensure
 * that the gnutella.net file is written to disk.  */
public class ConnectionManager implements Runnable {
    private int port;
    public RouteTable routeTable=new RouteTable(2048); //tweak as needed
    public RouteTable pushRouteTable = new RouteTable(2048);//same as Route Table could be lower
    private List /* of Connection */ connections=Collections.synchronizedList(
						      new ArrayList());
    public  HostCatcher catcher=new HostCatcher(this,Const.HOSTLIST);
    private int keepAlive=0;
    private ActivityCallback callback;
    private LimeProperties lp = new  LimeProperties("Neutella.props",true).getProperties();
    public String ClientId;



    public long totalSize;
    public long totalFiles;


    /** Creates a manager that listens for incoming connections on the given
     * port.  If this is a bad port, you will get weird messages when you
     * call run. */
    public ConnectionManager(int port) {
	this.port=port;
	propertyManager();
    }

	/**
	* Returns the port at which the Connection Manager listens for incoming connections
	* 	@return the listening port
	*/
	public int getListeningPort()
	{
		return port;
	}

    /** Creates a manager that listens on the default port (6346) for
     *	incoming connections. */
    public ConnectionManager() {
	this(6346);
	propertyManager();
    }  
    
    /** 
     *This method is used to get important properties (ClientId for now) at initialization
     *If the property is not set, a value is given to the property.
     */
    private void propertyManager(){
	ClientId = lp.getProperty("clientID");
	if (ClientId == null){
	    ClientId = new String(Message.makeGuid() );
	    lp.setProperty("ClientID",ClientId);
	}
    }

    /** @modifies network
     *
     *  @effects sends the message m to all connections except c.  This is useful
     *   for forwarding a packet.  (You don't want to forward it to the originator!)
     */
    public synchronized void sendToAllExcept(Message m, Connection c) 
	throws IOException {
	Assert.that(m!=null);
	Assert.that(c!=null);

	//Eventually this code will be specialized a choose a "good" subset
	//to forward to, especially on searches.
	Iterator iter=connections.iterator();
	while (iter.hasNext()) {
	    Connection c2=(Connection)iter.next();
	    Assert.that(c2!=null);
	    if (! c2.equals(c)) {
		c2.send(m);
	    }
	}
    }

    /** 
     *  @modifies network
     *  @effects sends the message m to all connections.  This
     *   is useful for intiating a ping or query.
     */
    public synchronized void sendToAll(Message m) 
	throws IOException {
	Assert.that(m!=null);

	//to forward to, especially on searches.
	Iterator iter=connections.iterator();
	while (iter.hasNext()) {
	    Connection c2=(Connection)iter.next();
	    Assert.that(c2!=null);
	    c2.send(m);
	}
    }
    

    /** @modifies this, network
     *  @effects accepts new incoming connections on a designated port
     *   and services incoming requests.
     */
    public void run() {	
	//1. Start background threads to fetch the desired number of
	//   connections.  These run in parallel until each has launched
	//   a connection--or there are no connections left to try.
	for (int i=0; i<keepAlive; i++) {	    
	    Thread fetcher=new ConnectionFetcher(this,1);
	    fetcher.start();
	}
	//2. Create the server socket, bind it to a port, listen for incoming
	//   connections, and accept them.
	ServerSocket sock=null;
	try {
	    sock=new ServerSocket(port);
	} catch (IOException e) {
	    ConnectionManager.error("Couldn't bind server socket to port");
	    return;
	}
	while (true) {
	    try {
		//Accept an incoming connection, make it into a Connection
		//object, handshake, and give it a thread to service it.
		Socket client=sock.accept();
		System.out.println("In the CManager.. soc was accepted");
		try {
		    tryingToConnect(
                      getHostName(client.getInetAddress()), 
                      client.getPort(), 
                      true);
		    Connection c=new Connection(this, client, true);		   
		    Thread t=new Thread(c);
		    t.setDaemon(true);
		    t.start();
		} catch (IOException e) { 
		    //handshake failed: try to close connection.
		    error("Could not establish incoming connection from "+
			  client.getInetAddress().toString()+"; recovering.");
		    try { client.close(); } catch (IOException e2) { }
		    continue;
		}
	    } catch (IOException e) {
		error("Mysterious error while accepting "
		      +"incoming connections; aborting.");
		return;
	    } catch (SecurityException e) {	
		error("Could not listen to socket for incoming connections; aborting");
		return;
	    }
	}
    }

    public static void error(String msg) {
	System.err.println(msg);
    }

    /** 
     *  Start passing on connection events
     */
    public void setActivityCallback(ActivityCallback connection) {
        callback = connection;
	//System.out.println("ConnectionManager init -  cc ="+connection);
    }

    /** @requires c not in this
     *  @effects adds c to this
     */
    public synchronized void add(Connection c) {
	connections.add(c);
	//Don't record incoming connections, since the foreign host's
	//port is ephemeral.
	if (c.isOutgoing())
	    catcher.addGood(c);

	//System.out.println("add - cc="+callback);
	// Tell the listener that this connection is okay.
	if ( callback != null )
	    callback.updateConnection(
              getHostName(c.getInetAddress()), 
              c.getPort(), 
              callback.STATUS_CONNECTED);
    }

    /** 
     *  @effects passes connecting information to ActivityCallback
     */
    public void tryingToConnect(String host, int port, boolean incoming) {
	//System.out.println("trying - cc="+callback);
	// Tell the listener that this connection is connecting.
	if ( callback != null )
	    callback.addConnection(
              host, 
              port, 
	      (incoming ? callback.CONNECTION_INCOMING :
                          callback.CONNECTION_OUTGOING), 
              callback.STATUS_CONNECTING);
    }

    /** 
     *  @effects passes failed connect information to ActivityCallback
     */
    public void failedToConnect(String host, int port) {
	//System.out.println("failed - cc="+callback);
	// Remove this connection
	if ( callback != null )
	    callback.removeConnection( host, port );
    }
	
    /** 
     * @modifies this, route table
     * @effects removes c from this' connection list and
     *  all corresponding errors from the route table.  If
     *  c is not in the connection list, does nothing.  May
     *  try to establish a new outgoing connection to replace
     *  this one.
     */
    public synchronized void remove(Connection c) {
	int i=connections.indexOf(c);
	if (i != -1) {
	    routeTable.remove(c);
	    pushRouteTable.remove(c);
	    connections.remove(i);
	    if (keepAlive!=0) {
		//Asynchronously fetch a connection to replace c
		Thread t=new ConnectionFetcher(this,1);
		t.start();
	    }

	    //System.out.println("remove - cc="+callback);
	    // Tell the listener that this connection is removed.
	    if ( callback != null )
		callback.removeConnection(
		  getHostName(c.getInetAddress()), c.getPort() );
	}	
    }
    
    public ActivityCallback getCallback()
    {
	return( callback );
    }

    private static String getHostName( InetAddress ia )
    {
	//System.out.println("InetAddr = " + ia );
	String host = ia.getHostAddress();
	
	return(host);
    }

    /**
     *  Returns the number of connections 
     */
    public int getNumConnections() {
	return connections.size();
    }

    /**Returns true if the given ClientID matches the ClientID of this host
     *else returns false.
     *This method will be called when a push request is being made and a 
     *host which receives the messages is unable to direct the message any further.
     *The host then needs to check if it is the final destination. 
     *If this method returns true, then it is the final destination.
     */
    public synchronized boolean isClient(byte[] ClientId){
	//get the client ID from the gnutella.ini file.
	String packetId = new String (ClientId);
	if ( this.ClientId.equals(packetId) )
	    return true;
	return false;
    }
    
    /** Returns an unmodifiable iterator of this' connections.
     *  The iterator yields items in any order.
     */
    public Iterator connections() {
	return new UnmodifiableIterator(connections.iterator());
    }

    /**
     * @requires x>=0, run() not called
     * @modifies this
     * @effects tells this to try to keep at least x active connections.
     *  You must call this before starting the run() method.
     */
    public void setKeepAlive(int x) {
	Assert.that(x>=0);
	this.keepAlive=x;
    }

    /**
     * @modifies the file gnutella.net, or its user-defined equivalent
     * @effects writes the gnutella.net file to disk.
     */
    public void shutdown() {
	try {
	    catcher.write(Const.HOSTLIST);
	} catch (IOException e) { }
    }
}

/** Asynchronously fetches new connections from hostcatcher.  */
class ConnectionFetcher extends Thread {
    private ConnectionManager manager;
    private int n;

    /** 
     * Tries to add N connections to the manager.  This
     * is always a Daemon thread.  Often N=1, and multiple
     * threads of this are running; this gives parallelism. 
     */
    public ConnectionFetcher(ConnectionManager manager, int n) {
	this.manager=manager;
	this.n=n;
	setDaemon(true);
	//System.out.println("Fetcher Started");
    }

    public void run() {
	for ( ; n>0 ; n--) {
	    try {
		//Note: this add c to manager if successful */
		Connection c=manager.catcher.choose();
		Thread t=new Thread(c);
		t.setDaemon(true);
		t.start();
		//Manager.error("Asynchronously established outgoing connection.");
		System.out.println("Got a connection");
	    } catch (NoSuchElementException e) {
		//give up
		//Manager.error("Host catcher is empty");
		System.out.println("HOST CATCHER IS EMPTY");
		return;
	    }
	}
    }		
}
