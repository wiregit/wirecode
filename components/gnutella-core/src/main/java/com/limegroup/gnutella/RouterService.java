package com.limegroup.gnutella;

import java.io.*;
import java.net.InetAddress;
import com.sun.java.util.collections.*;

/**
 * The External interface into the router world.
 */
public class RouterService
{        
    private ConnectionManager manager;
    private ResponseVerifier verifier = new ResponseVerifier();
    
    /**
     * Create a connection manager using the default port
     */
    public RouterService( )
    {
        manager = new ConnectionManager();
        initManager();
    }

    /**
     * Create a connection manager using the specified port
     */
    public RouterService( int port )
    {
        manager = new ConnectionManager( port );
        initManager();
    }

    /**
     * Initialize the connection manager 
     */
    private void initManager()
    {
        manager.setKeepAlive(SettingsManager.instance().getKeepAlive());
        Thread t=new Thread(manager);
        t.setDaemon(true);
        t.start();        
        // FileManager.getFileManager().addDirectory("C:/rjs/src");
        //FileManager.getFileManager().addDirectory("E:/My Music");
        //new LimeProperties("Neutella.props",true);

        manager.propertyManager();
        //Now if quick connecting, try hosts.
        if (SettingsManager.instance().getUseQuickConnect()) {
            Thread t2=new Thread() {
                public void run() {
                    quickConnect();
                }
            };
            t2.setDaemon(true);
            t2.start();
        }        
    }

    /**
     * Dump the routing table
     */
    public void dumpRouteTable()
    {
        System.out.println(manager.routeTable.toString());
    }

    /**
     * Dump the puch routing table
     */
    public void dumpPushRouteTable()
    {
        System.out.println(manager.pushRouteTable.toString());
    }



    /**
     * Dump the list of connections
     */
    public void dumpConnections()
    {
            Iterator iter=manager.connections();
            while (iter.hasNext())
                System.out.println(iter.next().toString());
    }

        private static final byte[] LOCALHOST={(byte)127, (byte)0, (byte)0, (byte)1};
    /**
     * Connect to remote host (establish outgoing connection).
     * Blocks until connection established.
         * If establishing c would connect us to the listening socket,
         * the connection is not established.
     */
    public void connectToHost( Connection c ) throws IOException
    {                                        
        try {
                //Don't allow connections to yourself.  We have to special
                //case connections to "localhost" or "127.0.0.1" since
                //they are aliases for what is returned by manager.getListeningPort.
            byte[] cIP=InetAddress.getByName(c.getOrigHost()).getAddress();
                if (Arrays.equals(cIP, LOCALHOST)) {
                        if (c.getOrigPort()==manager.getListeningPort())
                                throw new IOException();
                } else {
                        byte[] managerIP=manager.getAddress();
                        if (Arrays.equals(cIP, managerIP)
                                && c.getOrigPort()==manager.getListeningPort())
                                throw new IOException();
                }

                
            manager.tryingToConnect(c,false);
            c.connect();
            c.setManager(manager);
            manager.add(c); //calls ActivityCallback.updateConnection
            PingRequest pr=new PingRequest(SettingsManager.instance().getTTL());
            manager.fromMe(pr);
            c.send(pr);
            Thread tc=new Thread(c);
            tc.setDaemon(true);
            tc.start();
        } catch (IOException e) {
            manager.failedToConnect(c);
            throw e;
        }
    }

    /**
     * Connects to hosts using the quick connect list. 
     * Blocks until connected.
     */
    public void quickConnect() {
        SettingsManager settings=SettingsManager.instance();
        //Ensure the keep alive is at least 1.
        if (settings.getKeepAlive()<1)
            settings.setKeepAlive(SettingsInterface.DEFAULT_KEEP_ALIVE);
        adjustKeepAlive(settings.getKeepAlive());
        //Clear host catcher.  Note that if we already have outgoing
        //connections the host catcher will fill up after clearing it.
        //This means we won't really be trying those hosts.
        clearHostCatcher();

        //Try the quick connect hosts one by one.
        String[] hosts=SettingsManager.instance().getQuickConnectHosts();       
        for (int i=0; i<hosts.length; i++) {
            //Extract hostname+port
            Endpoint e;
            try {
                e=new Endpoint(hosts[i]);
            } catch (IllegalArgumentException exc) {
                continue;
            }

            //Connect...or try to.
            Connection c=new Connection(e.getHostname(), e.getPort());
            try {
                connectToHost(c);
            } catch (IOException exc) {
                continue;
            }
            
            //Wait some time.  If we still need more, try others.
            synchronized(this) {
                try {
                    wait(4000);
                } catch (InterruptedException exc) { }                
            }
            if (manager.catcher.getNumHosts()>=settings.getKeepAlive()) {
                break;
            }
        }
    }

