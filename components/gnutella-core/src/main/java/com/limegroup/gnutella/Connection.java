package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import java.util.*;


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
 *   PingRequest pr=new PingRequest(SettingsManager.instance().getTTL()); //Send initial ping.
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
 * it is intentional, as it makes interfacing with the GUI easier.<p>
 *
 * All connections have two underlying spam filters: a personal filter
 * (controls what I see) and a route filter (also controls what I pass
 * along to others).  See SpamFilter for a description.  These 
 * filters are configured by the properties in the SettingsManager, but
 * you can change them with setPersonalFilter and setRouteFilter.
 */
public class Connection implements Runnable { 
    /** These are only needed for the run method.
     *  INVARIANT: (manager==null)==(routeTable==null)==(pushRouteTable==null) 
     *             routeTable==manager.routeTable //i.e., it's shared
     *             pushRouteTable==manager.routeTable //i.e., it's shared
     */
    protected ConnectionManager manager=null;
    protected RouteTable routeTable=null;    
    protected RouteTable pushRouteTable=null;
    protected volatile SpamFilter routeFilter=SpamFilter.newRouteFilter();
    protected volatile SpamFilter personalFilter=SpamFilter.newPersonalFilter();

    /** The underlying socket.  sock, in, and out are null iff this is in
     *  the unconnected state.
     * 
     *  For thread synchronization reasons, it is important that this
     *  only be modified by the send(m) and receive() methods.  Also,
     *  only use in and out, buffered versions of the input and output
     *  streams, for writing.  
     */

    protected String host;
    protected int port;
    protected Socket sock;
    protected InputStream in;
    protected OutputStream out;
    protected boolean incoming;


    /** Trigger an opening connection to shutdown after it opens */
    private boolean doShutdown;

    /** 
     * The number of packets I sent and received.  This includes bad
     * packets.  These are synchronized by out and in, respectively.
     *
     * Dropped is the number of packets I read (<read) and dropped because the
     * host made one of the following errors: sent replies to requests
     * I didn't make, sent bad packets, or sent (route) spam.  It does
     * not include: TTL's of zero, duplicate requests (it's not their
     * fault), or buffer overflows in sendToAll.  
     *
     * lastReceived and lastDropped are the values of received and
     * dropped at the last call to getPercentDropped.
     */
    protected int sent=0;
    protected int received=0;
    protected int dropped=0;
    protected int lastReceived=0;
    protected int lastDropped=0;

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
     * The connection has a default spam filter, as determined by
     * settings manager.
     */
    public Connection(String host, int port) {	
	this.host=host;
	this.port=port;
	this.incoming=false;
	this.doShutdown=false;
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
	if ( doShutdown )
	    sock.close();
    }

    /** 
     * Allows for the creation of an incoming connection.
     * Note:  You must call init incoming for a incoming socket.
     * The connection has a default spam filter, as determined by settings
     * manager.
     */
    public Connection(String host, int port, boolean incoming) {	
	this.host=host;
	this.port=port;
	this.incoming=incoming;
	this.doShutdown=false;
    }    

    /** 
     * Initialize an incoming, connected connection around an existing socket.
     *
     * @requires the word "GNUTELLA " and nothing else has just been read from sock
     * @modifies network
     * @effects wraps a connection around sock and does the rest of the Gnutella
     *  handshake.  Throws IOException if the connection couldn't be established.
     *  DOES NOT ADD THIS TO MANAGER. 
     */
    public void initIncoming(Socket sock) throws IOException {
	this.sock=sock;
	this.in=new BufferedInputStream(sock.getInputStream());
	this.out=new BufferedOutputStream(sock.getOutputStream());
	this.incoming=true;

	//Handshake
	expectString(CONNECT_WITHOUT_FIRST_WORD);
	sendString(OK);
	if ( doShutdown )
	    sock.close();
    }    

    protected synchronized void sendString(String s) throws IOException {
	//TODO1: timeout.
	byte[] bytes=s.getBytes();
	OutputStream out=sock.getOutputStream();
	out.write(bytes);
	out.flush();
    }

