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
    private RouteTable pushRouteTable=null; 
    /** The underlying socket.  For thread synchronization reasons, it is important
     *  that this only be modified by the send(m) and receive() methods.  Also, use
     *  in and out, buffered versions of the input and output streams, for writing.
     */
    private Socket sock;
    private InputStream in;
    private OutputStream out;
    private boolean incoming;

    /** The number of packets I sent and received.  This includes bad packets. */
    private int sent;
    private int received;
    private static int total;


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
	this.in=new BufferedInputStream(sock.getInputStream());
	this.out=new BufferedOutputStream(sock.getOutputStream());
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
	synchronized (out) {
	    m.write(out);
	    out.flush();
	    sent++;
	    total++;
	}
	//System.out.println("Wrote "+m.toString()+"\n   to "+sock.toString());
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
	    total++;
	    //if (m!=null)
	    //System.out.println("Read "+m.toString()+"\n    from "+sock.toString());
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

			    FileManager fm = FileManager.getFileManager();
			    
			    int kilobytes = fm.getSize();
			    int num_files = fm.getNumFiles();

			    Message pingReply = new PingReply(m.getGUID(),m.getTTL(),
							      sock.getLocalPort(),
							      ip, num_files, kilobytes);

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

			    manager.totalSize += ((PingReply)m).getKbytes();
			    manager.totalFiles += ((PingReply)m).getFiles();
			    
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
			    FileManager fm = FileManager.getFileManager();
			    Response[] responses = fm.query((QueryRequest)m);
			    //TODO3: Make the Query Reply message and send it out.

			    byte[] guid = m.getGUID();
			    System.out.println("the guid " + guid);
			    byte ttl = Const.TTL;
			    System.out.println("the ttl " + ttl);
			    int port = manager.getListeningPort();
			    System.out.println("the port " + port);

			    byte[] ip=sock.getLocalAddress().getAddress(); //little endian
			    System.out.println("the ip " + ip);
			    // long speed = ((QueryRequest)m).getMinSpeed();
			    long speed = 0;
			    System.out.println("the speed " + speed);
			    byte[] clientGUID = manager.ClientId.getBytes();
			    System.out.println("the client GUID " + clientGUID);
			    // byte[] clientGUID = null;
			    QueryReply qreply = new QueryReply(guid, ttl, port, ip, 
					   speed, responses, clientGUID);
			    
			    //Don't forget the client ID!
			    //send the packet
			    send(qreply);



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
		    System.out.println("Recieved a Querry Reply");
		    Connection outConnection = routeTable.get(m.getGUID());
		    if(outConnection!=null){ //we have a place to route it
			//System.out.println("Sumeet:found connection");
			QueryReply qrep = (QueryReply)m;
			pushRouteTable.put(qrep.getClientGUID(),this);//first store this in pushRouteTable
			//System.out.println("Sumeet: stored reply in push route table");
			if (outConnection.equals(this)){ //I am the destination
			    //TODO1: This needs to be interfaced with Rob
			    //Unpack message
			    // and present it to user
			    //make HTTP connection with chosen message
			    //does this go here?
			    // interface with GUI client 

			    


			}
			else {//message needs to be routed.
			    //System.out.println("Sumeet:About to route reply");
			    outConnection.send(m);//send the message along on its route
			    //System.out.println("Sumeet:Sent query reply");
			}
		    }
		    else{//route table does not know what to do this message
			//do nothing...drop the message
		    }
		}
		else if (m instanceof PushRequest){
		    System.out.println("Sumeet:We have a push request");
		    PushRequest req = (PushRequest)m;
		    String DestinationId = new String(req.getClientGUID());
		    Connection nextHost = pushRouteTable.get(req.getClientGUID());
		    //System.out.println("Sumeet: push request goes to"+nextHost.toString());
		    //System.out.println("Sumeet: Destination ID" + DestinationId);
		    if (nextHost!=null){//we have a place to route this message
			//System.out.println("Sumeet : We have a host to route push to..");
			nextHost.send(m); //send the message to appropriate host
		    }
		    else if (manager.ClientId.equals(DestinationId) ){//I am the destination
			//System.out.println("Sumeet:I am the destination"); 
			//unpack message
			//make HTTP connection with originator
			//TODO1: Rob makes HHTTP connection
		    }
		    else{// the message has arrived in error
			//System.out.println("Sumeet: Message arrived in error");
			//do nothing.....drop the message
		    }
		}// else if		
	    }//while
	}//try
	catch (IOException e){
	    ConnectionManager.error("Connection closed: "+sock.toString());
	    manager.remove(this);
	}
    }//run

    public String toString() {
	return "Connection("+(incoming?"incoming":"outgoing")
	    +", "+sock.toString()+", "+sent+", "+received+")";
    }

    public boolean isOutgoing() {
	return !incoming;
    }

    /**
     *  Shutdown the Connections socket and thus the connection itself.
     */
    public void shutdown() {
	try {
	    sock.close();
	} catch(Exception e) {}
    }

    /**
     *  Return the total number of messages sent and received
     */
    public static int getTotalMessages() {
	return( total );
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