    /** 
     * @modifies this
     * @effects removes all connections.
     */
    public void disconnect() {
        SettingsManager settings=SettingsManager.instance();        
        int oldKeepAlive=settings.getKeepAlive();

        //1. Prevent any new threads from starting.       
        adjustKeepAlive(0);
        //2. Remove all connections.
        for (Iterator iter=manager.connections(); iter.hasNext(); ) {
            Connection c=(Connection)iter.next();
            removeConnection(c);
        }        
    }

    /**
     * Remove a connection based on the host/port
     */
    public void removeConnection( Connection c )
    {
        c.shutdown();
        manager.remove(c);
    }

    /**
     * Clear the hostcatcher if requested
     */
    public void clearHostCatcher()
    {
        manager.catcher.clear();
    }


    /**
     * Tell the system to send connection activity to this callback interface
     */
    public void setActivityCallback( ActivityCallback callback )
    {
        manager.setActivityCallback( callback );
    }

    /**
     * Shut stuff down and write the gnutella.net file
     */
    public void shutdown()
    {
        manager.shutdown(); //write gnutella.net
    }

    /**
     *  Reset how many connections you want and start kicking more off
     *  if required
     */
    public void adjustKeepAlive(int newKeep)
    {
        manager.adjustKeepAlive(newKeep);
    }

    /**
     * Notify the backend that spam filters settings have changed, and that
     * extra work must be done.
     */
    public void adjustSpamFilters() {
        //Just replace all the spam filters.  No need to do anything
        //fancy like incrementally updating them.
        for (Iterator iter=manager.connections(); iter.hasNext(); ) {
            Connection c=(Connection)iter.next();
            c.setPersonalFilter(SpamFilter.newPersonalFilter());
            c.setRouteFilter(SpamFilter.newRouteFilter());
        }
    }

    /**
     * @modifies this
     * @effects sets the port on which to listen for incoming connections. 
     *  If that fails, this is <i>not</i> modified and IOException is thrown.
     *  If port==0, tells this to stop listening to incoming connections.
     */
    public void setListeningPort(int port) throws IOException {
        manager.setListeningPort(port);
    }

    /**
     *  Return the total number of messages sent and received
     */
    public int getTotalMessages() {
        return( manager.getTotalMessages() );
    }

    /**
     *  Return the total number of dropped messages 
     */
    public int getTotalDroppedMessages() {
        return( manager.totDropped );
    }

    /**
     *  Return the total number of misrouted messages
     */
    public int getTotalRouteErrors() {
        return( manager.totRouteError );
    }

    /**
     *  Return the number of good hosts in my horizon.
     */
    public long getNumHosts() {
        long ret=0;
        for (Iterator iter=manager.connections(); iter.hasNext() ; ) 
            ret+=((Connection)iter.next()).getNumHosts();
        return ret;
    }

    /**
     * Return the number of files in my horizon. 
     */
    public long getNumFiles() {
        long ret=0;
        for (Iterator iter=manager.connections(); iter.hasNext() ; ) 
            ret+=((Connection)iter.next()).getNumFiles();
        return ret;
    }

    /**
     * Return the size of all files in my horizon, in kilobytes.
     */
    public long getTotalFileSize() {
        long ret=0;
        for (Iterator iter=manager.connections(); iter.hasNext() ; ) 
            ret+=((Connection)iter.next()).getTotalFileSize();
        return ret;
    }

    /**
     * Updates the horizon statistics.
     *
     * @modifies manager, network
     * @effects resets manager's horizon statistics and sends
     *  out a ping request.  Ping replies come back asynchronously
     *  and modify the horizon statistics.  Poll for them with
     *  getNumHosts, getNumFiles, and getTotalFileSize.
     */
    public void updateHorizon() {
        //Reset statistics first
        for (Iterator iter=manager.connections(); iter.hasNext() ; ) 
            ((Connection)iter.next()).clearHorizonStats();
    
        //Send ping to everyone.  Call to fromMe() notes that replies
        //are to me.
        PingRequest pr=new PingRequest(SettingsManager.instance().getTTL());
        manager.fromMe(pr);
        manager.sendToAll(pr);
    }

    /**
     * Searches Gnutellanet files of the given type with the given
     * query string and minimum speed.  If type is null, any file type
     * is acceptable.  Returns the GUID of the query request sent as a
     * 16 byte array, or null if there was a network error.
     * ActivityCallback is notified asynchronously of responses.
     * These responses can be matched with requests by looking at
     * their GUIDs.  (You may want to wrap the bytes with a GUID
     * object for simplicity.)  */
    public byte[] query(String query, int minSpeed, MediaType type) {
        QueryRequest qr=new QueryRequest(SettingsManager.instance().getTTL(), minSpeed, query);
        manager.fromMe(qr);
        verifier.record(qr, type);
        manager.sendToAll(qr);
        return qr.getGUID();
    }

