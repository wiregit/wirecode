package com.limegroup.gnutella;

import com.limegroup.gnutella.util.Buffer;
import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

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
    private  RouteTable routeTable=new RouteTable(2048); //tweak as needed
    private  RouteTable pushRouteTable = new RouteTable(2048);//same as Route Table could be lower
    static final Connection ME_CONNECTION = new Connection();

    /* List of all connections.  This is implemented with two data structures:
     * a list for fast iteration, and a set for quickly telling what we're
     * connected to.
     *
     * INVARIANT: "connections" contains no duplicates, and "endpoints" contains
     * exactly those endpoints that could be made from the elements of
     * "connections".
     *
     * INVARIANT: numFetchers = max(0, keepAlive - connections.size())
     *            Number of fetchers equals number of connections needed, unless
     *            that number is less than zero.
     *
     * LOCKING: connections and endpoints must NOT BE MUTATED.  Instead they
     *          should be replaced as necessary with new copies.  Before
     *          replacing the structures, obtain this' monitor.
     *          *** All six of the following members should only be modified
     *              from threads that have this' monitor ***
     */
    private volatile List /* of ManagedConnection */ initializedConnections=
        new ArrayList();
    private volatile List /* of ManagedConnection */ connections=
        new ArrayList();
    private volatile Set /* of Endpoint */ endpoints=new HashSet();
    private List /* of ConnectionFetcher */ fetchers =
        new ArrayList();
    private List /* of ManagedConnection */ initializingFetchedConnections =
        new ArrayList();
    private int keepAlive=0;

    public HostCatcher catcher;

    /** Queued up entries to send to each */
    static class MessagePair {
        Message m;
        Connection except;
        MessagePair (Message m, Connection except) { this.m=m; this.except=except; }
    }
    private static final int MESSAGE_QUEUE_SIZE=500;
    private Buffer /* of MessagePair */ messageQueue=new Buffer(MESSAGE_QUEUE_SIZE);

    private ActivityCallback callback;
    private GUID clientId;

    /** Variables for statistical purposes */
    /*NOTE: THESE VARIABLES ARE NOT SYNCHRONIZED...SO THE STATISTICS MAY NOT BE 100% ACCURATE. */
    public int total; //total number of messages sent and received
    public int PReqCount; //Ping Request count
    public int PRepCount; //Ping Reply count
    public int QReqCount; //Query Request count
    public int QRepCount; //Query Reply count
    public int pushCount; //Push request count
    public int totDropped; //Total dropped messages
    public int totRouteError; //Total misrouted messages

    private Vector badHosts = new Vector();

    /** Creates a manager that listens on the default port. Equivalent to
     *  ConnectionManager(SettingsManager.instance().getPort()) */
    public ConnectionManager(ActivityCallback callback) {
        this(SettingsManager.instance().getPort(), callback);
    }

    /** Creates a manager that tries to listen to incoming connections
     * on the given port.  If this is a bad port, the port will be
     * changed when run is called and SettingsManager will be updated.
     * If that fails, ActivityCallback.error will be called.  A port
     * of 0 means do not accept incoming connections.
     */
    public ConnectionManager(int port, ActivityCallback callback) {
        this.port=port;
        this.callback = callback;
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
            catcher=new HostCatcher(this,
                                    SettingsManager.instance().getHostList());

        Thread broadcastThread=new Thread(new MessageBroadcaster());
        broadcastThread.setDaemon(true);
        broadcastThread.start();

        setKeepAlive(SettingsManager.instance().getKeepAlive());

        try {
            clientId = new GUID(GUID.fromHexString(
                SettingsManager.instance().getClientID()));
        } catch (IllegalArgumentException e) {
            //This should never happen! But if it does, we can recover.
            clientId=new GUID(Message.makeGuid());
        }

        String[] allHosts = SettingsManager.instance().getBannedIps();
        for (int i=0; i<allHosts.length; i++)
            badHosts.add(allHosts[i]);

        Thread t=new Thread(this);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Returns the port at which the Connection Manager listens for incoming connections
     *  @return the listening port
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
     * @requires only one thread is calling this method at a time
     * @modifies this
     * @effects sets the port on which the ConnectionManager is listening.
     *  If that fails, this is <i>not</i> modified and IOException is thrown.
     *  If port==0, tells this to stop listening to incoming connections.
     *  This is properly synchronized and can be called even while run() is
     *  being called.
     */
    public void setListeningPort(int port) throws IOException {
        //1. Special case: if unchanged, do nothing.
        if (socket!=null && this.port==port)
            return;
        //2. Special case if port==0.  This ALWAYS works.
        //Note that we must close the socket BEFORE grabbing
        //the lock.  Otherwise deadlock will occur since
        //the acceptor thread is listening to the socket
        //while holding the lock.  Also note that port
        //will not have changed before we grab the lock.
        else if (port==0) {
            //Close old socket (if non-null)
            if (socket!=null) {
                try {
                    socket.close();
                } catch (IOException e) { }
            }
            synchronized (socketLock) {
                socket=null;
                this.port=0;
                socketLock.notifyAll();
            }
            return;
        }
        //3. Normal case.  See note about locking above.
        else {
            //a) Try new port.
            ServerSocket newSocket=null;
            try {
                newSocket=new ServerSocket(port);
            } catch (IOException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                throw new IOException();
            }
            //b) Close old socket (if non-null)
            if (socket!=null) {
                try {
                    socket.close();
                } catch (IOException e) { }
            }
            //c) Replace with new sock.  Notify the accept thread.
            synchronized (socketLock) {
                socket=newSocket;
                this.port=port;
                socketLock.notifyAll();
            }
            return;
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
    private class MessageBroadcaster implements Runnable {
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

                List initializedConnectionsSnapshot=initializedConnections;
                int n=initializedConnectionsSnapshot.size();
                for (int i=0; i<n; i++) {
                    Connection c2=
                        (Connection)initializedConnectionsSnapshot.get(i);
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
    public void run() {
        // Create the server socket, bind it to a port, and listen for
        // incoming connections.  If there are problems, we can continue
        // onward.
        try {
            int oldPort=port;
            setListeningPort(port,true);
            if (port!=oldPort) {
                SettingsManager.instance().setPort(port);
                callback.setPort(port);
            }
        } catch (IOException e) {
            error(ActivityCallback.ERROR_0);
        }

        while (true) {
            try {
                //Accept an incoming connection, make it into a
                //Connection object, handshake, and give it a thread
                //to service it.  If not bound to a port, wait until
                //we are.  If the port is changed while we are
                //waiting, IOException will be thrown, forcing us to
                //release the lock.
                Socket client=null;
                synchronized (socketLock) {
                    if (socket!=null) {
                        try {
                            client=socket.accept();
                        } catch (IOException e) {
                            continue;
                        }
                    } else {
                        try { socketLock.wait(); } catch (InterruptedException e) {}
                        continue;
                    }
                }
                //Check if IP address of the incoming socket is in badHosts
                if (badHosts.contains(
                        client.getInetAddress().getHostAddress())) {
                    client.close();
                    continue;
                }
                try {
                    InputStream in=client.getInputStream();
                    String word=readWord(in);

                    if (word.equals(SettingsManager.instance().
                            getConnectStringFirstWord())) {
                        if(getNumConnections() <
                                SettingsManager.instance().getMaxConn()) {
                            // Create a new connection and initialize it
                            // on another thread
                            new InitializingConnectionThread(
                                new ManagedConnection(client, this));
                        }
                        else{// we have more connections than we can handle
                            // No need to manage it -- it will complete after
                            // an appropriate wait to send pongs
                            // No need to initialize it -- it kicks off
                            // a thread to x
                            new RejectConnection(client, catcher);
                        }

                    }
                    //Incoming file transfer connection: normal HTTP and push.
                    else if (word.equals("GET")) {
                        HTTPManager mgr = new HTTPManager(client, this, false);
                    }
                    else if (word.equals("GIV")) {
                        HTTPManager mgr = new HTTPManager(client, this, true);
                    }
                    else {
                        throw new IOException();
                    }
                } catch (IOException e) {
                    //handshake failed: try to close connection.
                    try { client.close(); } catch (IOException e2) { }
                    continue;
                }
            } catch (IOException e) {
                error(ActivityCallback.ERROR_2);
                return;
            } catch (SecurityException e) {
                error(ActivityCallback.ERROR_3);
                return;
            } catch (Exception e) {
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
        callback.error(msg);
    }

    private void error(int msg, Throwable t) {
        callback.error(msg, t);
    }

    public ActivityCallback getCallback() {
        return callback;
    }

    public HostCatcher getCatcher() {
        return catcher;
    }

    public RouteTable getRouteTable() {
        return routeTable;
    }

    public RouteTable getPushRouteTable() {
        return pushRouteTable;
    }

    /**
     * Get the number of connections wanted to be maintained
     */
    public int getKeepAlive() {
        return keepAlive;
    }

    /**
     *  Reset how many connections you want and start kicking more off
     *  if required.  This IS synchronized because we don't want threads
     *  adding or removing connections while this is deciding whether
     *  to add more threads.
     */
    public synchronized void setKeepAlive(int newKeep) {
        keepAlive = newKeep;
        adjustConnectionFetchers();
    }

    /**Returns true if the given ClientID matches the ClientID of this host
     *else returns false.
     *This method will be called when a push request is being made and a
     *host which receives the messages is unable to direct the message any further.
     *The host then needs to check if it is the final destination.
     *If this method returns true, then it is the final destination.
     */
    public boolean isClient(byte[] clientId){
        //get the client ID from the gnutella.ini file.
        return this.clientId.equals(new GUID(clientId));
    }

    public byte[] getClientGUID() {
        return clientId.bytes();
    }

    /**
     * returns true if there is a connection to the given host.
     */
    public boolean isConnected(Endpoint host) {
        return endpoints.contains(host);
    }

    /**
     * Returns the number of connections
     */
    public int getNumConnections() {
        return connections.size();
    }

    /** Returns an iterator of a clone of this' initialized connections.
     *  The iterator yields items in any order.  It <i>is</i> permissible
     *  to modify this while iterating through the elements of this, but
     *  the modifications will not be visible during the iteration.
     */
    public Iterator initializedConnections() {
        List clone=new ArrayList();
        clone.addAll(initializedConnections);
        return clone.iterator();
    }

    /** Returns an iterator of a clone of all of this' connections.
     *  The iterator yields items in any order.  It <i>is</i> permissible
     *  to modify this while iterating through the elements of this, but
     *  the modifications will not be visible during the iteration.
     */
    public Iterator connections() {
        List clone=new ArrayList();
        clone.addAll(connections);
        return clone.iterator();
    }

    /**
     * Adds an initializing connection.
     * Should only be called from a thread that has this' monitor.
     * This is called from initializeExternallyGeneratedConnection
     * and initializeFetchedConnection, both times from within
     * synchronized(this) block.
     */
    private void add(Connection c) {
        //REPLACE connections with the list connections+[c]
        List newConnections=new ArrayList();
        newConnections.addAll(connections);
        newConnections.add(c);
        connections=newConnections;
    }

    /**
     * Adds an initializing connection.
     * Should only be called from a thread that has this' monitor.
     */
    private void connectionInitialized(Connection c) {
        //REPLACE initializedConnections with the list
        //initializedConnections+[c]
        List newConnections=new ArrayList();
        newConnections.addAll(initializedConnections);
        newConnections.add(c);
        initializedConnections=newConnections;

        //REPLACE endpoints with the set endpoints+{c}
        Set newEndpoints=new HashSet();
        newEndpoints.addAll(endpoints);
        newEndpoints.add(new Endpoint(c.getInetAddress().getHostAddress(),
                                      c.getPort()));
        endpoints=newEndpoints;
    }

    /**
     * @modifies this, route table
     * @effects closes c and removes it from this' connection list and
     *  all corresponding errors from the route table.  If
     *  c is not in the connection list, does nothing.  May
     *  try to establish a new outgoing connection to replace
     *  this one.
     */
    public synchronized void remove(Connection c) {
        removeInternal(c);
        adjustConnectionFetchers();
    }

    /**
     * An unsynchronized version of remove, meant to be used when the monitor
     * is already held.  This version does not kick off ConnectionFetchers;
     * only the externally exposed version of remove does that.
     */
    private void removeInternal(Connection c) {
        // Remove from the initialized connections list and clean up the
        // stuff associated with initialized connections
        int i=initializedConnections.indexOf(c);
        if (i != -1) {
            //REPLACE initializedConnections with the list
            //initializedConnections-[c]
            List newConnections=new ArrayList();
            newConnections.addAll(initializedConnections);
            newConnections.remove(c);
            initializedConnections=newConnections;

            //REPLACE endpoints with the set endpoints+{c}
            Set newEndpoints=new HashSet();
            newEndpoints.addAll(endpoints);
            newEndpoints.remove(new Endpoint(
                c.getInetAddress().getHostAddress(), c.getPort()));
            endpoints=newEndpoints;

            //Clean up route tables.
            routeTable.remove(c);
            pushRouteTable.remove(c);
        }

        // Remove from the all connections list and clean up the
        // stuff associated all connections
        i=connections.indexOf(c);
        if (i != -1) {
            //REPLACE connections with the list connections-[c]
            List newConnections=new ArrayList();
            newConnections.addAll(connections);
            newConnections.remove(c);
            connections=newConnections;

            c.shutdown();//ensure that the connection is closed.
            callback.connectionClosed(c); // Notify the listener
        }
    }

    /**
     * Starts or stops connection fetchers to maintain the invariant
     * that numConnections + numFetchers >= keepAlive
     *
     * This method is called from createConnection() [externally started outgoing
     * connection], run() [incoming connection], remove(Connection),
     * and setKeepAlive.  It should only be called from a thread that has
     * the ConnectionManager monitor. All four of the above mentioned
     * methods synchronize properly, so this precondition holds.
     */
    private void adjustConnectionFetchers() {
        int need = keepAlive - getNumConnections() - fetchers.size();

        // Start connection fetchers as necessary
        while(need > 0) {
            new ConnectionFetcher(); // This kicks off a thread and registers
                                     // the fetcher in the list
            need--;
        }

        // Stop connection fetchers as necessary, but it's possible there
        // aren't enough fetchers to stop.  In this case, close some of the
        // connections started by the ConnectionFetcher.
        int lastFetcherIndex = fetchers.size();
        while((need < 0) && (lastFetcherIndex > 0)) {
            ConnectionFetcher fetcher = (ConnectionFetcher)
                fetchers.remove(--lastFetcherIndex);
            fetcher.interrupt();
            need++;
        }
        int lastInitializingConnectionIndex =
            initializingFetchedConnections.size();
        while((need < 0) && (lastInitializingConnectionIndex > 0)) {
            Connection connection = (Connection)
                initializingFetchedConnections.remove(
                    --lastInitializingConnectionIndex);
            removeInternal(connection);
            need++;
        }
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

    /**
     * Create a new connection, blocking until it's initialized
     */
    public ManagedConnection createConnectionBlocking(
            String hostname, int portnum) throws IOException {
        ManagedConnection c = new ManagedConnection(hostname, portnum, this);

        // Initialize synchronously
        initializeExternallyGeneratedConnection(c);
        // Kick off a thread for the message loop.
        new InitializedConnectionThread(c);

        return c;
    }

    /**
     * Create a new connection, allowing it to initialize on its connection
     * thread.
     */
    public void createConnectionAsynchronously(
            String hostname, int portnum) {
        // Initialize and loop for messages on another thread.
        new InitializingConnectionThread(
                new ManagedConnection(hostname, portnum, this));
    }

    /**
     * Initializes an outgoing connection created by a ConnectionFetcher
     *
     * @throws IOException on failure.  No cleanup is necessary if this happens.
     */
    private void initializeFetchedConnection(ManagedConnection c,
                                             ConnectionFetcher fetcher)
            throws IOException {
        synchronized(this) {
            if(fetcher.isInterrupted())
                // Externally generated interrupt.
                // The interrupting thread has recorded the
                // death of the fetcher, so just return.
                return;

            add(c);
            initializingFetchedConnections.add(c);
            fetchers.remove(fetcher);
        }
        callback.connectionInitializing(c);

        try {
            c.initialize();
        } catch(IOException e) {
            synchronized(ConnectionManager.this) {
                initializingFetchedConnections.remove(c);
                removeInternal(c);
                adjustConnectionFetchers();
            }
            throw e;
        }

        synchronized(this) {
            initializingFetchedConnections.remove(c);
            adjustConnectionFetchers();
            connectionInitialized(c);
        }
        callback.connectionInitialized(c);
    }

    /**
     * Initializes an outgoing connection created by createConnection or any
     * incomingConnection.
     *
     * @throws IOException on failure.  No cleanup is necessary if this happens.
     */
    private void initializeExternallyGeneratedConnection(ManagedConnection c)
            throws IOException {
        synchronized(this) {
            add(c);
            adjustConnectionFetchers();
        }
        callback.connectionInitializing(c);

        try {
            c.initialize();
        } catch(IOException e) {
            remove(c);
            throw e;
        }

        synchronized(this) {
            connectionInitialized(c);
        }
        callback.connectionInitialized(c);
    }

    /**
     * This thread does the initialization and the message loop for
     * ManagedConnections not created by ConnectionFetchers.
     */
    private class InitializingConnectionThread
            extends Thread {
        private ManagedConnection connection;

        /**
         * The constructor calls start(), so allow you need to do
         * is construct the thread.
         */
        public InitializingConnectionThread(ManagedConnection connection) {
            this.connection = connection;
            setDaemon(true);
            start();
        }

        public void run() {
            try {
                initializeExternallyGeneratedConnection(connection);
            } catch(IOException e) {
                return;
            }
            connection.loopForMessages();
        }
    }

    /**
     * This thread does the message loop for
     * ManagedConnections not created by ConnectionFetchers.
     * The conneciton passed in should be already initialized
     */
    private class InitializedConnectionThread
            extends Thread {
        private ManagedConnection connection;

        /**
         * The constructor calls start(), so allow you need to do
         * is construct the thread.
         */
        public InitializedConnectionThread(ManagedConnection connection) {
            this.connection = connection;
            setDaemon(true);
            start();
        }

        public void run() {
            connection.loopForMessages();
        }
    }

    /**
     * Asynchronously fetches a connection from hostcatcher, then does
     * then initialization and message loop.
     *
     * The ConnectionFetcher is responsible for recording its instantiation
     * by adding itself to the fetchers list.  It is responsible  for recording
     * its death by removing itself from the fetchers list only if it
     * "interrupts itself", that is, only if it establishes a connection. If
     * the thread is interrupted externally, the interrupting thread is
     * responsible for recording the death.
     */
    private class ConnectionFetcher
            extends Thread {
        /**
         * Tries to add a connection.  Should only be called from a thread
         * that has the enclosing ConnectionManager's monitor.  This method
         * is only called from adjustConnectionFetcher's, which has the same
         * locking requirement.
         */
        public ConnectionFetcher() {
            // Record the fetcher creation
            fetchers.add(this);

            // Kick off the thread.
            setDaemon(true);
            start();
        }

        // Try a single connection
        public void run() {
            // Wait for an endpoint.
            Endpoint endpoint = null;
            synchronized(catcher) {
                while(endpoint == null) {
                    try {
                        endpoint = catcher.getAnEndpoint();
                    } catch (NoSuchElementException exc) {
                        try {
                            catcher.wait();
                        } catch (InterruptedException exc2) {
                            // Externally generated interrupt.
                            // The interrupting thread has recorded the
                            // death of the fetcher, so just return.
                            return;
                        }
                    }

                    // Only connect to currently unconnected endpoints.
                    if(isConnected(endpoint))
                        endpoint = null;  // and go around again.
                }
            }

            Assert.that(endpoint != null);

            ManagedConnection c = new ManagedConnection(
                endpoint.hostname, endpoint.port,
                ConnectionManager.this);

            try {
                initializeFetchedConnection(c, this);
            } catch(IOException e) {
                return;
            }
            c.loopForMessages();
        }
    }
}
