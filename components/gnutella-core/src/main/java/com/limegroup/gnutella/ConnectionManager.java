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
    /** The socket that listens for incoming connections.  Initially
     *  null until setIncomingPort is called.  Can be changed to
     *  listen to new ports.
     *
     * LOCKING: obtain socketLock before modifying either.  Notify socketLock when done.
     * INVARIANT: port==socket.getLocalPort() if socket!=null,  port==0 <==> socket==null 
     */
    private int port=0;
    private volatile ServerSocket socket=null;
    private Object socketLock=new Object();

    /** Routing information.  ME_CONNECTION is a hack described in Connection. */
    public RouteTable routeTable=new RouteTable(2048); //tweak as needed
    public RouteTable pushRouteTable = new RouteTable(2048);//same as Route Table could be lower
    static final Connection ME_CONNECTION=new Connection("",0);

    /* List of all connections.  This is implemented with two data structures: a list
     * for fast iteration, and a set for quickly telling what we're connected to.
     *
     * LOCKING: obtain this' monitor before modifying connections or endpoints.
     * INVARIANT: "connections" contains no duplicates, and "endpoints" contains exactly
     * those endpoints that could be made from the elements of "connections".
     */
    private List /* of Connection */ connections=new ArrayList();
    private Set /* of Endpoint */ endpoints=new HashSet();
    /** List of all connection fetchers.  This is synchronized. */
    List /* of ConnectionFetcher */ fetchers=
	Collections.synchronizedList(new ArrayList());
    public  HostCatcher catcher=new HostCatcher(this,SettingsManager.instance().getHostList());

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
    
    /** Creates a manager that listens on the default port. Equivalent to
     *  ConnectionManager(SettingsManager.instance().getPort()) */
    public ConnectionManager() {
	this(SettingsManager.instance().getPort());
    }  

    /**
     * Returns the port at which the Connection Manager listens for incoming connections
     * 	@return the listening port
     */
    public int getListeningPort() {
	return port;
    }

    /**
     * @modifies this
     * @effects sets the port on which the ConnectionManager is listening. 
     *  If that fails, this is <i>not</i> modified and IOException is thrown.
     *  If port==0, tells this to stop listening to incoming connections.
     *  This is properly synchronized and can be called even while run() is
     *  being called.
     */
    public void setListeningPort(int port) throws IOException {
	synchronized (socketLock) {
	    //Special case if port==0.  This ALWAYS works.
	    if (port==0) {
		//Close old socket (if non-null)
		if (socket!=null) {
		    try {
			socket.close();
		    } catch (IOException e) { }
		}
		socket=null;
		this.port=0;
		socketLock.notifyAll();
		return;
	    } 
	    //Normal case.
	    else {
		//1. Try new port.
		ServerSocket newSocket=null;
		try {
		    newSocket=new ServerSocket(port);
		} catch (IOException e) {
		    throw e;
		} catch (IllegalArgumentException e) {
		    throw new IOException();
		}
		//2. Close old socket (if non-null)
		if (socket!=null) {
		    try {
			socket.close();
		    } catch (IOException e) { }
		}
		//3. Replace with new sock.  Notify the accept thread.
		socket=newSocket;
		this.port=port;
		socketLock.notifyAll();
		return;
	    }
	}
    }
    
    public void propertyManager(){
	ClientId = SettingsManager.instance().getClientID();
	if (ClientId == null){
	    ClientId = new String(Message.makeGuid());
	    SettingsManager.instance().setClientID(ClientId);
	}
	boolean statVal = SettingsManager.instance().getStats();
	if(statVal)
	    stats = true;
	else
	    stats = false;	
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
	    ConnectionFetcher fetcher=new ConnectionFetcher(this,1);
	    fetcher.start();
	    fetchers.add(fetcher);
	}
	//2. Create the server socket, bind it to a port, and listen for
	//   incoming connections.  If there are problems, we can continue
	//   onward.
	try {
	    setListeningPort(this.port);
	} catch (IOException e) {
	    error("Can't listen for incoming connections on port "+port
		  +"; please specify another port.");
	}

	//3. Start the statistics thread
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
	    Connection c = null;
	    try {
		//Accept an incoming connection, make it into a Connection
		//object, handshake, and give it a thread to service it.
		//If not bound to a port, wait until we are.  We have a timeout
		//here so that setListeningPort will not block for more than
		//a second.  We may want to disable setListeningPort in
		//the router version of this class and get rid of the timeout here.
		Socket client=null;
		synchronized (socketLock) {
		    if (socket!=null) {
			socket.setSoTimeout(500); //0.5 second
			try { 
			    client=socket.accept(); 
			} catch (InterruptedIOException e) {
			    continue;
			}
		    } else {
			try { socketLock.wait(); } catch (InterruptedException e) {}
			continue;
		    }
		}
		try {
		    InputStream in=client.getInputStream();
		    String word=readWord(in);

		    c = null;

		    if (word.equals("GNUTELLA")) {
			//a) Gnutella connection

			if(getNumConnections() < SettingsManager.instance().getMaxConn() ){//

			c = new Connection( getHostName(client.getInetAddress() ), 
					    client.getPort(), true);
			tryingToConnect(c, true);
			c.initIncoming(client); 
			c.setManager(this);
			add(c);		 
			Thread t=new Thread(c);
			t.setDaemon(true);
			t.start();
			}
			else{// we have more connections than we can handle
			    RejectConnection rc = new RejectConnection(client);
			    rc.setManager(this);
			    Thread t = new Thread(rc);
			    t.setDaemon(true);
			    t.start();
			}

		    } 
		    else if( word.equals("GET") || word.equals("PUT") ){
			
			System.out.println("handling an http...");

			HTTPManager mgr = new HTTPManager(client, this);
			
		    }
		    else {
			throw new IOException();
		    }
		} catch (IOException e) { 
		    //handshake failed: try to close connection.
		    if ( c != null )
		        failedToConnect(c);
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
	endpoints.add(new Endpoint(c.getInetAddress().getHostAddress(), c.getPort()));

	// Tell the listener that this connection is okay.
	if ( callback != null )
	    callback.updateConnection(
	      c,
              callback.STATUS_CONNECTED);
    }

    /** 
     *  @effects passes connecting information to ActivityCallback
     */
    public void tryingToConnect(Connection c, boolean incoming) {
	// Tell the listener that this connection is connecting.
	if ( callback != null )
	    callback.addConnection(
	      c,
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
    public void failedToConnect(Connection c) {
	// Remove this connection
	if ( callback != null )
	    callback.removeConnection( c );

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
	    endpoints.remove(new Endpoint(c.getInetAddress().getHostAddress(), c.getPort()));
	    c.shutdown();//ensure that the connection is closed
	    int need = keepAlive - getNumConnections() - fetchers.size();
	    if ( need > 0 ) {
		//Asynchronously fetch a connection to replace c
		ConnectionFetcher t=new ConnectionFetcher(this,1);
		t.start();
		fetchers.add(t);
	    }

	    // Tell the listener that this connection is removed.
	    if ( callback != null )
		callback.removeConnection( c );
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

    /**
     * returns true if there is a connection to the given host. 
     */
    public synchronized boolean isConnected(Endpoint host) {
	return endpoints.contains(host);
    }	
    
    /** Returns an iterator of a clone of this' connections.
     *  The iterator yields items in any order.  It <i>is</i> permissible
     *  to modify this while iterating through the elements of this, but
     *  the modifications will not be visible during the iteration.
     */
    public synchronized Iterator connections() {
	List clone=new ArrayList();	
	clone.addAll(connections);
	return clone.iterator();
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
     * Get the number of connections wanted to be maintained
     */
    public int getKeepAlive() {
	return( keepAlive );
    }

    /**
     *  Reset how many connections you want and start kicking more off
     *  if required
     */
    public void adjustKeepAlive(int newKeep)
    {
	keepAlive = newKeep;
	if (keepAlive > 0) {
	    int need = keepAlive - getNumConnections() - fetchers.size();
	    //Asynchronously fetch connections to maintain keepAlive connections
	    for ( int i=0; i < need; i++ )
	    {
	        ConnectionFetcher t=new ConnectionFetcher(this, 1);
	        t.start();
		fetchers.add(t);
	    }
	}
    }

    /**
     *  Tell a ConnectionFetcher/HostCatcher whether I want more threads
     *  in total.
     */
    public boolean doYouWishToContinue()
    {
	return( getNumConnections() < getKeepAlive() );
    }

    /**
     * @modifies the file gnutella.net, or its user-defined equivalent
     * @effects writes the gnutella.net file to disk.
     */
    public void shutdown() {
	try {
	    catcher.write(SettingsManager.instance().getHostList());
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
	while ( n > 0 && 
    		manager.doYouWishToContinue() ) {
	    try {
		Connection c=manager.catcher.choose();
		// If the connection came back null then your not needed.
		if ( c == null ) {
		    manager.fetchers.remove(this);
		    return;
		}
		try {
		    //Send initial ping request.  HACK: use routeTable to
		    //designate that replies are for me.  Do this *before*
		    //sending message.
		    PingRequest pr=new PingRequest(SettingsManager.instance().getTTL());
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
		manager.fetchers.remove(this);

		return;
	    }
	}
	manager.fetchers.remove(this);
    }		

}
