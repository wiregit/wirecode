package com.limegroup.gnutella;

import java.net.*;
import java.io.IOException;
import java.util.*;

/**
 * A Gnutella router.  This is a server that manages connections.  It
 * is currently implemented with a thread-per-connection model.  It 
 * does not deal with the details of GUID->Connection routing (that's
 * handled by RouteTable) but it may deal with "semantic routing".<p>
 *
 * The router thread creates HostFetcher threads to create outgoing
 * connections as needed.  This does NOT guarantee that the router
 * will have the desired number of connections.  That's kind of tricky,
 * as you have to consider three events:
 * <ul>
 * <li>Connections removed
 * <li>Startup
 * <li><b>New hosts available</b> that weren't before
 * </ul>
 *
 * You should call the shutdown() method when you're done to ensure
 * that the gnutella.net file is written to disk.
 */
public class Router implements Runnable {
    private int port;
    public RouteTable routeTable=new RouteTable(2048); //tweak as needed
    private List /* of Connection */ connections=Collections.synchronizedList(
						      new ArrayList());
    public HostCatcher catcher=new HostCatcher(this,Const.HOSTLIST);
    private int keepAlive=0;

    /** Creates a router that listens for incoming connections on the given
     * port.  If this is a bad port, you will get weird messages when you
     * call run. */
    public Router(int port) {
	this.port=port;
    }

    /** Creates a router that listens on the default port (6346) for
     *	incoming connections. */
    public Router() {
	this(6346);
    }  

    /** @modifies: network
     *
     *  @effects: if this message has been encountered already, does
     *    nothing.  Otherwise sends the message m to some some subset of
     *    all connections except c. 
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

    /** @modifies: this, network
     *  @effects: accepts new incoming connections on a designated port
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
	    Router.error("Couldn't bind server socket to port");
	    return;
	}
	while (true) {
	    try {
		//Accept an incoming connection, make it into a Connection
		//object, and give it a thread to service it.
		Socket client=sock.accept();
		Connection c=new Connection(this, client,true);
		Thread t=new Thread(c);
		t.setDaemon(true);
		t.start();
	    } catch (IOException e) {
		error("IO error; recovering");		
	    } catch (SecurityException e) {
		error("Permission denied");
	    }
	}
    }

    public static void error(String msg) {
	System.err.println(msg);
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
    }
	
    /** 
     * @modifies this
     * @effects removes c from this' connection list.  If
     *  c is not in the connection list, does nothing.  May
     *  try to establish a new outgoing connection to replace
     *  this one.
     */
    public synchronized void remove(Connection c) {
	int i=connections.indexOf(c);
	if (i != -1) {
	    connections.remove(i);
	    if (keepAlive!=0) {
		//Asynchronously fetch a connection to replace c
		Thread t=new ConnectionFetcher(this,1);
		t.start();
	    }
	}	
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
    private Router router;
    private int n;

    /** 
     * Tries to add N connections to the router.  This
     * is always a Daemon thread.  Often N=1, and multiple
     * threads of this are running; this gives parallelism. 
     */
    public ConnectionFetcher(Router router, int n) {
	this.router=router;
	this.n=n;
	setDaemon(true);
    }

    public void run() {
	for ( ; n>0 ; n--) {
	    try {
		//Note: this add c to router if successful */
		Connection c=router.catcher.choose();
		Thread t=new Thread(c);
		t.setDaemon(true);
		t.start();
		//Router.error("Asynchronously established outgoing connection.");
	    } catch (NoSuchElementException e) {
		//give up
		//Router.error("Host catcher is empty");
		return;
	    }
	}
    }		
}
