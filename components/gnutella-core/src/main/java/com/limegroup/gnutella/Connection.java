package com.limegroup.gnutella;

import java.net.*;
import java.io.*;

/**
 * A Gnutella connection.  There are two kinds of connections:
 * incoming and outgoing.  The difference is not really substantial.<p>
 *
 * This class provides the core logic for handling the Gnutella
 * protocol.  This includes sending replies to requests but not
 * details of routing.  The Message class (and subclasses)
 * actually does the tedious reading/writing of bytes from socket.<p>
 * 
 * Typical use:
 * <pre>
 *   Socket s=new Socket(...);
 *   Connection c=new Connection(.., s, false); //does handshake
 *   Thread t=new Thread(c);
 *   t.start(); //services connection 
 * </pre>
 */
public class Connection implements Runnable { 
    private Router router;
    private RouteTable routeTable;
    Socket sock;
    private boolean incoming;

    /** The number of packets I sent and received.  This includes bad packets. */
    private int sent;
    private int received;

    /** A stub for testing only! */
    Connection() { }

    /** @modifies network, router
     *  @effects creates an outgoing socket to the following host and port.
     */
    public Connection(Router router, String host, int port) throws IOException {
	this(router, new Socket(host, port), false);
    }

    /** 
     * @modifies network, modifies
     * @effects Throws IOException if handshake failed.  Otherwise
     *  adds this router
     *
     * @param router the router managing me and my "sibling" connections
     * @param sock the socket used for communication
     * @param incoming true if this is an incoming connection.
     *    False otherwise.
     * @exception IOException if the handshake failed
     */
    public Connection(Router router, Socket sock, boolean incoming) 
	throws IOException {
	this.router=router;
	this.sock=sock;
	this.incoming=incoming;       
	this.routeTable=router.routeTable;

	//Handshake
	final String CONNECT="GNUTELLA CONNECT/0.4\n\n";
	final String OK="GNUTELLA OK\n\n";
	if (incoming) {
	    expectString(CONNECT);
	    sendString(OK);
	} else {
	    sendString(CONNECT);
	    expectString(OK);
	    Message m=new PingRequest(Const.TTL);
	    send(m);
	    //Hack: don't want to (later) forward message starting from me.
	    routeTable.put(m.getGUID(), this);
	}

	router.add(this);
  	System.out.println("Established "+(incoming?"incoming":"outgoing")
  			   +" connection on "+sock.toString());
    }
    
    private synchronized void sendString(String s) throws IOException {
	byte[] bytes=s.getBytes(); //TODO: I don't think this is what we want
	OutputStream out=sock.getOutputStream();
	out.write(bytes);
	out.flush();
    }

    private void expectString(String s) throws IOException {
	//TODO1: shouldn't this timeout?
	byte[] bytes=s.getBytes(); //TODO: I don't think this is what we want
	InputStream in=sock.getInputStream();
	//TODO3: can optimize, but this isn't really important
	for (int i=0; i<bytes.length; i++) {
	    int got=in.read();
	    if (got==-1)
		throw new IOException();
	    if (bytes[i]!=(byte)got)
		throw new IOException();
	}
    }
    
    //////////////////////////////////////////////////////////////////////

    /**
     * @modifies: the network underlying this
     * @effects: send m on the network.  Throws IOException if problems
     *   arise.
     */
    public synchronized void send(Message m) throws IOException {
	//System.out.println("Wrote "+m.toString()+"\n   to "+sock.toString());
	OutputStream out=sock.getOutputStream();
	m.write(out);
	out.flush();
	sent++;
    }

    /** 
     * See specification of Message.read. Note that this is
     *  non-blocking, but there is no hard guarantee on the maximum
     *  block time.  
     */
    public Message receive() throws IOException, BadPacketException {
	Message m=Message.read(sock.getInputStream());
	received++;  //keep statistics.
	//if (m!=null)
	//  System.out.println("Read "+m.toString()+"\n    from "+sock.toString());
	return m;
    }

    /**
     * @modifies: the network underlying this, router
     * @effects: receives request and sends appropriate replies.
     *   Returns if either the connection is closed or an error happens.
     *   If this happens, removes itself from the router's connection list.
     */
    public void run() {
	try {
	    while (true) {
		Message m=null;
		try {
		    m=receive();
		    if (m==null)
			continue;
		} catch (BadPacketException e) {
//  		    System.out.println("Discarding bad packet ("
//  				       +e.getMessage()+")");
		    continue;
		}
		//0. Look up the message in the routing table, 
		//   Pass it to the hostcatcher for inspection.
		byte[] guid=m.getGUID();
		Connection originator=routeTable.get(guid);
		router.catcher.spy(m);

		//1. Reply to request I haven't seen yet.
		//TODO: optimize with SWITCH statement or instance methods.
		if (m instanceof PingRequest && originator==null) {    
		    byte[] ip=sock.getLocalAddress().getAddress(); //little endian
		    send(new PingReply(guid,
				       m.getHops(), //TODO: I think...
				       (short)sock.getLocalPort(),
				       ip,
				       (int)0,
				       (int)0));		
		}
  		else if (m instanceof QueryRequest) {
		    //Don't bother to respond, since I have no data to share.
  		}

		//2. Decrement TTL.  If WAS zero, drop message; else forward.
		if ( m.hop()!=0 ) {
		    //a) Broadcast all requests I haven't seen.
		    //   Silently drop those I have seen.
		    if (m.isRequest()) {
			if (originator==null) {
			    routeTable.put(guid,this);
			    router.sendToAllExcept(m, this);
			}
		    }
		    //b) Replies: route to the original machine based
		    //   on the GUID.
		    else {			
			if (originator==this) //Hack or necessary?? 
			    ; //do nothing
			else if (originator!=null)
			    originator.send(m);
			else // originator==null
			    Router.error("Possible routing error on message "
					 +m.toString()
					 +",\n   or was intended for me.");
		    }
		}
	    }
	} catch (IOException e) {
	    router.remove(this);
	    Router.error("Connection closed: "+sock.toString());
	} catch (Exception e) {
	    Router.error("Unexpected exception.  Terminating.");
	    router.remove(this);
	    e.printStackTrace();	
	}
    } 

    public String toString() {
	return "Connection("+(incoming?"incoming":"outgoing")
	    +", "+sock.toString()+", "+sent+", "+received+")";
    }

    public boolean isOutgoing() {
	return !incoming;
    }
}
