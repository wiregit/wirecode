
package com.limegroup.gnutella;

import java.util.*;
import java.io.*;


/** Immutable IP/port pair.  Also contains an optional number and size of files. */
public class Endpoint implements Cloneable, Serializable, Comparable{
    String hostname;
    int port;
    /** Number of files at the host, or -1 if unknown */
    private long files=-1l;
    /** Size of all files on the host, or -1 if unknown */
    private long kbytes=-1l;

/**
* Needed for Network Discovery. Records information regarding wthether the neighbours
* of this node has been identified or not
*/
public boolean processed = false;
    
/**
* The number of other nodes that this node is connected to
*/
public int connectivity = 0;	


/**
* The comparison function (It uses the connectivity as the measure for comparison
* ie if a.connectivity > b.connectivity then a.compareTo(b) > 0
* @param o the other object to be compared to
*/
public int compareTo(Object o)
{
	//Check for the class of the passed object
	if(o == null)
		throw new ClassCastException();

	Endpoint other = (Endpoint) o;	

	if(connectivity > other.connectivity)	
		return 1;
	if(connectivity < other.connectivity)
		return -1;
	return 0;		
}

   
public Endpoint(String hostname, int port) {
	this.hostname=hostname;
	this.port=port;
}

/**
 * @param files the number of files the host has
 * @param kbytes the size of all of the files, in kilobytes
 */
public Endpoint(String hostname, int port, long files, long kbytes) {
        this(hostname, port);
        this.files=files;
	this.kbytes=kbytes;
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

/** Returns the number of files the host has, or -1 if I don't know */
public long getFiles() {
        return files;
}
 
/** Returns the size of all files the host has, in kilobytes, 
 *  or -1 if I don't know */   
public long getKbytes() {
        return kbytes;
}

/** 
 * Endpoints are equal if their hostnames and ports are.  The number
 * and size of files does not matter.
 */
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

