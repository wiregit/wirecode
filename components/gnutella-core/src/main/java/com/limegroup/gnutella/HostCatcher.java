package com.limegroup.gnutella;

import java.util.*;
import java.io.*;

/**
 * The host catcher.  This peeks at messages coming on the network and
 * snatches IP addresses of other Gnutella peers.  IP addresses may
 * also be added to it from a file (usually "gnutella.net").  The
 * servent may then connect to these addresses as necessary to
 * maintain full connectivity.<p>
 *
 * The host catcher maintains two sets of address: the "likelys" and
 * the "maybe" sets.  Likelys hosts are the results of ping replies
 * and hence are known to be good.  Maybe hosts are read from the
 * gnutella.net, so they may or may not be good.  Hosts we are
 * connected to will not be returned by choose(), but they may be
 * written to disk.
 */
public class HostCatcher {
    /* INVARIANT: "likelys" and "maybes" and the current list of 
     * connections are disjoint */
     
    /** The hosts read from disk, or otherwise unlikely to be
      * unconnected.  This could ultimately be prioritized by extra
      * info (e.g., connection speed, number of files) gathered in
      * spy(..).  This is NOT synchronized directly; rather you should
      * manually grab its monitor before modifying it. */
    protected Set /* of Endpoint */ maybes=new HashSet();

    /** Used exclusively for callbacks. */
    protected ConnectionManager manager;

    /** The hosts discovered by pings, or otherwise likely to be connected. */
    protected Set /* of Endpoint */ likelys=new HashSet();

    private void error(String message) {
	if (manager!=null) {
	    ActivityCallback callback=manager.getCallback();
	    if (callback!=null)
		callback.error(message);
	}
    }

    /** 
     * Creates an empty host catcher.  manager is used for callbacks
     * and checking my list of current connections, or null if we don't care.
     */
    public HostCatcher(ConnectionManager manager) { this.manager=manager; }

    /** 
     * Creates a host catcher whose maybe set contains the hosts
     * in the given file.  (The likelys set is empty.)  If filename
     * does not exist, then no error message is printed and this is
     * initially empty.  The file is expected to contain a sequence of
     * lines in the format "<host>:port\n".  Lines not in this format
     * are silently ignored.  manager is as described above.  
     */
    public HostCatcher(ConnectionManager manager, String filename) {
	this.manager=manager;
	
	BufferedReader in=null;
	try {
	    in=new BufferedReader(new FileReader(filename));
	} catch (FileNotFoundException e) {
	    error("HostCatcher couldn't find gnutella.net file: ignoring");
	    return;
	}
	while (true) {
	    String line=null;
	    try {
		line=in.readLine();
	    } catch (IOException e) {
		return; //fatal
	    }
	    if (line==null)   //nothing left to read?  Done.
		break;

	    //Break the line into host and port.  Skip if badly formatted.
	    int index=line.indexOf(':');
	    if (index==-1) {
		continue;
	    }
	    String host=line.substring(0,index);
	    int port=0;
	    try {
		port=Integer.parseInt(line.substring(index+1));
	    } catch (NumberFormatException e) {
		continue;
	    } catch (ArrayIndexOutOfBoundsException e) {
		continue;
	    }
	    
	    //Everything passed!  Add it.  No need to synchronize because
	    //this is a constructor.
	    Endpoint e = new Endpoint(host, port);	    
	    maybes.add(e);	    
    	    addKnownHost(e);
	}	
    }

    /**
     * @modifies the file named filename
     * @effects writes this to the given file.  The file
     *  is prioritized by rough probability of being good.
     */
    public synchronized void write(String filename) throws IOException {
	FileWriter out=new FileWriter(filename);
	//Write connections we're connected to...
	if (manager!=null) {
	    Set tmp=new HashSet();
	    for (Iterator iter=manager.connections(); iter.hasNext(); ) {
		Connection c=(Connection)iter.next();
		Endpoint e=new Endpoint(c.getInetAddress().getHostAddress(),
					c.getPort());
		tmp.add(e);
	    }
	    writeInternal(out, tmp);
	}
	
	//Followed by pings...
	synchronized(likelys) {
	    writeInternal(out, likelys);
	}
	//Followed by gnutella.net entries...
	synchronized(maybes) {
	    writeInternal(out, maybes);
	}
	out.close();	
    }

    private void writeInternal(Writer out, Set set) throws IOException {
	Iterator enum=set.iterator();
	while (enum.hasNext()) {
	    Endpoint e=(Endpoint)enum.next();
	    out.write(e.toString()+"\n");
	}
    }

    //////////////////////////////////////////////////////////////////////

    
    /** 
     * @modifies this
     * @effects may choose to add hosts listed in m to this' likelys set.
     *  c is the connection this message came in on; if m came directly
     *  from the foreign host of c, m is ignored.
     */
    public void spy(Message m) {	
	//We could also get ip addresses from query hits and push
	//requests, but then we have to guess the ports for incoming
	//connections.  (These only give ports for HTTP.) 
	if (! (m instanceof PingReply))
	    return;
	    	
	PingReply pr=(PingReply)m;
	Endpoint e=new Endpoint(pr.getIP(), pr.getPort(), 
				pr.getFiles(), pr.getKbytes());

	//Ignore m if I'm connected to it.  This implies that hops==0,
	//but converse does not hold; cached pongs from Gnucache
	//servers have hops==0.   TODO3: if this is slow, change implementation
	//of ConnectionManager.isConnected.
	//if (manager!=null && manager.isConnected(pr.getIP(), pr.getPort()))
	//    return;

	//Add it to the likely set, remove it from the maybes set if
	//it's there.
	synchronized(likelys) {
	    likelys.add(e);
	}
	synchronized(maybes) {
	    maybes.remove(e); //ok even if e not in maybe
	}
	addKnownHost(e);
    }


