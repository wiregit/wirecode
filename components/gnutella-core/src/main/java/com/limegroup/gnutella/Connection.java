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
 * The class can be used in a number of ways.  The typical use, to
 * handle a normal Gnutella connection, involves creating a
 * ConnectionManager and a thread:<p>
 *
 * <pre>
 *   Socket s=new Socket(...);
 *   Connection c=new Connection(.., s, false); //does handshake
 *   Thread t=new Thread(c);       //create new handler thread
 *   t.start();                    //services connection by calling run()
 * </pre>
 *
 * The second use is for "do it yourselfers".  This is useful for
 * Gnutella spiders.  In this case, you don't pass a ConnectionManager
 * as a constructor argument and don't call the run() method.
 */
public class Connection implements Runnable { 
    private ConnectionManager manager=null; //may be null
    private RouteTable routeTable=null;     //may be null
    private PushRouteTable pushRouteTable=null; 
    /** The underlying socket.  For thread synchronization reasons, it is important
     *  that this only be modified by the send(m) and receive() methods. 
     */
    private Socket sock;
    private boolean incoming;

    /** The number of packets I sent and received.  This includes bad packets. */
    private int sent;
    private int received;


    /** 
     * Creates an outgoing connection with a fresh socket.
     * 
     * @modifies network, manager
     * @effects creates an outgoing socket to the following host and port and does 
     *   the Gnutella handshake.   Throws IOException if the connection couldn't be 
     *   established.  If manager is non-null, also sends an initial PingRequest
     *   and register this with manager.
     *
     * @param manager the manager managing me and my "sibling" connections, or null
     *   if the run() method will not be called.
     * @param host the address of the host to contact, e.g., "192.168.0.1" or
     *   "gnutella.limegroup.com"
     * @param port the port of the host to contact, e.g, 6346
     * @exception IOException if the connection or the handshake failed
     */
    public Connection(ConnectionManager manager, String host, int port) 
	throws IOException {
	this(manager, new Socket(host, port), false);
    }

