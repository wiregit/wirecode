package com.limegroup.gnutella;

import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.io.*;


/** Immutable IP/port pair.  Also contains an optional number and size of files. */
public class Endpoint implements Cloneable, Serializable, 
				 com.sun.java.util.collections.Comparable{
    String hostname;
    int port;
    /** Number of files at the host, or -1 if unknown */
    private long files=-1;
    /** Size of all files on the host, or -1 if unknown */
    private long kbytes=-1;
    
    /**
     * Needed for Network Discovery. Records information regarding wthether the neighbours
     * of this node has been identified or not
     */
    public boolean processed = false;
    
    /**
     * The number of other nodes that this node is connected to
     */
    private int connectivity = 0;	

	/**
	* The weight is used in ranking the endpoints 
	*/
	private int weight = 0;
    
    /**
     * Sets the connectivity of the node
     * @param connectivity the connectivity to be set 
     */
    public void setConnectivity(int connectivity){
		this.connectivity = connectivity;
    }

	 /**
     * Sets the weight of the node
     * @param weight the weight to be set 
     */
    public void setWeight(int weight){
		this.weight = weight;
    }

	/**
	* Gets the weight of this endpoint
	* @return The weight of the endpoint
	*/
	public int getWeight()
	{
		return weight;
	}
	
    /**
     * returns the connectivity of the node
     * @return The connectivity of the node 
     */
    public int getConnectivity()
    {
        return connectivity;
    }
    
    /**
     * The comparison function (It uses the connectivity as the measure for comparison
     * ie if a.connectivity > b.connectivity then a.compareTo(b) > 0
     * @param o the other object to be compared to
     */
    public int compareTo(Object o){
	//Check for the class of the passed object
	if(o == null)
	    throw new ClassCastException();
	
	Endpoint other = (Endpoint) o;	
	
	if(weight > other.weight)	
	    return 1;
	if(weight < other.weight)
	    return -1;
	return 0;		
    }

    /** 
     * Extracts a hostname and port from a string:
     * <ul>
     * <li>If hostAndPort is of the format "host:port", returns new
     *   Endpoint(host, port).
     * <li>If hostAndPort contains no ":" or a ":" at the end of the string,
     *   returns new Endpoint(hostAndPort, 6346).
     * <li>Otherwise throws IllegalArgumentException.
     * </ul>
     */
    public Endpoint(String hostAndPort) throws IllegalArgumentException {
	final int DEFAULT=6346;
	int j=hostAndPort.indexOf(":");
	if (j<0) {
	    this.hostname=hostAndPort;
	    this.port=DEFAULT;
	} else if (j==0) {
	    throw new IllegalArgumentException();
	} else if (j==(hostAndPort.length()-1)) {
	    this.hostname=hostAndPort.substring(0,j);
	    this.port=DEFAULT;
	} else {
	    this.hostname=hostAndPort.substring(0,j);
	    try {
		this.port=Integer.parseInt(hostAndPort.substring(j+1));
	    } catch (NumberFormatException e) {
		throw new IllegalArgumentException();
	    }
	}	
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
	return hostname+":"+port + " connectivity=" + connectivity + " files="
					+ files + " kbytes=" + kbytes;
    }
    
    public String getHostname(){
	return hostname;
    }
    
    public void setHostname(String hostname){
	this.hostname = hostname;
    }

    public int getPort(){
	return port;
    }
    
    /** Returns the number of files the host has, or -1 if I don't know */
    public long getFiles(){
        return files;
    }
    
 /** Sets the number of files the host has */
    public void setFiles(long files) {
        this.files = files;
    }
    
    
    
    /** Returns the size of all files the host has, in kilobytes, 
    *  or -1 if I don't know, it also makes sure that the kbytes/files 
    *  ratio is not ridiculous, in which case it normalizes the values
    */   
    public long getKbytes() {
        return kbytes;
    }

    /**
    * If the number of files or the kbytes exceed certain limit, it
    * considers them as false data, and initializes the number of files as well
    * as kbytes to zero in that case
    */
    public void normalizeFilesAndSize()
    {
        //normalize files
        try
        {
            if(kbytes > 20000000) // > 20GB
            {
                files = kbytes = 0;
                return;
            }
            if(files > 10000)  //>10000 files
            {
                files = kbytes = 0;
                return;
            }
            
            if(kbytes/files < 35000)  //ie avg file size less than 35MB
            {
                files = kbytes = 0;
                return;
            }
            else if(kbytes/files < 150000 && files < 10) //ie avg file size less
            {                               //than 150MB, and num-files < 10
                                            //might be some video files
                                            //but with more number of files
                                            //maintaining such a ratio may not 
                                            //be possible
                files = kbytes = 0;
                return;
            }
             
        }
        catch(ArithmeticException ae)
        {
            files = kbytes = 0;
            return;   
        }
        
    }
    
    /** Sets the size of all files the host has, in kilobytes, 
    */   
    public void setKbytes(long kbytes) {
        this.kbytes = kbytes;
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
    
    
    protected Object clone(){
	return new Endpoint(new String(hostname), port, files, kbytes);
    }
    
    /**
     *This method  returns the IP of the end point as an array of bytes
     * @requires the hostname is is dotted decimal format
     */
    public byte[] getHostBytes(){
	StringTokenizer tokenizer = new StringTokenizer(hostname,".");
	String a = tokenizer.nextToken();
	String b = tokenizer.nextToken();
	String c = tokenizer.nextToken();
	String d = tokenizer.nextToken();
	
	int a1 = Integer.parseInt(a);
	int b1 = Integer.parseInt(b);
	int c1 = Integer.parseInt(c);
	int d1 = Integer.parseInt(d);
	byte[] retBytes = {(byte)a1, (byte)b1,(byte)c1,(byte)d1};
	return retBytes;
    }
    
    
//       // Unit tester
//       public static void main(String args[]){
//  //         Endpoint e = new Endpoint(args[0], 8001);
//  //       	byte[] b = e.getHostBytes();
//  //       	byte[] b1 = {(byte)255,(byte)255,(byte)255,(byte)255}; // fence post
//  //       	byte[] b2 = {(byte)127,(byte)0,(byte)0,(byte)1}; // normal case
//  //       	System.out.println("Sumeet: testing 255 case " + Arrays.equals(b,b1) );
//  //       	System.out.println("Sumeet: testing normal case " + Arrays.equals(b,b2) );
//  	Endpoint e;
//  	try {
//  	    e=new Endpoint(":6347");
//  	    Assert.that(false);
//  	} catch (IllegalArgumentException exc) {
//  	    Assert.that(true);
//  	}
//  	try {
//  	    e=new Endpoint("abc:cas");
//  	    Assert.that(false);
//  	} catch (IllegalArgumentException exc) {
//  	    Assert.that(true);
//  	}
//  	try {
//  	    e=new Endpoint("abc");
//  	    Assert.that(e.getHostname().equals("abc"));
//  	    Assert.that(e.getPort()==6346);
//  	} catch (IllegalArgumentException exc) {
//  	    Assert.that(false);
//  	}
//  	try {
//  	    e=new Endpoint("abc:");
//  	    Assert.that(e.getHostname().equals("abc"));
//  	    Assert.that(e.getPort()==6346);
//  	} catch (IllegalArgumentException exc) {
//  	    Assert.that(false);	    
//  	}
//  	try {
//  	    e=new Endpoint("abc:7");
//  	    Assert.that(e.getHostname().equals("abc"));
//  	    Assert.that(e.getPort()==7);
//  	} catch (IllegalArgumentException exc) {
//  	    Assert.that(false);	    
//  	}	
//      } 
}

