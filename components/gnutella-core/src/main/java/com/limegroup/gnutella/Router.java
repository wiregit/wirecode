package com.limegroup.gnutella;

import java.net.*;
import java.io.IOException;
import java.util.*;

/**
 * A Gnutella router.  This is a server that manages connections.  It
 * is currently implemented with a thread-per-connection model.  It 
 * does not deal with the details of GUID->Connection routing (that's
 * handled by RouteTable) but it may deal with "semantic routing".
 */
public class Router implements Runnable {
    private int port;
    public RouteTable routeTable=new RouteTable(2048); //tweak as needed
    private List /* of Connection */ connections=Collections.synchronizedList(
						      new ArrayList());

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
    }
	
    /** 
     * @modifies this
     * @effects removes c from this' connection list.  If
     *  c is not in the connection list, does nothing. 
     */
    public synchronized void remove(Connection c) {
	int i=connections.indexOf(c);
	if (i != -1)
	    connections.remove(i);
    }

    /** Returns an unmodifiable iterator of this' connections.
     *  The iterator yields items in any order.
     */
    public Iterator connections() {
	return new UnmodifiableIterator(connections.iterator());
    }
}

