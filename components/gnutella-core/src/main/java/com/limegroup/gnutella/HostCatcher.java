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
    //Currently implemented with a set. 
    //TODO3: prioritize the elements of the set based on extra info
    //(e.g., connection speed, number of files) gathered in spy(..)
    protected Set /* of Endpoint */ set=new HashSet();
    protected Router router;

    private void error(String message) {
	System.err.println(message);
    }

    /** 
     * Creates an empty host catcher.  The router r is where new connection
     *  should be registered in the choose method.
     */
    public HostCatcher(Router r) { this.router=r; }

    /** 
     * Creates a host catcher containing the hosts in the given file.
     * If filename does not exist, then no error message is printed
     * and this is initially empty.  The file is expected to contain a
     * sequence of lines in the format "<host>:port\n".  Lines not in
     * this format are silently ignored.  r is as described above.  
     */
    public HostCatcher(Router r, String filename) {
	this.router=r;
	
	BufferedReader in=null;
	try {
	    in=new BufferedReader(new FileReader(filename));
	} catch (FileNotFoundException e) {
	    error("File not found.");
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
		return;

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
	    set.add(new Endpoint(host, port));	    
	}
    }

    /** 
     * @modifies this
     * @effects adds any hosts listed in m to this
     */
    public void spy(Message m) {
	Assert.that(false,"TODO1: Unimplemented!"); 	
    }

    /**
     * @modifies router
     * @effects returns a new outgoing connection to some host in this, and 
     *  adds the connection to router.  If no such host can be found, throws
     *  NoSuchElementException.
     */
    public Connection choose() throws NoSuchElementException {
	Iterator iter=set.iterator();
	while (iter.hasNext()) {
	    Endpoint e=(Endpoint)iter.next();
	    try {
		Connection ret=new Connection(router,e.hostname,e.port);
		return ret;
	    } catch (IOException exc) {
		//Assume if it's no good now, it's no good later.
		set.remove(e);
		continue;
	    }
	}
	throw new NoSuchElementException();
    }
    
    /**
     * @modifies this
     * @effects conn is not in this.  May even "blacklist" the 
     *  host in the future.
     */
    public void remove(Connection conn) {
	//Kinda tricky to implement, because socket's name 
	//may be numerical or symbolic
	Assert.that(false,"TODO1: Unimplemented!"); 
    }

    String toString() {
	return set.toString();
    }

    /** Unit test.  First arg is filename */
    public static void main(String args[]) {
	HostCatcher hc=new HostCatcher(new Router(), args[0]);
	System.out.println(hc.choose());
    }
}

class Endpoint {
    String hostname;
    int port;
    
    Endpoint(String hostname, int port) {
	this.hostname=hostname;
	this.port=port;
    }

    String toString() {
	return hostname+":"+port;
    }
}
