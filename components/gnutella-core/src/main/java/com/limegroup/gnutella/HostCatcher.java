package com.limegroup.gnutella;

import java.util.*;
import java.io.*;

/**
 * The host catcher.  This peeks at messages coming on the network and
 * snatches IP addresses of other Gnutella peers.  IP addresses may
 * also be added to it from a file (usually "gnutella.net").  The
 * servent may then connect to these addresses as necessary to
 * maintain full connectivity.  
 */
public class HostCatcher {
    /** The untested set of connections to try.  This could ultimately
      * be prioritized by extra info (e.g., connection speed, number
      * of files) gathered in spy(..).  Succesful connections from
      * this are added to the elected set.  This is NOT synchronized
      * directly; rather you should manually grab its monitor before
      * modifying it.  */
    protected Set /* of Endpoint */ candidates=new HashSet();

    /** Where to add new connections. */
    protected ConnectionManager manager;

    /** The good connections we've made.  We keep this around to write
      * it to disk.  This is NOT synchronized directly; rather you
      * should manually grab its monitor before modifying it */
    protected Set /* of Endpoint */ elected=new HashSet();
    

    private void error(String message) {
	System.err.println(message);   //Sssshhh!
    }

    /** 
     * Creates an empty host catcher.  The manager r is where new connection
     *  should be registered in the choose method.
     */
    public HostCatcher(ConnectionManager manager) { this.manager=manager; }

    /** 
     * Creates a host catcher containing the hosts in the given file.
     * If filename does not exist, then no error message is printed
     * and this is initially empty.  The file is expected to contain a
     * sequence of lines in the format "<host>:port\n".  Lines not in
     * this format are silently ignored.  manager is as described above.  
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
		error("IOException");
		return; //fatal
	    }
	    if (line==null)   //nothing left to read?  Done.
		break;

	    //Break the line into host and port.  Skip if badly formatted.
	    int index=line.indexOf(':');
	    if (index==-1) {
		error("No ':' in line");
		continue;
	    }
	    String host=line.substring(0,index);
	    int port=0;
	    try {
		port=Integer.parseInt(line.substring(index+1));
	    } catch (NumberFormatException e) {
		error("Poorly formatted number");
		continue;
	    } catch (ArrayIndexOutOfBoundsException e) {
		continue;
	    }
	    
	    //Everything passed!  Add it.
	    Endpoint e = new Endpoint(host, port);	    
	    candidates.add(e);	    
    	    addKnownHost(e);
	}	
    }

    /**
     * @modifies the file named filename
     * @effects writes this to the given file. Those hosts who
     *  actually accepted connections are written first.
     */
    public void write(String filename) throws IOException {
	FileWriter out=new FileWriter(filename);
	synchronized(elected) {
	    writeInternal(out, elected);
	}
	synchronized(candidates) {
	    writeInternal(out, candidates);
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
    
    /** 
     * @modifies this
     * @effects ensures any hosts listed in m are in this
     */
    public void spy(Message m) {
	String ip=null;
	int port=6346;
	if (m instanceof PingReply) {
	    PingReply pr=(PingReply)m;
	    ip=pr.getIP();
	    port=pr.getPort();
	} 
	//We could also get ip addresses from query hits and push
	//requests, but then we have to guess the ports for incoming
	//connections.  (These only give ports for HTTP.)
	else {
	    return;
	}
	synchronized(candidates) {
	    Endpoint e=new Endpoint(ip, port);
	    //Only add e if its not in candidates.  This effectively ignores
	    //pings from any hosts we are directly connected to, since we added
	    //them to the elected set via addGood.
	    if (! elected.contains(e)) {
		candidates.add(e);
    	        addKnownHost(e);
	    }
	}
    }

    /** 
     * @modifies this
     * @effects notifies this that c is a good connection and should be
     *  written to disk later.
     */
    public void addGood(Connection c) {
	String host=c.getInetAddress().getHostAddress();
	int port=c.getPort();
	synchronized (elected) {
	    elected.add(new Endpoint(host,port));
	}	
    }

    /**
     * @modifies this
     * @effects returns a new outgoing connection to some host in this
     *  and removes it from this (atomically).  Does not modify manager.
     *  If no such host can be found, throws NoSuchElementException.  This
     *  method <i>is</i> thread-safe, but it can be run in parallel with
     *  itself.
     */
    public Connection choose() throws NoSuchElementException {
	while (true) {
	    Endpoint e=null;
	    //1. While we have this' monitor, get an endpoint and
	    //   remove it from this.	   
	    synchronized (candidates) {
		Iterator iter=candidates.iterator();	
		if (! iter.hasNext()) 
		    throw new NoSuchElementException();
		e=(Endpoint)iter.next();
		candidates.remove(e);	
	    }
	    //2. Now--without the lock--try to establish connection. If
	    //   successful, add the endpoint to the elected set so we 
	    //   can write to disk and try later.
	    try {
		manager.tryingToConnect(e.hostname,e.port, false);
		Connection ret=new Connection(e.hostname,e.port);
		ret.connect();
		synchronized(elected) {
		    elected.add(e);
		}
		return ret;
	    } catch (IOException exc) {
		manager.failedToConnect(e.hostname,e.port);
		continue;
	    }
	}
    }

  /**
  	 *	returns an Endpoint from the candidates list
	 *	@return An Endpoint out of the candidates list
	 *	@throws NoSuchElementException if the candidates list is empty
     */
    public Endpoint getAnEndpoint() throws NoSuchElementException 
    {
	Endpoint e=null;
	//Synchronize on candidates to avoid structural modifications to it
	//by multiple threads
	synchronized (candidates) {
		Iterator iter=candidates.iterator();	
		if (! iter.hasNext()) 
		    	throw new NoSuchElementException();
		e=(Endpoint)iter.next();
		candidates.remove(e);
    	}

	return e;

    }//end of getAnEndpoint

    /**
     *  Return the number of good hosts
     */
    public int getNumHosts() {
	return( candidates.size() );
    }

    /**
     *  Return an iterator on the candidates
     */
    public Iterator getHosts() {
	return( candidates.iterator() );
    }

    /**
     *  Add a known host/port
     */
    public void addKnownHost(Endpoint e)
    {
	if ( manager.getCallback() != null )
	    manager.getCallback().knownHost(e);
    }



    public String toString() {
	return "("+elected.toString()+", "+candidates.toString()+")";
    }

    /** Unit test.  First arg is filename */
//      public static void main(String args[]) {
//  	HostCatcher hc=new HostCatcher(new ConnectionManager(), args[0]);
//  	System.out.println(hc.toString());
//      }
}

