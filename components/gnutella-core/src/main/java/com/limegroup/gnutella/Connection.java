package com.limegroup.gnutella;

import java.net.*;
import java.io.*;

/**
 * A Gnutella connection. A connection is either INCOMING or OUTGOING;
 * the difference doesn't really matter. It is also in one of three
 * states: UNCONNECTED, CONNECTING, or CONNECTED.<p>
 *
 * This class provides the core logic for handling the Gnutella
 * protocol.  This includes sending replies to requests but not
 * details of routing.  The Message class (and subclasses)
 * actually does the tedious reading/writing of bytes from socket.<p>
 * 
 * The class can be used in a number of ways.  The typical use, to
 * handle a normal Gnutella connection, involves creating a
 * ConnectionManager and a thread.<p>
 *
 * <pre>
 *   //1. Setup manager and connection.
 *   ConnectionManager cm=new ConnectionManager();
 *   Connection c=new Connection(host, port);
 *   c.connect();                  //actually connect
 *
 *   //2. Send initial ping request.  The second line is a complete hack
 *   //   hack so that we know responses are for this.
 *   PingRequest pr=new PingRequest(Const.TTL); //Send initial ping.
 *   cm.fromMe(pr);
 *   c.send(pr); 
 *
 *   //3. Handle everything else asynchronously
 *   cm.add(c);                    //connnections in cm can broadcast via c
 *   c.setManager(cm);             //c can broadcast via cm
 *   Thread t=new Thread(c);       //create new handler thread
 *   t.start();                    //services connection by calling run()
 * </pre>
 *
 * The second use is for "do it yourselfers".  This is useful for
 * Gnutella spiders.  In this case, you don't set a ConnectionManager
 * and don't call the run() method.<p>
 *
 * You will note that the first constructor doesn't actually connect
 * this.  For that you must call connect().  While this is awkward,
 * it is intentional, as it makes interfacing with the GUI easier.
 */
public class Connection implements Runnable { 
    /** These are only needed for the run method.
     *  INVARIANT: (manager==null)==(routeTable==null)==(pushRouteTable==null) 
     */
    private ConnectionManager manager=null; //may be null
    private RouteTable routeTable=null;     //may be null
    private RouteTable pushRouteTable=null; 

    /** The underlying socket.  sock, in, and out are null iff this is in
     *  the unconnected state.
     * 
     *  For thread synchronization reasons, it is important that this
     *  only be modified by the send(m) and receive() methods.  Also,
     *  only use in and out, buffered versions of the input and output
     *  streams, for writing.  
     */
    private String host;
    private int port;
    private Socket sock;
    private InputStream in;
    private OutputStream out;
    private boolean incoming;

    /** The number of packets I sent and received.  This includes bad packets. 
     *  These are synchronized by out and in, respectively. */
    private int sent=0;
    private int received=0;
    
    /** Statistics about horizon size. */
    public long totalFileSize;
    public long numFiles;
    public long numHosts;

    static final String CONNECT="GNUTELLA CONNECT/0.4\n\n";
    static final String CONNECT_WITHOUT_FIRST_WORD="CONNECT/0.4\n\n";
    static final String OK="GNUTELLA OK\n\n";

    /** 
     * Creates a new outgoing connection in the unconnected state.
     * You must call connect() to actually establish the connection.
     */
    public Connection(String host, int port) {	
	this.host=host;
	this.port=port;
	this.incoming=false;
    }

    /** 
     * Connects an unconnected/outgoing connection.
     * 
     * @requires this is in the unconnected state
     * @modifies this, network
     * @effects connects to host:port, and does the Gnutella handshake.
     *  Throws IOException if this failed.
     *  DOES NOT SEND INITIAL PING REQUEST OR ADD THIS TO MANAGER.
     */
    public synchronized void connect() throws IOException {
	Assert.that(sock==null && in==null && out==null, "Illegal state");
	//Entering CONNECTING state.  TODO: notify GUI
	sock=new Socket(host, port);
	//Entering CONNECTED state.  TODO: notify GUI
	in=new BufferedInputStream(sock.getInputStream());
	out=new BufferedOutputStream(sock.getOutputStream());

	//Handshake
	sendString(CONNECT);
	expectString(OK);
    }

