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
	    candidates.add(new Endpoint(host, port));	    
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
     * @effects adds any hosts listed in m to this
     */
    public void spy(Message m) {
	String ip=null;
	int port=6346;
	if (m instanceof PingReply) {
	    ip=((PingReply)m).getIP();
	    port=port;
	} 
	//We could also get ip addresses from query hits and push
	//requests, but then we have to guess the ports for incoming
	//connections.  (These only give ports for HTTP.)
	else {
	    return;
	}
	synchronized(candidates) {
	    candidates.add(new Endpoint(ip, port));
	}
    }

    /** 
     * @modifies this
     * @effects notifies this that c is a good connection and should be
     *  written to disk later.
     */
    public void addGood(Connection c) {
	String host=c.sock.getInetAddress().getHostAddress();
	int port=c.sock.getPort();
	synchronized (elected) {
	    elected.add(new Endpoint(host,port));
	}	
    }

    /**
     * @modifies manager
     * @effects returns a new outgoing connection to some host in this,
     *  adds the connection to manager, and removes it from this (atomically).
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
		System.out.print("Establishing outgoing connection on "
				  +e.toString()+"...");
	    }
	    //2. Now--without the lock--try to establish connection. If
	    //   successful, add the endpoint to the elected set so we 
	    //   can write to disk and try later.
	    try {
		Connection ret=new Connection(manager,e.hostname,e.port);
		synchronized(elected) {
		    elected.add(e);
		}
		System.out.println("OK");
		return ret;
	    } catch (IOException exc) {
		System.out.println("FAILED");
		continue;
	    }
	}
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

/** Immutable IP/port pair. */
class Endpoint {
    String hostname;
    int port;
    
    Endpoint(String hostname, int port) {
	this.hostname=hostname;
	this.port=port;
    }

    public String toString() {
	return hostname+":"+port;
    }
    
    public boolean equals(Object o) {
	if (! (o instanceof Endpoint))
	    return false;
	Endpoint e=(Endpoint)o;
	return hostname.equals(e.hostname) && port==e.port;
    }

    public int hashCode() {
	//This is good enough, since one host rarely has multiple ports.
	return hostname.hashCode();
    }
}

