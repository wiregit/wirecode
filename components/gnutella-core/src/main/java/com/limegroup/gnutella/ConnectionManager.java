package com.limegroup.gnutella;

import com.limegroup.gnutella.util.Buffer;
import java.net.*;
import java.io.*;
import java.rmi.RemoteException;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.networkdiscoverer.NDAccess;

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
    private byte[] ip;

    /** Routing information.  ME_CONNECTION is a hack described in Connection. */
    public RouteTable routeTable=new RouteTable(2048); //tweak as needed
    public RouteTable pushRouteTable = new RouteTable(2048);//same as Route Table could be lower
    static final Connection ME_CONNECTION=new Connection("",0);

    /* List of all connections.  This is implemented with two data structures: a list
     * for fast iteration, and a set for quickly telling what we're connected to.
     *
     * LOCKING: connections and endpoints must NOT BE MUTATED.  Instead they should be
     *          replaced as necessary with new copies.  Before replacing the structures,
     *          obtain this' monitor.
     *         
     * INVARIANT: "connections" contains no duplicates, and "endpoints" contains exactly
     * those endpoints that could be made from the elements of "connections".
     */
    private volatile List /* of Connection */ connections=new ArrayList();
    private volatile Set /* of Endpoint */ endpoints=new HashSet();
    /** List of all connection fetchers.  This is synchronized. */
    List /* of ConnectionFetcher */ fetchers=
	Collections.synchronizedList(new ArrayList());
    public HostCatcher catcher;
    
    /** Queued up entries to send to each */
    static class MessagePair {
	Message m;
	Connection except;
	MessagePair (Message m, Connection except) { this.m=m; this.except=except; }
    }
    private static final int MESSAGE_QUEUE_SIZE=500;
    private Buffer /* of MessagePair */ messageQueue=new Buffer(MESSAGE_QUEUE_SIZE);

    private int keepAlive=0;
    private ActivityCallback callback;
    public GUID ClientId;
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
    public int totDropped; //Total dropped messages
    public int totRouteError; //Total misrouted messages
    
    /**
    * An instance of NDAccess for remote access to the network discovery engine 
    * to retrieve good hosts
    */
    private NDAccess ndAccess = null;
    
    private Vector badHosts = new Vector();

    /** Creates a manager that tries to listen to incoming connections
     * on the given port.  If this is a bad port, the port will be
     * changed when run is called and SettingsManager will be updated.
     * If that fails, ActivityCallback.error will be called.  A port
     * of 0 means do not accept incoming connections. 
     */
    public ConnectionManager(int port) {
        
	this.port=port;
	try {
	    ip=InetAddress.getLocalHost().getAddress();
	} catch (Exception e) {
	    //In case of UnknownHostException or SecurityException, we have
	    //no choice but to use a fake address: all zeroes.
	    ip=new byte[4];
	}

        
       
        
	//If we're using quick-connect by default, don't load gnutella.net file.
	//(In this case, RouterService will call quick connect.)
	boolean quickConnect=SettingsManager.instance().getUseQuickConnect();
	if (quickConnect)
	    catcher=new HostCatcher(this);
	else
	    catcher=new HostCatcher(this,SettingsManager.instance().getHostList());	    

        /** //anu //we dont need message broadcaster
	Thread t=new Thread(new MessageBroadcaster());
	t.setDaemon(true);
	t.start();
        */
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

    /** Returns this' address to use for ping replies, query replies, and pushes. */
    public byte[] getAddress() {
	//TODO3: if FORCE_LOCAL_IP is true, then use that value instead.
	//       (Alternative implementation: just set this.ip accordingly during
	//        initialization.)
	return ip;
    }

    /**
    * Accessor method to get an instance of NDAccess, which is used for 
    * remote access to the network discovery engine 
    * to retrieve good hosts
    * @return An instance of NDAccess, or null if not able to initialize
    * @see com.limegroup.gnutella.networkdiscoverer.NDAccess
    */
    public NDAccess getNDAccess()
    {
        
        if(ndAccess == null)
        {
             //initialize NDAccess for remote access to Network Discovery Engine
            try
            {
                ndAccess = new NDAccess();
                return ndAccess;
            }
            catch(RemoteException re)
            {
                //e.printStackTrace();
                return null;
            }
        }
        else
        {
            return ndAccess;
        }
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
	    //Special case: if unchanged, do nothing.
	    if (socket!=null && this.port==port)
		return;
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

    /**
     * @modifies this
     * @effects if tryOthers==false, equivalent to "setListeningPort(suggestPort);
     *  return suggestedPort".
     *     Otherwise, tries to set the port to suggestPort.  If that fails, tries
     *  random values.  If this works, the new port value is returned; otherwise,
     *  throws IOException, and this is not modified.<p>
     *
     *  If port==0, tells this to stop listening to incoming connections.
     *  This is properly synchronized and can be called even while run() is
     *  being called.
     */
    public int setListeningPort(int suggestedPort, boolean tryOthers)
	throws IOException {
	//Special case.
	if (!tryOthers) {
	    setListeningPort(suggestedPort);
	    return suggestedPort;
	}
	
	//1. Try suggested port.
	try {
	    setListeningPort(suggestedPort);
	    return suggestedPort;
	} catch (IOException e) { /* continue on */ }

	//2. Try 10 different ports
	for (int i=0; i<10; i++) {
	    int port=i+6346;
	    try {
		setListeningPort(port);
		return port;
	    } catch (IOException e) { }
	}

	//3. Everything failed; give up!
	throw new IOException();
    }
    
    public void propertyManager(){
	boolean ok=true;
	try {
	    ClientId = new GUID(GUID.fromHexString(SettingsManager.instance().getClientID()));
	    if (ClientId==null)
		ok=false;
	} catch (IllegalArgumentException e) {
	    ok=false;
	}
	if (!ok) {
	    //This should never happen! But if it does, we can recover.
	    ClientId=new GUID(Message.makeGuid());
	}

	boolean statVal = SettingsManager.instance().getStats();
	if(statVal)
	    stats = true;
	else
	    stats = false;
	String[] allHosts = SettingsManager.instance().getBannedIps();
	for (int i=0; i<allHosts.length; i++)
	    badHosts.add(allHosts[i]);
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

    /**
     *  @modifies network
     *  @effects sends the message m to all connections except c.  This is useful
     *   for forwarding a packet.  (You don't want to forward it to the originator!)
     *   Underlying IO errors (e.g., because a connection has closed) are caught
     *   and silently ignored.
     */
    public void sendToAllExcept(Message m, Connection c) {
	Assert.that(m!=null);
	Object dropped;
	//Queue the message.  MessageBroadcaster will dequeue and send.
	synchronized (messageQueue) {
	    dropped=messageQueue.add(new MessagePair(m,c));
	    messageQueue.notify();
	}
	if (dropped!=null) {
	    //TODO: increment dropped message count if returned value
	    //of add(..) is not null, i.e., if buffer capacity is reached.	    
	}
    }

    /** 
     *  @modifies network
     *  @effects sends the message m to all connections.  This
     *   is useful for intiating a ping or query. 
     */
    public void sendToAll(Message m) {
	sendToAllExcept(m, null);
    }

    /** Broadcasts queued messages to connections. */
    class MessageBroadcaster implements Runnable {  	    
	public void run() {
	    while (true) {
		//Get a MessagePair from the messageQueue. (Wait if empty.)
		MessagePair pair=null;
		synchronized(messageQueue) {
		    while (messageQueue.isEmpty())
			try {
			    messageQueue.wait();
			} catch (InterruptedException e) { /* do nothing */ }
		    pair=(MessagePair)messageQueue.removeLast();
		}
		Message m=pair.m;
		Connection except=pair.except;
		
		List connectionsSnapshot=connections;
		int n=connectionsSnapshot.size();
		for (int i=0; i<n; i++) {
		    Connection c2=(Connection)connectionsSnapshot.get(i);
		    Assert.that(c2!=null);
		    if (c2!=except) {
			try {
			    c2.send(m);
			} catch (IOException e) { /* ignore */ }
		    }
		}
	    }
	}
    }    
    
    /**
     *  Return the total number of messages sent and received
     */
    public int getTotalMessages() {
	return total;
    }

    /** @modifies this, network, SettingsManager
     *  @effects accepts new incoming connections on a designated port
     *   and services incoming requests.  If the port was changed
     *   in order to accept incoming connections, SettingsManager is
     *   changed accordingly.
     */
    public void run() 
    {	
        //1. Start background threads to fetch the desired number of
        //   connections.  These run in parallel until each has launched
        //   a connection--or there are no connections left to try.

            /** //anu //we dont really need to initiate the connections. Its a router 
            //and is just gonna listen for connections
        for (int i=0; i<keepAlive; i++) {	    
            ConnectionFetcher fetcher=new ConnectionFetcher(this,1);
            fetcher.start();
            fetchers.add(fetcher);
        }
            */
        //2. Create the server socket, bind it to a port, and listen for
        //   incoming connections.  If there are problems, we can continue
        //   onward.
        try {
            int oldPort=port;
            setListeningPort(port,true);
            if (port!=oldPort) {
            SettingsManager.instance().setPort(port);
            if (callback!=null)
                callback.setPort(port);
            }
        } catch (IOException e) {
            error(ActivityCallback.ERROR_0);
        }

        //3. Start the statistics thread
        try{
            Stat s = new Stat(this);
            Thread stat = new Thread(s);
            if (stats==true)
            stat.start();
        }
        catch (Exception e){
            error(ActivityCallback.ERROR_1);
        }


        while (true) {
            try 
            {
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

            //anu
            //now we have got a connection from the Client
            //do everything now in a separate thread
            Thread rejectConnectionHandler = new Thread(
                                        new RejectConnectionHandler(client, this));
            //start the new Thread    
            rejectConnectionHandler.setDaemon(true);
            rejectConnectionHandler.start();



            }//end of try
            catch (IOException e)
            {
                error(ActivityCallback.ERROR_2);
                return;
            } 
            catch (SecurityException e)
            {
                error(ActivityCallback.ERROR_3);
                return;
            } 
            catch (Exception e)
            {
                //Internal error!
                error(ActivityCallback.ERROR_20, e);
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

    private void error(int msg) {
	ActivityCallback callback=getCallback();
	if (callback!=null)
	    callback.error(msg);
    }

    private void error(int msg, Throwable t) {
	ActivityCallback callback=getCallback();
	if (callback!=null)
	    callback.error(msg, t);
    }

    /** 
     *  Start passing on connection events
     */
    public void setActivityCallback(ActivityCallback connection) {
        callback = connection;
    }

    /** 
     *  Adds a connection.
     * 
     *  @requires c in the connected state
     *  @modifies this
     *  @effects if c already in this, does nothing; otherwise, adds c to this.
     */
    public synchronized void add(Connection c) {
	if (connections.contains(c))
	    return;

	//REPLACE connections with the list connections+[c]
	List newConnections=new ArrayList();
	newConnections.addAll(connections);
	newConnections.add(c);
	connections=newConnections;

	//REPLACE endpoints with the set endpoints+{c}
	Set newEndpoints=new HashSet();
	newEndpoints.addAll(endpoints);
	newEndpoints.add(new Endpoint(c.getInetAddress().getHostAddress(), c.getPort()));
	endpoints=newEndpoints;

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
	    //REPLACE connections with the list connections-[c]
	    List newConnections=new ArrayList();
	    newConnections.addAll(connections);
	    newConnections.remove(c);
	    connections=newConnections;
	    
	    //REPLACE endpoints with the set endpoints+{c}
	    Set newEndpoints=new HashSet();
	    newEndpoints.addAll(endpoints);
	    newEndpoints.remove(new Endpoint(c.getInetAddress().getHostAddress(), c.getPort()));
	    endpoints=newEndpoints;

	    //Clean up route tables.
	    routeTable.remove(c);
	    pushRouteTable.remove(c);
	    c.shutdown();//ensure that the connection is closed
	    int need = keepAlive - getNumConnections() - fetchers.size();
	    for (int j=0; j<need; j++) {
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
    public int getNumConnections() {
	List connectionsSnapshot=connections;
	return connectionsSnapshot.size();
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
	return this.ClientId.equals(new GUID(ClientId));
    }

    /**
     * returns true if there is a connection to the given host. 
     */
    public boolean isConnected(Endpoint host) {
	Set endpointsSnapshot=endpoints;
	return endpointsSnapshot.contains(host);
    }	
    
    /** Returns an iterator of a clone of this' connections.
     *  The iterator yields items in any order.  It <i>is</i> permissible
     *  to modify this while iterating through the elements of this, but
     *  the modifications will not be visible during the iteration.
     */
    public Iterator connections() {
	List connectionsSnapshot=connections;
	List clone=new ArrayList();	
	clone.addAll(connectionsSnapshot);
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
     *  if required.  This IS synchronized because we don't want threads
     *  adding or removing connections while this is deciding whether
     *  to add more threads.
     */
    public synchronized void adjustKeepAlive(int newKeep)
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
     *  Returns true if this needs more connections.  This is NOT synchronized;
     *  grab this' monitor before calling if you need atomicity.
     */
    boolean needsMoreConnections()
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
//  	    ConnectionManager cm=new ConnectionManager();
//  	    Connection c1=new Connection("localhost", 3333);
//  	    c1.connect();
//  	    Connection c2=new Connection("localhost", 3333);
//  	    c2.connect();

//  	    //Note that modifications are not reflected in the iterator.
//  	    cm.add(c1);
//  	    Iterator iter=cm.connections();	    
//  	    cm.remove(c1);
//  	    cm.add(c2);
//  	    Assert.that(iter.next()==c1);
//  	    Assert.that(! iter.hasNext());

//  	    //You should be able to remove elements not in there, and add
//  	    //elements already in there (without duplicates)
//  	    cm.remove(c1);
//  	    cm.add(c2);
//  	    iter=cm.connections();
//  	    Assert.that(iter.next()==c2);
//  	    Assert.that(! iter.hasNext());
//  	} catch (IOException e) {
//  	    System.out.println("Couldn't connect to host.  Try again.");
//  	}
//      }
    
    
    //INNER CLASS
    private class RejectConnectionHandler implements Runnable
    {

        Socket client = null;
        ConnectionManager connectionManager = null;

        public RejectConnectionHandler(Socket client, 
                                    ConnectionManager connectionManager)
        {
            this.client = client;
            this.connectionManager = connectionManager;
        }

        public void run()
        {
            //set Time out for the client Socket
            try
            {
                client.setSoTimeout(8000); //8 seconds
            }
            catch(SocketException se)
            {
                //return if we socket is not valid
                return;
            }
            //Check if IP address of the incoming socket is in badHosts 
            //(initialized in propertyManager()
            if (badHosts.contains(client.getInetAddress().getHostAddress() ) )
            {
                try
                {
                    client.close();
                }
                catch(IOException ioe)
                {
                    
                }
                return;
            }

            RejectConnection rc = null;
            //read the first word, and if it is the expected one, proceed
            try
            {
                InputStream in=client.getInputStream();
                String word=readWord(in);

                if (word.equals(SettingsManager.instance().getConnectStringFirstWord()))
                {
                    //a) Gnutella connection

                    /** //anu //we just wanna return good hostnames and dont really
                    //want to maintain connections.
                    //So just open a Reject Connection instead
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
                     */
                    //anu //else{// we have more connections than we can handle
                    
                    //send the pongs (RejectConnection handles this)
                    rc = new RejectConnection(client);
                    rc.setManager(connectionManager);
                    
                    //no need to start a new thread, we r already in a thread
                    //just call the run method without starting a new thread
                    rc.run();
                    
                    //Thread t = new Thread(rc);
                    //t.setDaemon(true);
                    //t.start();
                    //}

                }
                /* //anu //we dont wanna handle any other requests
                //Incoming file transfer connection: normal HTTP and push.
                //we
                else if (word.equals("GET")) {
                HTTPManager mgr = new HTTPManager(client, this, false);
                }
                else if (word.equals("GIV")) {
                HTTPManager mgr = new HTTPManager(client, this, true);
                }
                 */

                //else just close the connection //anu
                else
                {
                    throw new IOException();
                }
            } 
            catch (IOException e)
            {
                //handshake failed: try to close connection.
                if ( rc != null )
                failedToConnect(rc);
                try
                { 
                    client.close(); 
                } 
                catch (IOException e2)
                { 
                }
                return;
            }

        }//end of run
    }//end of class RejectConnectionHandler
    
    
    
    
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
    		manager.needsMoreConnections() ) {

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
		//It's entirely possible that this connection is no longer needed because
		//KEEP_ALIVE was changed above.  Therefore I must check again whether
		//the connection is really needed.  Grab the manager's mutex before doing so.
		synchronized(manager) {
		    if (manager.needsMoreConnections())
			manager.add(c);
		    else {
			//My result is no longer needed.
			c.shutdown();
			manager.failedToConnect(c);
			manager.fetchers.remove(this);
			return;
		    }
		}
		Thread t=new Thread(c);
		t.setDaemon(true);
		t.start();
		//Manager.error("Asynchronously established outgoing connection.");
		n--;

	} //end while
	manager.fetchers.remove(this);
    }		

}





