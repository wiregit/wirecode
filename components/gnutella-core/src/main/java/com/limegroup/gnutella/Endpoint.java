package com.limegroup.gnutella;

import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.io.*;


/** Immutable IP/port pair.  Also contains an optional number and size of files. */
public class Endpoint implements Cloneable, Serializable, Comparable{
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
     * Sets the connectivity of the node
     * @param connectivity the connectivity to be set 
     */
    public void setConnectivity(int connectivity){
	this.connectivity = connectivity;
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
	
	if(connectivity > other.connectivity)	
	    return 1;
	if(connectivity < other.connectivity)
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

    public int getPort(){
	return port;
    }
    
    /** Returns the number of files the host has, or -1 if I don't know */
    public long getFiles() {
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
        try
        {
            if(kbytes/files < 35000)  //ie avg file size less than 35MB
            {
                return kbytes;
            }
            else if(kbytes/files < 150000 && files < 8) //ie avg file size less
            {                               //than 150MB, and num-files < 8
                                            //might be some video files
                                            //but with more number of files
                                            //maintaining such a ratio may not 
                                            //be possible
                return kbytes;
            }
            else if(kbytes/files < 150000000) //ie avg file size less than 150GB
                                              //but more than 150MB
            {
                                             //user might have sent number of 
                                             //bytes instead of number of kbytes
                return kbytes/1000;
                
            }
        }
        catch(ArithmeticException ae)
        {
            return 0;   //as files=0 throws ArithmeticException, that should
                        //make the kbytes also zero
        }
        
        
        return -1;
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