    /**
     * @modifies network
     * @effects attempts to read s.size() characters from the network/
     *  If they do not match s, throws IOException.  If the characters
     *  cannot be read within TIMEOUT milliseconds (as defined by the
     *  property manager), throws IOException.
     */
    protected synchronized void expectString(String s) throws IOException {
	int oldTimeout=sock.getSoTimeout();
	try {
	    sock.setSoTimeout(SettingsManager.instance().getTimeout());
	    byte[] bytes=s.getBytes();
	    for (int i=0; i<bytes.length; i++) {
		int got=in.read();
		if (got==-1)
		    throw new IOException();
		if (bytes[i]!=(byte)got)
		    throw new IOException();
	    }
	} finally {
	    //Restore socket timeout.
	    sock.setSoTimeout(oldTimeout);
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
    }

    /** 
     * Receives a message.
     *
     * @requires this is in the CONNECTED state
     * @effects exactly like Message.read(), but blocks until a 
     *  message is available.  A half-completed message
     *  results in InterruptedIOException.
     */
    public Message receive() throws IOException, BadPacketException {
	Assert.that(sock!=null && in!=null && out!=null, "Illegal socket state for receive");
	//Can't use same lock as send()!
	synchronized(in) {
	    while (true) {
		Message m=Message.read(in);
		if (m==null) 
		    continue;
  		//System.out.println("Read "+m.toString());
		if (manager!=null)
		    manager.total++;
		received++;  //keep statistics.
		return m;
	    }
	}
    }

    /**
     * Receives a message with timeout.
     *
     * @requires this is in the CONNECTED state
     * @effects exactly like Message.read(), but throws InterruptedIOException if
     *  timeout!=0 and no message is read after "timeout" milliseconds.  In this
     *  case, you should terminate the connection, as half a message may have been 
     *  read.
     */
    public Message receive(int timeout) 
	throws IOException, BadPacketException, InterruptedIOException {
	synchronized (in) {
	    //temporarily change socket timeout.
	    int oldTimeout=sock.getSoTimeout();
	    sock.setSoTimeout(timeout);
	    try {
		Message m=Message.read(in);
		if (m==null)
		    throw new InterruptedIOException();
		if (manager!=null)
		    manager.total++;
		received++;  //keep statistics.
		return m;		
	    } finally {
		sock.setSoTimeout(oldTimeout);
	    }
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

    private final boolean isRouteSpam(Message m) {
	if (! routeFilter.allow(m)) {
	    dropped++;
	    return true;
	} else
	    return false;
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
	//We won't wait more than TIMEOUT milliseconds for a half-completed message.
	//However, we will wait as long as necessary for an incomplete message.
	//See Message.read(..) for an explanation.
	try {
	    sock.setSoTimeout(SettingsManager.instance().getTimeout());
	} catch (SocketException e) {
	    //Ignore?
	}
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
		    //dropped++;
		    continue;
		}
		if(m instanceof PingRequest){
		    Connection inConnection = routeTable.get(m.getGUID()); 
		    //connection has never been encountered before...
		    if (inConnection==null && !isRouteSpam(m)){
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

			    Message pingReply = new PingReply(m.getGUID(),(byte)(m.getHops()+1),
							      manager.getListeningPort(),
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
		    // System.out.println("Sumeet: Getting ping reply");
		    Connection outConnection = routeTable.get(m.getGUID());
		    manager.catcher.spy(m);//update hostcatcher (even if this isn't for me)
		    if(outConnection!=null && routeFilter.allow(m)){ //we have a place to route it
			if (manager.stats==true)
			    manager.PRepCount++; //keep stats if stats is turned on
			//HACK: is the reply for me?
			if (outConnection.equals(manager.ME_CONNECTION)) {
			    //System.out.println("Sumeet: I am the destination");
			    //Update horizon stats.
			    //This is not necessarily atomic, but that doesn't matter.
			    totalFileSize += ((PingReply)m).getKbytes();
			    numFiles += ((PingReply)m).getFiles();
			    numHosts++;			   
			    //System.out.println("Sumeet : updated stats"); 
			}
			else{//message needs to routed
			    m.hop();// It's Ok to route even if TTL is zero since this is a reply
			    outConnection.send(m);
			}
		    }
		    else { //Route Table does not know what to do with message
			//do nothing...drop the message
			dropped++;
		    }
		}
		else if (m instanceof QueryRequest){
		    Connection inConnection = routeTable.get(m.getGUID());
		    if (inConnection==null && !isRouteSpam(m)){

			// Feed to the UI Monitor
			ActivityCallback ui=manager.getCallback();
			if (ui!=null && personalFilter.allow(m)) 
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
				byte ttl = (byte)(m.getHops() +1);
				int port = manager.getListeningPort();
				byte[] ip=sock.getLocalAddress().getAddress(); //little endian
				long speed = SettingsManager.instance().getConnectionSpeed();
				//byte[] clientGUID = manager.ClientId.getBytes(); - This should
				// have been a bug
				byte[] clientGUID = GUID.fromHexString(manager.ClientId);

				// changing the port here to test push:
			  	QueryReply qreply = new QueryReply(guid, ttl, port, ip, 
  								   speed, responses, clientGUID);

		//  		QueryReply qreply = new QueryReply(guid, ttl, 1234, ip, 
//  								   speed, responses, clientGUID);

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
		    Connection outConnection = routeTable.get(m.getGUID());
		    if(outConnection!=null && routeFilter.allow(m)){ //we have a place to route it
			if (manager.stats==true)
			    manager.QRepCount++;
			//System.out.println("Sumeet:found connection");
			QueryReply qrep = (QueryReply)m;
			pushRouteTable.put(qrep.getClientGUID(),this);//first store this in pushRouteTable
			//HACK: is the reply for me?
			if (outConnection.equals(manager.ME_CONNECTION)) {
			    //Unpack message and present it to user via ActivityCallback,
			    //if it is not spam.
			    ActivityCallback ui=manager.getCallback();
			    if (ui!=null && personalFilter.allow(m)) 
				ui.handleQueryReply((QueryReply)m);
			}
			else {//message needs to be routed.
			    m.hop(); // It's Ok to route even if TTL is zero since this is a reply
			    outConnection.send(m);//send the message along on its route
			}
		    }
		    else{//route table does not know what to do this message
			//do nothing...drop the message
			dropped++;
		    }
		}
		else if (m instanceof PushRequest){

		    //System.out.println("Requested a push");

		    if (manager.stats==true)
			manager.pushCount++;//keeps stats if stats turned on
		    PushRequest req = (PushRequest)m;

		    byte[] req_guid = req.getClientGUID();

		    String DestinationId = new String(req.getClientGUID());
		    Connection nextHost = pushRouteTable.get(req.getClientGUID());


		    byte[] client_id = GUID.fromHexString(manager.ClientId);
		    String client_str = new String(client_id);

		    if (nextHost!=null && routeFilter.allow(m)){//we have a place to route this message
			m.hop(); // Ok to send even if ttl =0 since the message has a specific place to go
			nextHost.send(m); //send the message to appropriate host
		    }
		    
		    // This comparison doesn't work:
		    // // if (manager.ClientId.equals(DestinationId) ){
		      //I am the destination
		    else if (client_str.equals(DestinationId)) {
			// else
			
			//unpack message
			//make HTTP connection with originator
			//TODO1: Rob makes HHTTP connection
			System.out.println("Establishing HTTP");


			
			String host = new String(req.getIP());

			System.out.println("Host in Conn " + host);
			System.out.println("Host in Conn " + req.getIP());

			byte[] ip = req.getIP();

			StringBuffer buf=new StringBuffer();
			buf.append(ByteOrder.ubyte2int(ip[0])+".");
			buf.append(ByteOrder.ubyte2int(ip[1])+".");
			buf.append(ByteOrder.ubyte2int(ip[2])+".");
			buf.append(ByteOrder.ubyte2int(ip[3])+"");
			String h = buf.toString();

			System.out.println("Host in Conn " + h);


			int port = req.getPort();
			
			FileManager fmanager = FileManager.getFileManager();
			int index = (int)req.getIndex();

			System.out.println("THe index is " + index); 

			FileDesc desc = (FileDesc)fmanager._files.get(index);
			System.out.println("THe size is " + fmanager._files.size()); 

			String file = desc._name;
			
			HTTPUploader up = new 
			    HTTPUploader("http", h, port, file, manager);

			up.run();

		    }
		    else{// the message has arrived in error or is spam
			//do nothing.....drop the message
			dropped++;
		    }
		}// else if		
	    }//while
	}//try
	catch (IOException e){
	    manager.remove(this);
	    //e.printStackTrace();
	}
    }//run

    public String toString() {
	return "Connection("+(incoming?"incoming":"outgoing")
	    +", "+host+":"+port+", "+sent+", "+received+")";
    }

    public boolean isOutgoing() {
	return !incoming;
    }

    /**
     *  Shutdown the Connections socket and thus the connection itself.
     */
    public void shutdown() {
	doShutdown = true;
	try {
	    sock.close();
	} catch(Exception e) {}
    }

    /** Returns the host set at construction */
    public String getOrigHost() {
	return host;
    }

    /** Returns the port set at construction */
    public int getOrigPort() {
	return port;
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

    /** Returns the number of messages sent on this connection */
    public long getNumSent() {
	return sent;
    }

    /** Returns the number of messages received on this connection */
    public long getNumReceived() {
	return received;
    }

    /** Returns the number of messages dropped on this connection.<p>
     * 
     * Here, dropped messages mean replies to requests I didn't make,
     * bad packets, broadcasted pushes, or spam.  It does not include:
     * TTL's of zero, duplicate requests (it's not their fault), or
     * buffer overflows in sendToAll.  
     */
    public long getNumDropped() {
	return dropped;
    }

    /** Return the percentage of messages dropped on this connection since
     *  the last call to getPercentDropped. */
    public float getPercentDropped() {
	int rdiff=received-lastReceived;
	int ddiff=dropped-lastDropped;
	float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);
	
	lastReceived=received;
	lastDropped=dropped;
	return percent;
    }

    /** 	
     * @modifies this
     * @effects sets the underlying routing filter.   Note that
     *  most filters are not thread-safe, so they should not be shared
     *  among multiple connections.
     */
    public void setRouteFilter(SpamFilter filter) {
	this.routeFilter=filter;
    }

    /** 	
     * @modifies this
     * @effects sets the underlying personal filter.   Note that
     *  most filters are not thread-safe, so they should not be shared
     *  among multiple connections.
     */
    public void setPersonalFilter(SpamFilter filter) {
	this.personalFilter=filter;
    }

}
