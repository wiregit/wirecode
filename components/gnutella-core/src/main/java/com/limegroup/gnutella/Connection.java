package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.Buffer;

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

    /** The underlying socket, its address, and input and output
     *  streams.  sock, in, and out are null iff this is in the
     *  unconnected state.  For thread synchronization reasons, it is
     *  important that this only be modified by the send(m) and
     *  receive() methods.
     * 
     *  This implementation has two goals:
     *    1) a slow connection cannot prevent other connections from making 
     *       progress.  Packets must be dropped.
     *    2) packets should be sent in large batches to the OS, but the
     *       batches should not be so long as to cause undue latency.
     * 
     *  Towards this end, we queue sent messages on the front of
     *  outputQueue.  Whenever outputQueue contains at least
     *  BATCH_SIZE messages or QUEUE_TIME milliseconds has passed, the
     *  messages on outputQueue are written to out.  Out is then
     *  flushed exactly once. outputQueue is fixed size, so if the
     *  output thread can't keep up with the producer, packets will be
     *  (intentionally) droppped.  LOCKING: obtain outputQueueLock
     *  lock before modifying or replacing outputQueue.
     *
     *  One problem with this scheme is that IOExceptions from sending
     *  data happen asynchronously.  When this happens, connectionClosed
     *  is set to true.  Then the next time send is called, an IOException
     *  is thrown.  */
    protected String host;
    protected int port;
    protected Socket sock;
    protected InputStream in;
    protected OutputStream out;
    protected boolean incoming;

    /** The (approximate) max time a packet can be queued, in milliseconds. */
    private static final int QUEUE_TIME=750;
    /** The number of packets to present to the OS at a time.  This
     *  should be roughly proportional to the OS's send buffer. */
    private static final int BATCH_SIZE=50;
    /** The size of the queue.  This must be larger than BATCH_SIZE.
     *  Larger values tolerate temporary bursts of producer traffic
     *  without traffic but may result in overall latency. */
    private static final int QUEUE_SIZE=500;
    /** A lock to protect the swapping of outputQueue and oldOutputQueue. */
    private Object outputQueueLock=new Object();
    /** The producer's queue. */
    private volatile Buffer outputQueue=new Buffer(QUEUE_SIZE);
    /** The consumer's queue. */
    private volatile Buffer oldOutputQueue=new Buffer(QUEUE_SIZE);
    private boolean connectionClosed=false;
    /** True iff the output thread should write the queue immediately.
     *  Synchronized by outputQueueLock. */
    private boolean flushImmediately=false;
    /** A condition variable used to implement the flush() method.
     *  Call notify when outputQueueLock and oldOutputQueueLock are
     *  empty. */
    private Object flushLock=new Object();

    /** Trigger an opening connection to shutdown after it opens */
    private boolean doShutdown;

    /** 
     * Various statistics follow.  sent and received are the number of
     * packets I sent and received.  This includes dropped packets.
     * These are synchronized by out and in, respectively.
     *
     * recvDropped is the number of packets I read and dropped because
     * the host made one of the following errors: sent replies to
     * requests I didn't make, sent bad packets, or sent (route) spam.
     * It does not include: TTL's of zero, duplicate requests (it's
     * not their fault), or buffer overflows in sendToAll.  Note that
     * this is always less than received.  No synchronization is
     * necessary.
     *
     * sentDropped is the number of packets I dropped because the
     * output queue overflowed.  This happens when the remote host
     * cannot receive packets as quickly as I am trying to send them.
     * No synchronization is necessary.
     * 
     * lastSent/lastSentDropped and lastReceived/lastRecvDropped the
     * values of sent/sentDropped and received/recvDropped at the last
     * call to getPercentDropped.  These are synchronized by this;
     * finer-grained schemes could be used.
     */
    private int sent=0;
    private int sentDropped=0;
    private int lastSent=0;
    private int lastSentDropped=0;

    private int received=0;
    private int recvDropped=0;
    private int lastReceived=0;
    private int lastRecvDropped=0;

    /** Statistics about horizon size. */
    public long totalFileSize;
    public long numFiles;
    public long numHosts;

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
        SettingsManager settings=SettingsManager.instance();
        sendString(settings.getConnectString()+"\n\n");
        expectString(settings.getConnectOkString()+"\n\n");
        if ( doShutdown )
            sock.close();

        Thread t=new Thread(new OutputRunner());
        t.setDaemon(true);
        t.start();
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
        SettingsManager settings=SettingsManager.instance();
        expectString(settings.getConnectStringRemainder()+"\n\n");
        sendString(settings.getConnectOkString()+"\n\n");
        if ( doShutdown )
            sock.close();


        Thread t=new Thread(new OutputRunner());
        t.setDaemon(true);
        t.start();
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

    private static boolean disposeable(Message m) {
        return  ((m instanceof PingRequest) && (m.getHops()!=0))
          || (m instanceof PingReply);
    } 

    /**
     * Sends a message.
     * 
     * @requires this is in the CONNECTED state
     * @modifies the network underlying this
     * @effects send m on the network.  Throws IOException if problems
     *   arise.  This is thread-safe and guaranteed not to block.
     */
    public void send(Message m) throws IOException {
        if (connectionClosed)
            throw new IOException();

        synchronized (outputQueueLock) {
            sent++;
            if (outputQueue.isFull()) {
                //Drop case. Instead of using a FIFO replacement scheme, we
                //use the following:
                //  1) Throw away m if it is (a) a ping request
                //     whose hops count is not zero or (b) a pong.  
                //  2) If that doesn't work, throw away the oldest message
                //     message meeting above criteria.
                //  3) If that doesn't work, throw away the oldest message.
                sentDropped++;
                if (disposeable(m)) 
                    return;                
                
                //It's possible to optimize this by keeping track of the
                //last value of i, but case (1) occurs more frequently.
                int i;
                for (i=outputQueue.getSize()-1; i>=0; i--) {
                    Message mi=(Message)outputQueue.get(i);
                    if (disposeable(mi))
                        break;
                }

                if (i>=0)
                    outputQueue.set(i,m);
                else
                    outputQueue.addFirst(m);
            } else {
                //Normal case.
                outputQueue.addFirst(m);
                if (outputQueue.getSize() >= BATCH_SIZE)
                    outputQueueLock.notify();
            }
        }
    }

    /** 
     * @requires no other threads are calling send() or flush()
     * @effects block until all queued data is written.  Normally,
     *  there is no need to call this method; the output buffers are
     *  automatically flushed every few seconds (at most).  However, it
     *  may be necessary to call this method in situations where high
     *  latencies are not tolerable, e.g., in the network
     *  discoverer. 
     */
    public void flush() {
        synchronized (outputQueueLock) {
            flushImmediately=true;
            outputQueueLock.notify();
        }
        synchronized (flushLock) {
            while (! (outputQueue.isEmpty() && oldOutputQueue.isEmpty())) {
                try {
                    flushLock.wait();
                } catch (InterruptedException e) { }
                try {
                    //Flush is needed in case the wait() returns
                    //prematurely.
                    out.flush();
                } catch (IOException e) { /* throw to caller? */ }
            }
        }
    }

    /** Repeatedly sends all the queued data every few seconds. */
    private class OutputRunner implements Runnable {
        public void run() {
            while (!connectionClosed) {
                //1. Wait until (1) the queue is full or (2) the
                //maximum allowable send latency has passed (and the
                //queue is not empty)... 
                synchronized (outputQueueLock) {
                    try {
                        if (!flushImmediately && outputQueue.getSize()<BATCH_SIZE)
                            outputQueueLock.wait(QUEUE_TIME);
                    } catch (InterruptedException e) { }
                    flushImmediately=false;
                    if (outputQueue.isEmpty())
                        continue;
                    //...and swap outputQueue and oldOutputQueue.
                    Buffer tmp=outputQueue;
                    outputQueue=oldOutputQueue;  outputQueue.clear();
                    oldOutputQueue=tmp;
                }

                //2. Now send all the data on the old queue.
                //No need for any locks here since there is only one
                //OutputRunner thread.
                Assert.that(sock!=null && in!=null && out!=null, 
                            "Illegal socket state for send");
                while (! oldOutputQueue.isEmpty()) {
                    Message m=(Message)oldOutputQueue.removeLast();
                    try {
                        m.write(out);
                    } catch (IOException e) {
                        //An IO error while sending will be detected next
                        //time send() is called.
                        connectionClosed=true;
                        break;
                    }
                    if (manager!=null)
                        manager.total++;
                }                

                //3. Flush.  IOException can also happen hear.  Treat as above.
                try {
                    out.flush();
                } catch (IOException e) {
                    connectionClosed=true;
                }
                synchronized(flushLock) {
                    //note that oldOutputQueue.isEmpty()
                    if (outputQueue.isEmpty())
                        flushLock.notify();
                }
            }
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
            manager.totDropped++;
            recvDropped++;
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
        //  Disabled 9/1/2000 by crohrs.
        //      try {
        //          sock.setSoTimeout(SettingsManager.instance().getTimeout());
        //      } catch (SocketException e) {
        //          //Ignore?
        //      }
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
                    //recvDropped++;
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
                
                            int kilobytes = fm.getSize()/1024;
                            int num_files = fm.getNumFiles();

                            Message pingReply = 
							new PingReply(m.getGUID(),
										  (byte)(m.getHops()+1),
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
                        manager.totDropped++;
                        recvDropped++;
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
                                byte[] clientGUID = manager.ClientId.bytes();

                                // changing the port here to test push:
                
                                //Modified by Sumeet Thadani
                                // If the number of responses is more 255, we are going to 
                                // drop the responses after index 255. This can be corrected 
                                //post beta, so that the extra responses can be sent along as
                                //another query reply.
                                if (responses.length > 255){
                                    Response[] res = new Response[255];
                                    for(int i=0; i<255;i++)
                                        res[i] = responses[i]; //copy first 255 elements of old array
                                    responses = res;//old array will be garbage collected
                                }
                                QueryReply qreply = new QueryReply(guid, ttl, port, ip, 
                                                                   speed, responses, clientGUID);
                                //QueryReply qreply = new QueryReply(guid, ttl, 1234, ip, 
                                //               speed, responses, clientGUID);                
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
                        manager.totDropped++;
                        recvDropped++;
                    }
                }
                else if (m instanceof PushRequest){

                    //System.out.println("Requested a push");

                    if (manager.stats==true)
                        manager.pushCount++;//keeps stats if stats turned on
                    PushRequest req = (PushRequest)m;

                    byte[] req_guid = req.getClientGUID();
                    String req_guid_hexstring = (new GUID(req_guid)).toString();
                    Connection nextHost = pushRouteTable.get(req_guid);

                    if (nextHost!=null && routeFilter.allow(m)){//we have a place to route this message
                        m.hop(); // Ok to send even if ttl =0 since the message has a specific place to go
                        nextHost.send(m); //send the message to appropriate host
                    }
            
                    // This comparison doesn't work:
                    // // if (manager.ClientId.equals(DestinationId) ){
                    //I am the destination
                    else if (manager.isClient(req_guid)) {
				
						// Ignore excess upload requests
						if ( manager.getCallback().getNumUploads() >=
                             SettingsManager.instance().getMaxUploads() )
						    continue;

                        //unpack message
                        //make HTTP connection with originator
                        String host = new String(req.getIP());
                        byte[] ip = req.getIP();
                        StringBuffer buf=new StringBuffer();
                        buf.append(ByteOrder.ubyte2int(ip[0])+".");
                        buf.append(ByteOrder.ubyte2int(ip[1])+".");
                        buf.append(ByteOrder.ubyte2int(ip[2])+".");
                        buf.append(ByteOrder.ubyte2int(ip[3])+"");
                        String h = buf.toString();
                        int port = req.getPort();
            
                        FileManager fmanager = FileManager.getFileManager();
                        int index = (int)req.getIndex();
            
                        FileDesc desc;          
                        try {
                            desc =(FileDesc)fmanager._files.get(index); 
                        }
                        catch (Exception e) {
                            //I'm catching Exception because I don't know
                            //exactly which IndexOutOfBoundsException is
                            //thrown: from normal util package or com.sun...

                            //TODO?: You could connect and send 404 file
                            //not found....but why bother?
                            continue;
                        } 
                        String file = desc._name;
            
                        HTTPUploader up = new 
                        HTTPUploader(h, port, index, req_guid_hexstring, manager);
                        Thread t=new Thread(up);
                        t.setDaemon(true);
                        t.start();
                    }
                    else{// the message has arrived in error or is spam
                        //do nothing.....drop the message
                        manager.totRouteError++;
                        manager.totDropped++;
                        recvDropped++;
                    }
                }// else if     
            }//while
        }//try
        catch (IOException e){
       
            manager.remove(this);

        }
        catch (Exception e) {
            //Internal error!  Cleanup this connection and notify the user 
            //of the problem.
            manager.remove(this);
            ActivityCallback ui=manager.getCallback();
            if (ui!=null) {
                ui.error(ActivityCallback.ERROR_20, e);
            }
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

    /** Returns the number of messages I tried to send on this connection.
     *  Some may have been dropped. */
    public long getNumSent() {
        return sent;
    }

    /** Returns the number of messages I dropped while trying to send
     *  on this connection.  This happens when the remote host cannot
     *  keep up with me. */
    public long getNumSentDropped() {
        return sentDropped;
    }

    /** 
     * @modifies this
     * @effects Returns the percentage of messages sent on this
     *  since the last call to getPercentSentDropped that were 
     *  dropped by this end of the connection.
     */
    public synchronized float getPercentSentDropped() {
        int rdiff=sent-lastSent;
        int ddiff=sentDropped-lastSentDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);
    
        lastSent=sent;
        lastSentDropped=sentDropped;
        return percent;
    }

    /** Returns the number of messages received on this connection.
     *  Some may have been dropped. */
    public long getNumReceived() {
        return received;
    }

    /** Returns the number of bad messages received on this connection.<p>
     * 
     * Here, dropped messages mean replies to requests I didn't make,
     * bad packets, broadcasted pushes, or spam.  It does not include:
     * TTL's of zero, duplicate requests (it's not their fault), or
     * buffer overflows in sendToAll.  
     */
    public long getNumRecvDropped() {
        return recvDropped;
    }

    /** 
     * @modifies this
     * @effects Returns the percentage of messages received since the
     *  last call to getRecvPercentDropped that were bad. 
     */
    public synchronized float getPercentRecvDropped() {
        int rdiff=received-lastReceived;
        int ddiff=recvDropped-lastRecvDropped;
        float percent=(rdiff==0) ? 0.f : ((float)ddiff/(float)rdiff*100.f);
    
        lastReceived=received;
        lastRecvDropped=recvDropped;
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

    
    ///** Unit test */
    /*
    public static void main(String args[]) {
        //1. Test replacement policies.
        Message qr=new QueryRequest((byte)5, 0, "test");
        Message qr2=new QueryRequest((byte)5, 0, "test2");
        Message preq=new PingRequest((byte)5); preq.hop(); //from other
        Message preq2=new PingRequest((byte)5);            //from me
        Message prep=new PingReply(new byte[16], (byte)5, 6346,
                                   new byte[4], 0, 0);

        Connection c=new Connection("localhost", 6346);
        try {
            //   a') Regression test
            c.send(qr);
            c.send(qr2);
            Assert.that(c.outputQueue.get(0)==qr2);
            Assert.that(c.outputQueue.get(1)==qr);

            for (int i=0; i<QUEUE_SIZE-2; i++) {
                Assert.that(! c.outputQueue.isFull());
                c.send(qr);
            }
            Assert.that(c.outputQueue.isFull());
                    
            //   a) No pings or pongs.  Boot oldest.
            c.send(preq2);
            Assert.that(c.outputQueue.isFull());
            Assert.that(c.outputQueue.get(0)==preq2);

            //   b) Old ping request in last position
            c.outputQueue.set(QUEUE_SIZE-1, preq);
            c.send(preq2);
            Assert.that(c.outputQueue.get(QUEUE_SIZE-1)==preq2);
        
            //   c) Old ping reply in second to last position.
            c.outputQueue.set(QUEUE_SIZE-2, prep);
            c.send(qr);
            Assert.that(c.outputQueue.get(QUEUE_SIZE-1)==preq2);
            Assert.that(c.outputQueue.get(QUEUE_SIZE-2)==qr);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.that(false, "IOException");
        }
    }    
    */
}
