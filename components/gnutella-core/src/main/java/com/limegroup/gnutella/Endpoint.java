package com.limegroup.gnutella;

import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.limegroup.gnutella.util.StringUtils;

/**
 * Immutable IP/port pair.  Also contains an optional number and size
 * of files, mainly for legacy reasons.
 */
public class Endpoint implements Cloneable, Serializable,
com.sun.java.util.collections.Comparable
{
    /**
    * serial version (needed for serialization/deserialization)
    */
    static final long serialVersionUID = 4686711693494625070L;
    
    private String hostname = null;
    int port = 0;
    /** Number of files at the host, or -1 if unknown */
    private long files=-1;
    /** Size of all files on the host, or -1 if unknown */
    private long kbytes=-1;
    
    /**
     * IP Address of form '144.145.146.147' will be stored as:
     * ip[0] = 144
     * ip[1] = 145
     * ip[2] = 146
     * ip[3] = 147
     * Note: Lazy initialization may be done for this field.
     * Therefore, apart from
     * constructors, it should be accessed using only getter/setter methods
     */
    private byte[] hostBytes = null;

    /**
     * Needed for Network Discovery. Records information regarding
     * wthether the neighbours of this node has been identified or not
     */
    public transient boolean processed = false;

    /**
     * The number of other nodes that this node is connected to
     */
    private int connectivity = 0;

    /**
     * The weight is used in ranking the endpoints
     */
    private transient int weight = 0;
    
    /**
     * Sets the connectivity of the node
     * @param connectivity the connectivity to be set
     */
    public void setConnectivity(int connectivity)
    {
        this.connectivity = connectivity;
    }

    /**
     * Sets the weight of the node
     * @param weight the weight to be set
     */
    public void setWeight(int weight)
    {
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
     * The comparison function (It uses the connectivity as the
     * measure for comparison ie if a.connectivity > b.connectivity
     * then a.compareTo(b) > 0
     * @param o the other object to be compared to
     */
    public int compareTo(Object o)
    {
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
     * Returns a new Endpoint from a Gnutella-style host/port pair:
     * <ul>
     * <li>If hostAndPort is of the format "host:port", where port
     *   is a number, returns new Endpoint(host, port).
     * <li>If hostAndPort contains no ":" or a ":" at the end of the string,
     *   returns new Endpoint(hostAndPort, 6346).
     * <li>Otherwise throws IllegalArgumentException.
     * </ul>
     */
    public Endpoint(String hostAndPort) throws IllegalArgumentException 
    {
        this(hostAndPort, false);
    }

    /**
     * Same as new Endpoint(hostAndPort) but with additional restrictions on
     * hostAndPart; if requireNumeric==true and the host part of hostAndPort is
     * not as a numeric dotted-quad IP address, throws IllegalArgumentException.
     * Examples:
     * <pre>
     * new Endpoint("www.limewire.org:6346", false) ==> ok
     * new Endpoint("not a url:6346", false) ==> ok
     * new Endpoint("www.limewire.org:6346", true) ==> IllegalArgumentException
     * new Endpoint("64.61.25.172:6346", true) ==> ok
     * new Endpoint("64.61.25.172", true) ==> ok
     * new Endpoint("127.0.0.1:ABC", false) ==> IllegalArgumentException     
     * </pre> 
     * No DNS lookups are ever involved, so this constructor won't block.
     *
     * @see Endpoint (String))
     */
    public Endpoint(String hostAndPort, boolean requireNumeric) 
        throws IllegalArgumentException
    {
        final int DEFAULT=6346;
        int j=hostAndPort.indexOf(":");
        if (j<0)
        {
            this.hostname = hostAndPort;
            this.port=DEFAULT;
        } else if (j==0)
        {
            throw new IllegalArgumentException();
        } else if (j==(hostAndPort.length()-1))
        {
            this.hostname = hostAndPort.substring(0,j);
            this.port=DEFAULT;
        } else
        {
            this.hostname = hostAndPort.substring(0,j);
            try
            {
                this.port=Integer.parseInt(hostAndPort.substring(j+1));
            } catch (NumberFormatException e)
            {
                throw new IllegalArgumentException();
            }
        }

        if (requireNumeric) 
        {
            //TODO3: implement with fewer allocations
            String[] numbers=StringUtils.split(hostname, '.');
            if (numbers.length!=4)
                throw new IllegalArgumentException();
            for (int i=0; i<numbers.length; i++) 
            {
                try {
                    int x=Integer.parseInt(numbers[i]);
                    if (x<0 || x>255)
                        throw new IllegalArgumentException();
                } catch (NumberFormatException fail) {
                    throw new IllegalArgumentException();
                }
            }
        }
    }

    public Endpoint(String hostname, int port)
    {
        this.hostname = hostname;
        this.port=port;
    }

    /**
    * Creates a new Endpoint instance
    * @param hostBytes IP address of the host (MSB first)
    * @param port The port number for the host
    */
    public Endpoint(byte[] hostBytes, int port)
    {
        this.hostBytes = hostBytes;
        this.port = port;
        
        //initialize hostname also
        this.hostname = Message.ip2string(hostBytes);
    }
    
    
    /**
     * @param files the number of files the host has
     * @param kbytes the size of all of the files, in kilobytes
     */
    public Endpoint(String hostname, int port, long files, long kbytes)
    {
        this(hostname, port);
        this.files=files;
        this.kbytes=kbytes;
    }
    
    /**
    * Creates a new Endpoint instance
    * @param hostBytes IP address of the host (MSB first)
    * @param port The port number for the host
    * @param files the number of files the host has
    * @param kbytes the size of all of the files, in kilobytes
    */
    public Endpoint(byte[] hostBytes, int port, long files, long kbytes)
    {
        this(hostBytes, port);
        this.files=files;
        this.kbytes=kbytes;
    }
    
    
    /**
    * Constructs a new endpoint from pre-existing endpoint by copying the
    * fields
    * @param ep The endpoint from whom to initialize the member fields of
    * this new endpoint
    */
    public Endpoint(Endpoint ep)
    {
        //copy the fields
        this.connectivity = ep.connectivity;
        this.files = ep.files;
        this.hostname = ep.hostname;
        this.hostBytes = ep.hostBytes;
        this.kbytes = ep.kbytes;
        this.port = ep.port;
        this.processed = ep.processed;
        this.weight = ep.weight;
    }

    public String toString()
    {
        return hostname+":"+port + " connectivity=" + connectivity + " files="
        + files + " kbytes=" + kbytes;
    }

    public String getHostname()
    {
        return hostname;
    }

    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }

    public int getPort()
    {
        return port;
    }

    /** Returns the number of files the host has, or -1 if I don't know */
    public long getFiles()
    {
        return files;
    }

    /** Sets the number of files the host has */
    public void setFiles(long files)
    {
        this.files = files;
    }

    /** Returns the size of all files the host has, in kilobytes,
     *  or -1 if I don't know, it also makes sure that the kbytes/files
     *  ratio is not ridiculous, in which case it normalizes the values
     */
    public long getKbytes()
    {
        return kbytes;
    }

    /**
     * If the number of files or the kbytes exceed certain limit, it
     * considers them as false data, and initializes the number of
     * files as well as kbytes to zero in that case
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
            else if(files > 5000)  //> 5000 files
            {
                files = kbytes = 0;
                return;
            }
            else if (kbytes/files > 250000) //> 250MB/file
            {
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
    public void setKbytes(long kbytes)
    {
        this.kbytes = kbytes;
    }

    /**
     * Endpoints are equal if their hostnames and ports are.  The number
     * and size of files does not matter.
     */
    public boolean equals(Object o)
    {
        if (! (o instanceof Endpoint))
        return false;
        Endpoint e=(Endpoint)o;
        return hostname.equals(e.hostname) && port==e.port;
    }

    public int hashCode()
    {
        //This is good enough, since one host rarely has multiple ports.
        return hostname.hashCode();
    }


    protected Object clone()
    {
        return new Endpoint(new String(hostname), port, files, kbytes);
    }

    /**
     *This method  returns the IP of the end point as an array of bytes
     */
    public byte[] getHostBytes() throws UnknownHostException
    {
        if(hostBytes == null)
        {
            hostBytes = InetAddress.getByName(hostname).getAddress();
            //the above fn call might throw UnknownHostException, but thats what
            //we want in case of DNS failure
        }
        return hostBytes;
    }

    /**
     * @requires this has a dotted-quad address or a name that can be
     *  resolved.
     * @effects Returns true iff this is a private IP address as defined by
     *  RFC 1918.  In the case that this has a symbolic name that
     *  cannot be resolved, returns true;
     */
    public boolean isPrivateAddress()
    {
        byte[] bytes;
        try {
            bytes=getHostBytes();
        } catch (UnknownHostException e) {
            return false;
        }
        if (bytes[0]==(byte)10)
            return true;  //10.0.0.0 - 10.255.255.255
        else if (bytes[0]==(byte)172 &&
                 bytes[1]>=(byte)16 &&
                 bytes[1]<=(byte)31)
            return true;  //172.16.0.0 - 172.31.255.255
        else if (bytes[0]==(byte)192 &&
                 bytes[1]==(byte)168)
            return true; //192.168.0.0 - 192.168.255.255
        else if (bytes[0]==(byte)0 &&
                 bytes[1]==(byte)0 &&
                 bytes[2]==(byte)0 &&
                 bytes[3]==(byte)0)
            return true; //0.0.0.0 - Gnutella (well BearShare really) convention
        else
            return false;
    }

    /**
     * @requires this and other have dotted-quad addresses, or
     *  names that can be resolved.
     * @effects Returns true if this is on the same subnet as 'other',
     *  i.e., if this and other are in the same IP class and have the
     *  same network number.
     */
    public boolean isSameSubnet(Endpoint other)
    {
        byte[] a;
        byte[] b;
        int first;
        try {
            a=getHostBytes();
            first=ByteOrder.ubyte2int(a[0]);
            b=other.getHostBytes();
        } catch (UnknownHostException e) {
            return false;
        }

        //See http://www.3com.com/nsc/501302.html
        //class A
        if (first<=127)
            return a[0]==b[0];
        //class B
        else if (first <= 191)
            return a[0]==b[0] && a[1]==b[1];
        //class C
        else
            return a[0]==b[0] && a[1]==b[1] && a[2]==b[2];
    }
}

