
package com.limegroup.gnutella;

import java.util.*;
import java.io.*;


/** Immutable IP/port pair. */
public class Endpoint implements Cloneable, Serializable{
    String hostname;
    int port;
    
public Endpoint(String hostname, int port) {
	this.hostname=hostname;
	this.port=port;
}

public String toString() {
	return hostname+":"+port;
}

public String getHostname()
{
	return hostname;
}

public int getPort()
{
	return port;
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


protected Object clone()
{
	return new Endpoint(new String(hostname), port);
}
}