    /** Same as query(query, minSpeed, null), i.e., searches for files of any type. */
    public byte[] query(String query, int minSpeed) {
        return query(query, minSpeed, null);
    }

    /** Same as ResponseVerifier.score. */
    public int score(byte[] Guid, Response resp){
        return verifier.score(Guid,resp);
    }

    /** Same as ResponseVerifier.matchesType. */
    public boolean matchesType(byte[] guid, Response response) {
        return verifier.matchesType(guid, response);
    }

    /**
     * Searches the given host for all its files.  Results are given
     * to the GUI via handleQuery.  Returns null if the host couldn't
     * be reached.  Blocks until the connection is established and
     * the query is sent.
     */
    public byte[] browse(String host, int port) {
        Connection c=null;

        //1. See if we're connected....  
        for (Iterator iter=manager.connections(); iter.hasNext(); ) {
            Connection c2=(Connection)iter.next();
            //Get the IP address of c2 in dotted decimal form.  Note
            //that c2.getOrigHost is no good as it may return a
            //symbolic name.
            String ip=c2.getInetAddress().getHostAddress();
            if (ip.equals(host) && c2.getOrigPort()==port) {
                c=c2;
                break;
            }
        }
        //...if not, establish a new one.
        if (c==null) {
            try {
                c=new Connection(host, port);
                connectToHost(c);
            } catch (IOException e) {
                return null;
            }
        }
            
        //2. Send a query for "*.*" with a TTL of 1.
        QueryRequest qr=new QueryRequest((byte)1, 0, "*.*");
        manager.fromMe(qr);
        try {
            c.send(qr);
        } catch (IOException e) {
            return null;
        }

        //3. Remove a lesser connection if necessary.  Current heuristic:
        //drop the connection other than c with least number of files.
        //
        //TODO: this should go in ConnectionManager, but that requires
        //us to add MAX_KEEP_ALIVE property.  Besides being the logical 
        //place for this functionality, it would make the network
        //hill climbing a snap to implement.  It would also allow us to 
        //synchronize properly to prevent race conditions.
        if (manager.getNumConnections()>manager.getKeepAlive()) {
            Connection worst=null;
            long files=Long.MAX_VALUE;
            for (Iterator iter=manager.connections(); iter.hasNext(); ) {
                Connection c2=(Connection)iter.next();
                //Don't remove the connection to the host we are browsing.
                if (c2==c)
                    continue;
                long n=c2.getNumFiles();
                if (n<files) {
                    worst=c2;
                    files=n;
                }                
            }
            if (worst!=null) {
                manager.remove(worst);                            
            }
        }
                
        return qr.getGUID();
    }

    /**
     *  Return an iterator on the hostcatcher hosts
     */
    public Iterator getHosts() {
        return( manager.catcher.getHosts() );
    }

    /**
     *  Return the number of good hosts
     */
    public int getNumConnections() {
        return( manager.getNumConnections() );
    }

    /**
     *  Return the number searches made locally ( QReq )
     */
    public int getNumLocalSearches() {
        return( manager.QReqCount );
    }

    /**
     *  Remove unwanted or used entries from host catcher 
     */
    public void removeHost(String host, int port) {
        manager.catcher.removeHost(host, port);
    }

    /**
     * Returns an instance of a SettingsInterface
     */
    public SettingsInterface getSettings() {
        return SettingsManager.instance();
    }

    /** Returns an instance of the library class */
    public Library getLibrary() {
        return Library.instance();
    }

    /**
     * Initialize a download request
     */
    public HTTPDownloader initDownload(String ip, int port, int index, 
      String fname, byte[] bguid, int size) {

        HTTPDownloader down = new
            HTTPDownloader("http", ip, port, index, fname, manager, bguid, 
              size);

        return( down );
    }

    /**
     * Kickoff a download request
     */
    public void kickoffDownload(HTTPDownloader down) {

        down.ensureDequeued();

        Thread t = new Thread(down);

        t.setDaemon(true);

        t.start();
    }

    /**
     * Create and kickoff a download request
     */
    public void tryDownload(String ip, int port, int index, String fname, 
      byte[] bguid, int size) {

        HTTPDownloader down = initDownload(ip, port, index, fname, bguid, size);

        kickoffDownload(down);
    }

    /**
     * Create a queued download request
     */
    public void queueDownload(String ip, int port, int index, String fname, 
      byte[] bguid, int size) {

        HTTPDownloader down = initDownload(ip, port, index, fname, bguid, size);

        down.setQueued();
        manager.getCallback().addDownload( down );
    }

    /**
     * Try to resume a download request
     */
    public void resumeDownload( HTTPDownloader mgr )
    {
        mgr.resume();

        kickoffDownload(mgr);
    }


    /**
     * Return how many files are being shared
     */
    public int getNumSharedFiles( )
    {
        return( FileManager.getFileManager().getNumFiles() );
    }
        
}









