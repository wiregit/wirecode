package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
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
    /** The port to listen for incoming connections. */
    private int port;

    /** Routing information.  ME_CONNECTION is a hack described in Connection. */
    public RouteTable routeTable=new RouteTable(2048); //tweak as needed
    public RouteTable pushRouteTable = new RouteTable(2048);//same as Route Table could be lower
    static final Connection ME_CONNECTION=new Connection("",0);

    /** List of all connections.  This is <i>not</i> synchronized, so you must
     * always hold this' monitor before modifying it. */
    private List /* of Connection */ connections=new ArrayList();
    public  HostCatcher catcher=new HostCatcher(this,Const.HOSTLIST);

    private int keepAlive=0;
    private ActivityCallback callback;
    public String ClientId;
    private int   activeAttempts = 0;
    public boolean stats;

    /** Variables for statistical purposes */
    /*NOTE: THESE VARIABLES ARE NOT SYNCHRONIZED...SO THE STATISTICS MAY NOT BE 100% ACCURATE. */
    public int total; //total number of messages sent and received
    public  int PReqCount; //Ping Request count
    public int PRepCount; //Ping Reply count
    public int QReqCount; //Query Request count
    public int QRepCount; //Query Reply count
    public int pushCount; //Push request count

    /** Creates a manager that listens for incoming connections on the given
     * port.  If this is a bad port, you will get weird messages when you
     * call run. */
    public ConnectionManager(int port) {
	this.port=port;
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
    }  
    
    /** 
     *This method is used to get important properties (ClientId for now) at initialization
     *If the property is not set, a value is given to the property.
     */
    public void propertyManager(){
	ClientId = LimeProperties.getProperties().getProperty("clientID");
	if (ClientId == null){
	    ClientId = new String(Message.makeGuid() );
	    LimeProperties.getProperties().setProperty("ClientID",ClientId);
	}
	String statVal=LimeProperties.getProperties().getProperty("stats");
	if (statVal==null)
	    LimeProperties.getProperties().setProperty("stats","off");
	else {
	    if (statVal.equals("on"))
		stats= true;
	    else 
		stats= false;
	}	  
    }

    /** 
     * Associate a GUID with this host
     * 
     * @modifies route table
     * @effects notifies the route table that m is a request originating from 
     *  this host and I expect replies, i.e.,
     *  <pre>
     *      routeTable.put(m.getGUID(), ME_CONNECTION);
     *  </pre>
     */
    public void fromMe(Message m) {
	routeTable.put(m.getGUID(), ME_CONNECTION);
    }

    /** @modifies network
     *
     *  @effects sends the message m to all connections except c.  This is useful
     *   for forwarding a packet.  (You don't want to forward it to the originator!)
     */
    public synchronized void sendToAllExcept(Message m, Connection c) 
	throws IOException {
	//TODO2: use reader/writer lock to allow parallelism.  Avoid iterator.
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
	//TODO2: use reader/writer lock to allow parallelism.  Avoid iterator.
	Assert.that(m!=null);

	//to forward to, especially on searches.
	Iterator iter=connections.iterator();
	while (iter.hasNext()) {
	    Connection c2=(Connection)iter.next();
	    Assert.that(c2!=null);
	    c2.send(m);
	}
    }
    
    /**
     *  Return the total number of messages sent and received
     */
    public int getTotalMessages() {
	return total;
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
	    error("Couldn't bind server socket to port");
	    return;
	}
	// start the statistics thread
	try{
	    Stat s = new Stat(this);
	    Thread stat = new Thread(s);
	    if (stats==true)
		stat.start();
	}
	catch (Exception e){
	    error("Could not start statistics gatherer.");
	}
	while (true) {
	    try {
		//Accept an incoming connection, make it into a Connection
		//object, handshake, and give it a thread to service it.

		Socket client = null;

		try
		{
			client=sock.accept();
		}
		catch(NullPointerException nfe)
		{
			//cant do anything
			//this is in the ServerSocket class (due to lack of memory maybe)
			//so just continue and wait for the next conection
			continue;
		}
		try {
		    InputStream in=client.getInputStream();
		    String word=readWord(in);

		    if (word.equals("GNUTELLA")) {
			//a) Gnutella connection
			tryingToConnect(
					getHostName(client.getInetAddress()), 
					client.getPort(), 
					true);
			
			Connection c=new Connection(client); 
			c.setManager(this);
			add(c);		 
			Thread t=new Thread(c);
			t.setDaemon(true);
			t.start();
		    } else if (word.equals("PUT")) {
			//b) HTTP with PUT command
			TransferConnection xfer=new TransferConnection(client,false);
			Thread t=new Thread(xfer);
			t.setDaemon(true);
			t.start();
		    } else if (word.equals("GET")) {
			//c) HTTP with GET command
			TransferConnection xfer=new TransferConnection(client,true);
			Thread t=new Thread(xfer);
			t.setDaemon(true);
			t.start();
		    } else {
			throw new IOException();
		    }
		} catch (IOException e) { 
		    //handshake failed: try to close connection.
		    failedToConnect(getHostName(client.getInetAddress()), client.getPort());
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

    /** Returns the first word (i.e., no whitespace) of less than 8 characters
     *  read from sock, or throws IOException if none found. */
    private static String readWord(InputStream sock) throws IOException {
	final int N=9;  //number of characters to look at
	char[] buf=new char[N];
	for (int i=0 ; i<N ; i++) {
	    int got=sock.read();
	    if (got==-1)  //EOF
		throw new IOException();
	    if ((char)got==' ') { //got word.  Exclude space.
		return new String(buf,0,i);
	    }
	    buf[i]=(char)got;
	}
	throw new IOException();       	    
    }

    private void error(String msg) {
	ActivityCallback callback=getCallback();
	if (callback!=null)
	    callback.error(msg);
    }

    /** 
     *  Start passing on connection events
     */
    public void setActivityCallback(ActivityCallback connection) {
        callback = connection;
    }

    /** @requires c not in this
     *  @effects adds c to this
     */
    public synchronized void add(Connection c) {
	Assert.that(!connections.contains(c));
	
	connections.add(c);
	//Don't record incoming connections, since the foreign host's
	//port is ephemeral.
	if (c.isOutgoing())
	    catcher.addGood(c);

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
	// Tell the listener that this connection is connecting.
	if ( callback != null )
	    callback.addConnection(
              host, 
              port, 
	      (incoming ? callback.CONNECTION_INCOMING :
                          callback.CONNECTION_OUTGOING), 
              callback.STATUS_CONNECTING);

	// Maintain a count of attempted outgoing connections
    	if ( ! incoming ) 
	{
	    activeAttempts++;
	}
    }

    /** 
     *  @effects passes failed connect information to ActivityCallback
     */
    public void failedToConnect(String host, int port) {
	// Remove this connection
	if ( callback != null )
	    callback.removeConnection( host, port );

	// Maintain a count of attempted outgoing connections
	activeAttempts--;
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
	    c.shutdown();//ensure that the connection is closed
	    if (keepAlive!=0) {
		//Asynchronously fetch a connection to replace c
		Thread t=new ConnectionFetcher(this,1);
		t.start();
	    }

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

    public int getActiveAttempts()
    {
	return(activeAttempts--);
    }

    private static String getHostName( InetAddress ia )
    {
	String host = ia.getHostAddress();
	
	return(host);
    }

    /**
     *  Returns the number of connections 
     */
    public synchronized int getNumConnections() {
	return connections.size();
    }

    /**Returns true if the given ClientID matches the ClientID of this host
     *else returns false.
     *This method will be called when a push request is being made and a 
     *host which receives the messages is unable to direct the message any further.
     *The host then needs to check if it is the final destination. 
     *If this method returns true, then it is the final destination.
     */
    public boolean isClient(byte[] ClientId){
	//get the client ID from the gnutella.ini file.
	String packetId = new String (ClientId);
	if ( this.ClientId.equals(packetId) )
	    return true;
	return false;
    }
    
    /** Returns an unmodifiable iterator of a clone of this' connections.
     *  The iterator yields items in any order.  It <i>is</i> permissible
     *  to modify this while iterating through the elements of this, but
     *  the modifications will not be visible during the iteration.
     */
    public synchronized Iterator connections() {
	//Iterator cannot be modified after it is returned.
	List clone=new ArrayList();	
	clone.addAll(connections);
	return new UnmodifiableIterator(clone.iterator());
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
     *  Reset how many connections you want and start kicking more off
     *  if required
     */
    public void adjustKeepAlive(int newKeep)
    {
	keepAlive = newKeep;
	if (keepAlive > 0) {
	    int need = keepAlive - getNumConnections() - getActiveAttempts();
	    //Asynchronously fetch connections to maintain keepAlive connections
	    for ( int i=0; i < need; i++ )
	    {
	        Thread t=new ConnectionFetcher(this, 1);
	        t.start();
	    }
	}
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

//      public static void main(String args[]) {
//  	try {
//  	    //Tests ConnectionManager.connections()
//  	    Const.KEEP_ALIVE=0;
//  	    ConnectionManager cm=new ConnectionManager();
//  	    Thread t=new Thread(cm);
//  	    t.setDaemon(true);
//  	    t.start();
//  	    ConnectionManager cm2=new ConnectionManager(6349);
//  	    Thread t2=new Thread(cm);
//  	    t2.setDaemon(true);
//  	    t2.start();

//  	    Connection c1=new Connection(cm2, "localhost", 6346);
//  	    Iterator iter=cm2.connections();	    
//  	    Connection c2=new Connection(cm2, "localhost", 6346);
//  	    Assert.that(iter.next()==c1);
//  	    Assert.that(! iter.hasNext());
//  	} catch (IOException e) {
//  	    e.printStackTrace();
//  	    Assert.that(false);
//  	}
//      }
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
    }

    public void run() {
	while (n>0) {
	    try {
		Connection c=manager.catcher.choose();
		try {
		    //Send initial ping request.  HACK: use routeTable to
		    //designate that replies are for me.  Do this *before*
		    //sending message.
		    PingRequest pr=new PingRequest(Const.TTL);
		    manager.fromMe(pr);
		    c.send(pr); 
		} catch (IOException e) {
		    //Try again!
		    c.shutdown();
		    continue;
		}
		c.setManager(manager);
		manager.add(c);
		Thread t=new Thread(c);
		t.setDaemon(true);
		t.start();
		//Manager.error("Asynchronously established outgoing connection.");
		n--;
	    } catch (NoSuchElementException e) {
		//give up
		//Manager.error("Host catcher is empty");

		return;
	    }
	}
    }		

}