    /** 
     * Creates an incoming, connected connection around an existing socket.
     *
     * @requires the word "GNUTELLA " and nothing else has just been read from sock
     * @modifies network
     * @effects wraps a connection around sock and does the rest of the Gnutella
     *  handshake.  Throws IOException if the connection couldn't be established.
     *  DOES NOT ADD THIS TO MANAGER. 
     */
    public Connection(Socket sock) throws IOException {
	this.sock=sock;
	this.in=new BufferedInputStream(sock.getInputStream());
	this.out=new BufferedOutputStream(sock.getOutputStream());
	this.incoming=true;

	//Handshake
	expectString(CONNECT_WITHOUT_FIRST_WORD);
	sendString(OK);
    }    

    protected synchronized void sendString(String s) throws IOException {
	//TODO1: timeout.
	byte[] bytes=s.getBytes();
	OutputStream out=sock.getOutputStream();
	out.write(bytes);
	out.flush();
    }

    protected synchronized void expectString(String s) throws IOException {
	//TODO1: timeout.
	byte[] bytes=s.getBytes();
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
     * Sends a message.
     * 
     * @requires this is in the CONNECTED state
     * @modifies the network underlying this
     * @effects send m on the network.  Throws IOException if problems
     *   arise.  This is thread-safe.
     */
    public void send(Message m) throws IOException {
	Assert.that(sock!=null && in!=null && out!=null, "Illegal socket state for send");
	//Can't use same lock as receive()!
	synchronized (out) {
	    m.write(out);
	    out.flush();
	    sent++;	    
	    if (manager!=null)
		manager.total++;
	}
	//System.out.println("Wrote "+m.toString()+"\n   to "+sock.toString());
    }

    /** 
     * Receives a message.
     *
     * @requires this is in the CONNECTED state
     * @effects See specification of Message.read. Note that this <i>may</i> be
     *  non-blocking, but there is no hard guarantee on the maximum
     *  block time.  This is thread-safe.
     */
    public Message receive() throws IOException, BadPacketException {
	Assert.that(sock!=null && in!=null && out!=null, "Illegal socket state for receive");
	//Can't use same lock as send()!
	synchronized(in) {
	    Message m=Message.read(in);
	    received++;  //keep statistics.
	    if (manager!=null)
		manager.total++;
	    //if (m!=null)
	    //	System.out.println("Read "+m.toString()+"\n    from "+sock.toString());
	    return m;
	}
    }

    /** 
     * Sets the connection manager in preparation for run().
     * 
     * @requires manager!=null
     * @modifies this
     * @effects sets the manager to use for broadcasting.  
     *  It is only necessary to call this method if you are 
     *  going to call the run method.
     */
    public synchronized void setManager(ConnectionManager manager) {
	Assert.that(manager!=null, "Cannot set null manager");
	this.manager=manager;
	if (manager!=null){
	    this.routeTable=manager.routeTable;
	    this.pushRouteTable = manager.pushRouteTable;
	}
    }

    /**
     * Handles core Gnutella request/reply protocol.
     *
     * @requires this is in the CONNECTED state and
     *   the manager to this has been set via setManager
     * @modifies the network underlying this, manager
     * @effects receives request and sends appropriate replies.
     *   Returns if either the connection is closed or an error happens.
     *   If this happens, removes itself from the manager's connection list.
     */
    public void run() {
	Assert.that(sock!=null && in!=null && out!=null, "Illegal socket state for run");
	Assert.that(manager!=null && routeTable!=null && pushRouteTable!=null,
		    "Illegal manager state for run");
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
			if (manager.stats==true)
			    manager.PReqCount++;//keep track of statistics if stats is turned on.
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
			    if (manager.stats==true)
				manager.PRepCount++;//keep stats if stats is turned on
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
		    manager.catcher.spy(m);//update hostcatcher (even if this isn't for me)
		    if(outConnection!=null){ //we have a place to route it
			if (manager.stats==true)
			    manager.PRepCount++; //keep stats if stats is turned on
			//HACK: is the reply for me?
			if (outConnection.equals(manager.ME_CONNECTION)) { 
			    //Update horizon stats.
			    //This is not necessarily atomic, but that doesn't matter.
			    totalFileSize += ((PingReply)m).getKbytes();
			    numFiles += ((PingReply)m).getFiles();
			    numHosts++;			    
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

			// Feed to the UI Monitor
			ActivityCallback ui=manager.getCallback();
			if (ui!=null) 
			    ui.handleQueryString(((QueryRequest)m).getQuery());

			//reduce TTL,increment hops, If old val of TTL was 0 drop message
			//NOTE: This is Num Local Searches so always count it
			//if (manager.stats==true)  
			manager.QReqCount++;//keep stats if stats turned on

			if (m.hop()!=0){
			    routeTable.put(m.getGUID(),this); //add to Reply Route Table
			    manager.sendToAllExcept(m,this); //broadcast to other hosts
			 
			    FileManager fm = FileManager.getFileManager();
			    Response[] responses = fm.query((QueryRequest)m);
			    
			    if (responses.length > 0) {
				byte[] guid = m.getGUID();
				// System.out.println("the guid " + guid);
				byte ttl = Const.TTL;
				// System.out.println("the ttl " + ttl);
				int port = manager.getListeningPort();
				// System.out.println("the port " + port);
				byte[] ip=sock.getLocalAddress().getAddress(); //little endian
				// System.out.println("the ip " + ip);
				// System.out.println("the ip ");
				// for(int i = 0; i < ip.length; i++) {
				//     System.out.print(ip[i]);
				// }
				// long speed = ((QueryRequest)m).getMinSpeed();
				long speed = 0;
				// System.out.println("the speed " + speed);
				byte[] clientGUID = manager.ClientId.getBytes();
				// System.out.println("the client GUID ");
				
				// for(int i = 0; i < clientGUID.length; i++) {
				//    System.out.print(clientGUID[i]);
				// }
				
				// System.out.println("");
				
				QueryReply qreply = new QueryReply(guid, ttl, port, ip, 
								   speed, responses, clientGUID);
				send(qreply);
				if(manager.stats == true)
				    manager.QRepCount++;//keep stats if stats is turned on

			    }
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


		    // System.out.println("Recieved a Querry Reply");
		    Connection outConnection = routeTable.get(m.getGUID());
		    if(outConnection!=null){ //we have a place to route it
			if (manager.stats==true)
			    manager.QRepCount++;
			//System.out.println("Sumeet:found connection");
			QueryReply qrep = (QueryReply)m;
			pushRouteTable.put(qrep.getClientGUID(),this);//first store this in pushRouteTable
			//HACK: is the reply for me?
			if (outConnection.equals(manager.ME_CONNECTION)) { 
			    //Unpack message and present it to user via ActivityCallback.
			    ActivityCallback ui=manager.getCallback();
			    if (ui!=null) ui.handleQueryReply((QueryReply)m);
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
		    // System.out.println("Sumeet:We have a push request");
		    if (manager.stats==true)
			manager.pushCount++;//keeps stats if stats turned on
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
	    manager.remove(this);
	}
    }//run

    public String toString() {
	//hack!
	if (this==ConnectionManager.ME_CONNECTION)
	    return "Connection(<ME>)";
	//unconnected outgoing connection
	else if (sock==null) 
	    return "Connection(unconnected, "+host+":"+port+")";
	//normal, connected connection
	else
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

    /** Clears the statistics about files reachable from me. */
    public void clearHorizonStats() {
	totalFileSize=0;
	numHosts=0;
	numFiles=0;
    }

    /** Returns the number of hosts reachable from me. */
    public long getNumHosts() {
	return numHosts;
    }

    /** Returns the number of files reachable from me. */
    public long getNumFiles() {
	return numFiles;
    }

    /** Returns the size of all files reachable from me. */
    public long getTotalFileSize() {
	return totalFileSize;
    }   

//      public static void main(String args[]) {
//  	System.out.println(ConnectionManager.ME_CONNECTION.toString());
//  	System.out.println((new Connection("somehost.com",123)).toString());
//      }	
}