    /** 
     * Creates an outgoing or incoming connection around an existing socket.
     *
     * @modifies network, manager
     * @effects wraps a connection around sock and does the Gnutella handshake.
     *  Throws IOException if the connection couldn't be established.
     *  If manager is non-null, also registers this with manager and sends
     *  an initial PingRequest if this is outgoing..
     *
     * @param manager the manager managing me and my "sibling" connections, or null
     *   if the run() method will not be called.
     * @param sock the socket used for communication
     * @param incoming true if this is an incoming connection.
     *    False otherwise.
     * @exception IOException if the handshake failed
     */
    public Connection(ConnectionManager manager, Socket sock, boolean incoming) 
	throws IOException {
	this.manager=manager;
	this.sock=sock;
	this.incoming=incoming;
	if (manager!=null){
	    this.routeTable=manager.routeTable;
	    this.pushRouteTable = manager.pushRouteTable;
	}
	//Handshake
	final String CONNECT="GNUTELLA CONNECT/0.4\n\n";
	final String OK="GNUTELLA OK\n\n";
	if (incoming) {
	    expectString(CONNECT);
	    sendString(OK);
	} else { //outgoing
	    sendString(CONNECT);
	    expectString(OK);
	    if (manager!=null) {
		Message m=new PingRequest(Const.TTL);
		send(m);
		routeTable.put(m.getGUID(), this);
	    }
	}
	
	if (manager!=null)
	    manager.add(this);
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
     * @modifies the network underlying this
     * @effects send m on the network.  Throws IOException if problems
     *   arise.  This is thread-safe.
     */
    public void send(Message m) throws IOException {
	//Can't use same lock as receive()!
	OutputStream out=sock.getOutputStream();
	synchronized (out) {
	    m.write(out);
	    out.flush();
	    sent++;
	}
	System.out.println("Wrote "+m.toString()+"\n   to "+sock.toString());
    }

    /** 
     * See specification of Message.read. Note that this is
     *  non-blocking, but there is no hard guarantee on the maximum
     *  block time.  This is thread-safe.
     */
    public Message receive() throws IOException, BadPacketException {
	InputStream in=sock.getInputStream();
	//Can't use same lock as send()!
	synchronized(in) {
	    Message m=Message.read(in);
	    received++;  //keep statistics.
	    if (m!=null)
	    System.out.println("Read "+m.toString()+"\n    from "+sock.toString());
	    return m;
	}
    }

    /**
     * @requires the manager to this is non-null
     * @modifies the network underlying this, manager
     * @effects receives request and sends appropriate replies.
     *   Returns if either the connection is closed or an error happens.
     *   If this happens, removes itself from the manager's connection list.
     */
    public void run() {
	Assert.that(manager!=null && routeTable!=null && pushRouteTable!=null);
	try {
	    while (true) {
		Message m=null;
		try {
		    m=receive();
		    if (m==null)
			continue;
		} 
		catch (BadPacketException e) {
		    //System.out.println("Discarding bad packet ("+e.getMessage()+")");
		    continue;
		}
		if(m instanceof PingRequest){
		    Connection inConnection = routeTable.get(m.getGUID()); 
		    //connection has never been encountered before...
		    if (inConnection==null){
			//reduce TTL, increment hops. If old val of TTL was 0 drop message
			if (m.hop()!=0){
			    routeTable.put(m.getGUID(),this);//add to Reply Route Table
			    manager.sendToAllExcept(m, this);//broadcast to other hosts
			    byte[] ip=sock.getLocalAddress().getAddress(); //little endian
			    Message pingReply = new PingReply(m.getGUID(),m.getTTL(),manager.getListeningPort(),
							      ip,
							      0, //I think we will get this value from Rob's code
							      0); //Kilobytes also from Robs code
			    send(pingReply);
			}
			else{//TTL is zero
			    //do nothing (drop the message).
			}
		    }
		    else{// message has already been processed before
			//do nothing (drop message)
		    }
		}
		else if (m instanceof PingReply){
		    Connection outConnection = routeTable.get(m.getGUID());
		    if(outConnection!=null){ //we have a place to route it
			if (outConnection.equals(this)){ //I am the destination
			    manager.catcher.spy(m);//update hostcatcher
			    //TODO2: So what else do we have to do here??
			}
			else{//message needs to routed
			    outConnection.send(m);
			}
		    }
		    else { //Route Table does not know what to do with message
			//do nothing...drop the message
		    }
		}
		else if (m instanceof QueryRequest){
		    Connection inConnection = routeTable.get(m.getGUID());
		    if (inConnection==null){
			//reduce TTL,increment hops, If old val of TTL was 0 drop message
			if (m.hop()!=0){
			    routeTable.put(m.getGUID(),this); //add to Reply Route Table
			    manager.sendToAllExcept(m,this); //broadcast to other hosts
			    //TODO3: Rob does the search
			    //TODO3: Make the Query Reply message and send it out.
			    //Don't forget the client ID!
			    //send the packet
			}
			else{//TTL is zero
			    //do nothing(drop the message)
			}
		    }
		    else{//message has been entry in Route Table, has already been processed.
			//do nothing (drop message)
		    }
		}
		else if (m instanceof QueryReply){
		    Connection outConnection = routeTable.get(m.getGUID());
		    if(outConnection!=null){ //we have a place to route it
			System.out.println("Sumeet:found connection");
			pushRouteTable.put(this, m);//first store this in pushRouteTable
			System.out.println("Sumeet: stored reply in push route table");
			if (outConnection.equals(this)){ //I am the destination
			    //TODO1: This needs to be interfaced with Rob
			    //Unpack message
			    // and present it to user
			    //make HTTP connection with chosen message
			    //does this go here?
			    // interface with GUI client 
			}
			else {//message needs to be routed.
			    System.out.println("Sumeet:About to route reply");
			    outConnection.send(m);//send the message along on its route
			    System.out.println("Sumeet:Sent query reply");
			}
		    }
		    else{//route table does not know what to do this message
			//do nothing...drop the message
		    }
		}
		else if (m instanceof PushRequest){
		    System.out.println("Sumeet:We have a push request");
		    Connection nextHost = pushRouteTable.get(m);
		    PushRequest req = (PushRequest)m;
		    String DestinationId = new String(req.getClientGUID());
		    if (nextHost!=null){//we have a place to route this message
			nextHost.send(m); //send the message to appropriate host
		    }
		    else if (manager.ClientId.equals(DestinationId) ){//I am the destination
			//unpack message
			//make HTTP connection with originator
			//TODO1: Rob makes HHTTP connection
		    }
		    else{// the message has arrived in error
			//do nothing.....drop the message
		    }
		}// else if		
	    }//while
	}//try
	catch (Throwable t){
	    System.out.println("Error in Connection thread");
	}
    }//run

    public String toString() {
	return "Connection("+(incoming?"incoming":"outgoing")
	    +", "+sock.toString()+", "+sent+", "+received+")";
    }

    public boolean isOutgoing() {
	return !incoming;
    }

    /** Returns the port of the foreign host this is connected to. */
    public int getPort() {
	return sock.getPort();
    }

    /** Returns the port this is connected to locally. */
    public int getLocalPort() {
	return sock.getLocalPort();
    }

    /** Returns the address of the foreign host this is connected to. */
    public InetAddress getInetAddress() {
	return sock.getInetAddress();
    }

    /** Returns the local address of this. */
    public InetAddress getLocalAddress() {
	return sock.getLocalAddress();
    }

}