    /**
     * @modifies this
     * @effects returns a new outgoing, connected connection to some
     *  host in this and removes it from this (atomically).  If manager is non-null, 
     *  it is (almost) guaranteed that we do not return any hosts we are connected to.  
     *  Does not modify manager.  If no such host can be found, throws
     *  NoSuchElementException.  This method <i>is</i> thread-safe, but
     *  it is cleverly synchronized so that multiple invocations can run
     *  at the same time.
     */
    public Connection choose() throws NoSuchElementException {
	while (true) {
	    // Return if the manager has enough
	    if ( manager != null && !manager.doYouWishToContinue() )
		return null;
	    Endpoint e=getAnEndpoint();	
	    // Skip anything we are connected to.
	    if (manager != null && manager.isConnected(e.getHostname(),e.getPort()))
		continue;
	    Connection ret = null;
	    try {
		ret = new Connection(e.hostname,e.port);
		if (manager!=null)
		    manager.tryingToConnect(ret, false);
		ret.connect();
		return ret;
	    } catch (IOException exc) {
		if (manager!=null)
		    manager.failedToConnect(ret);
		continue;
	    }
	}
    }

  /**
   * @modifies this
   * @effects atomically returns the highest priority host from this
   *  and removes it from this.  Throws NoSuchElementException if 
   *  no host could be found.
   */
    public Endpoint getAnEndpoint() throws NoSuchElementException 
    {
	//Use pings before gnutella.net if possible.
	if (likelys.size()>0) {
	    try {
		Endpoint e=choose(likelys);
	    } catch (NoSuchElementException e) { 
		//This can happen if another thread removed the last likelys
		//host before I got to acquire the monitor.  If it happens,
		//try maybes set.
	    }
	}
	return choose(maybes);    
    }//end of getAnEndpoint

    /**
     * @modifies s
     * @effects atomically returns an element of s and removes it.
     *  Throws NoSuchElementException if s is empty.
     */
    private static Endpoint choose(Set s) throws NoSuchElementException {
	//  While we have s' monitor, get an endpoint and
	//  remove it from this.	   
	synchronized (s) {
	    Iterator iter=s.iterator();	
	    if (! iter.hasNext()) 
		throw new NoSuchElementException();
	    Endpoint e=(Endpoint)iter.next();
	    s.remove(e);	
	    return e;
	}
    }	 

    /**
     *  Return the number of good hosts
     */
    public int getNumHosts() {
	return( maybes.size() );
    }

    /**
     * Return an iterator of the hosts in this. This can be modified 
     * while iterating through the result, but the modifications will
     * not be observed.
     */
    public Iterator getHosts() {
	Set everything=new HashSet();
	synchronized(likelys) {
	    everything.addAll(likelys);
	}
	synchronized(maybes) {
	    everything.addAll(maybes);
	}	
	return( everything.iterator() );
    }

    /** 
     * @requires n>0
     * @effects returns an iterator that yields up the best n endpoints of this
     */ 
    public Iterator getBestHosts(int n) {
	Set best=new HashSet();
	//ignore maybes
	synchronized(likelys) {
	    Iterator iter=likelys.iterator();
	    for (int i=0; i<n && iter.hasNext(); i++) 
		best.add(iter.next());
	    return best.iterator();
	}
    }

    /**
     *  Notifies callback that e has been discovered.
     */
    private void addKnownHost(Endpoint e)
    {
	if ( manager!=null && manager.getCallback() != null )
	    manager.getCallback().knownHost(e);
    }

    /**
     *  Remove unwanted or used entries
     */
    public void removeHost(String host, int port) {
	synchronized(likelys) {
	    removeHost(host, port, likelys);
	}
	//Followed by gnutella.net entries...
	synchronized(maybes) {
	    removeHost(host, port, maybes);
	}
    }

    private void removeHost(String host, int port, Set set) {
	Iterator enum=set.iterator();
	while (enum.hasNext()) {
	    Endpoint e=(Endpoint)enum.next();
	    if ( e.hostname.equals(host) && e.port == port )
	    {
		set.remove(e);
		return;
	    }
	}
    }

    public String toString() {
	return "("+likelys.toString()+", "+maybes.toString()+")";
    }

//      /** Unit test.  First arg is filename */
//      public static void main(String args[]) {
//  	HostCatcher hc=new HostCatcher(null, args[0]);
//  	System.out.println(hc.toString());

//  	PingRequest ping=new PingRequest((byte)3);
//  	PingReply pr1=new PingReply(ping.getGUID(), (byte)5, 0,
//  				    new byte[] {(byte)127, (byte)0, (byte)0, (byte)1},
//  				    0, 0);
//  	hc.spy(pr1); //ignored because the hop is 0
//  	PingReply pr2=new PingReply(ping.getGUID(), (byte)5, 0,
//  				   new byte[] {(byte)127, (byte)0, (byte)0, (byte)2},
//  				   10, 20);
//  	pr2.hop();
//  	hc.spy(pr2); //not ignored
//  	Iterator iter=hc.getBestHosts(10);
//  	Assert.that(iter.hasNext());
//  	Endpoint e=(Endpoint)iter.next();
//  	String host=e.getHostname();
//  	Assert.that(host.equals("127.0.0.2"));
//  	Assert.that(e.getFiles()==10);
//  	Assert.that(e.getKbytes()==20);
//  	Assert.that(! iter.hasNext());
//      }
}

