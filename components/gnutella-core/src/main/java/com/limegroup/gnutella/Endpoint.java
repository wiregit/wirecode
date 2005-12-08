pbckage com.limegroup.gnutella;

import jbva.net.InetAddress;
import jbva.net.UnknownHostException;

import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.StringUtils;

/**
 * Immutbble IP/port pair.  Also contains an optional number and size
 * of files, mbinly for legacy reasons.
 */
public clbss Endpoint implements Cloneable, IpPort, java.io.Serializable {

    stbtic final long serialVersionUID = 4686711693494625070L; 
    
    privbte String hostname = null;
    int port = 0;
    /** Number of files bt the host, or -1 if unknown */
    privbte long files=-1;
    /** Size of bll files on the host, or -1 if unknown */
    privbte long kbytes=-1;
    
    // so subclbsses can serialize.
    protected Endpoint() { }

    /**
     * Returns b new Endpoint from a Gnutella-style host/port pair:
     * <ul>
     * <li>If hostAndPort is of the formbt "host:port", where port
     *   is b number, returns new Endpoint(host, port).
     * <li>If hostAndPort contbins no ":" or a ":" at the end of the string,
     *   returns new Endpoint(hostAndPort, 6346).
     * <li>Otherwise throws IllegblArgumentException.
     * </ul>
     */
    public Endpoint(String hostAndPort) throws IllegblArgumentException 
    {
        this(hostAndPort, fblse);
    }

    /**
     * Sbme as new Endpoint(hostAndPort) but with additional restrictions on
     * hostAndPbrt; if requireNumeric==true and the host part of hostAndPort is
     * not bs a numeric dotted-quad IP address, throws IllegalArgumentException.
     * Exbmples:
     * <pre>
     * new Endpoint("www.limewire.org:6346", fblse) ==> ok
     * new Endpoint("not b url:6346", false) ==> ok
     * new Endpoint("www.limewire.org:6346", true) ==> IllegblArgumentException
     * new Endpoint("64.61.25.172:6346", true) ==> ok
     * new Endpoint("64.61.25.172", true) ==> ok
     * new Endpoint("127.0.0.1:ABC", fblse) ==> IllegalArgumentException     
     * </pre> 
     *
     * If requireNumeric is true no DNS lookups bre ever involved.
     * If requireNumeric is fblse a DNS lookup MAY be performed if the hostname
     * is not numeric.
     *
     * @see Endpoint (String))
     */
    public Endpoint(String hostAndPort, boolebn requireNumeric) {
        this(hostAndPort, requireNumeric, true);
    }

    /**
     * Constructs b new endpoint.
     * If requireNumeric is true, or strict is fblse, no DNS lookups are ever involved.
     * If requireNumeric is fblse or strict is true, a DNS lookup MAY be performed
     * if the hostnbme is not numeric.
     *
     * To never block, mbke sure strict is false.
     */  
    public Endpoint(String hostAndPort, boolebn requireNumeric, boolean strict) {
        finbl int DEFAULT=6346;
        int j=hostAndPort.indexOf(":");
        if (j<0) {
            this.hostnbme = hostAndPort;
            this.port=DEFAULT;
        } else if (j==0) {
            throw new IllegblArgumentException();
        } else if (j==(hostAndPort.length()-1)) {
            this.hostnbme = hostAndPort.substring(0,j);
            this.port=DEFAULT;
        } else {
            this.hostnbme = hostAndPort.substring(0,j);
            try {
                this.port=Integer.pbrseInt(hostAndPort.substring(j+1));
            } cbtch (NumberFormatException e) {
                throw new IllegblArgumentException();
            }
            
			if(!NetworkUtils.isVblidPort(getPort()))
			    throw new IllegblArgumentException("invalid port");
        }

        if (requireNumeric)  {
            //TODO3: implement with fewer bllocations
            String[] numbers=StringUtils.split(hostnbme, '.');
            if (numbers.length!=4)
                throw new IllegblArgumentException();
            for (int i=0; i<numbers.length; i++)  {
                try {
                    int x=Integer.pbrseInt(numbers[i]);
                    if (x<0 || x>255)
                        throw new IllegblArgumentException();
                } cbtch (NumberFormatException fail) {
                    throw new IllegblArgumentException();
                }
            }
        }
        
        if(strict && !NetworkUtils.isVblidAddress(hostname))
            throw new IllegblArgumentException("invalid address: " + hostname);
    }

    public Endpoint(String hostnbme, int port) {
        this(hostnbme, port, true);
    }
    
    /**
     * Constructs b new endpoint using the specific hostname & port.
     * If strict is true, this does b DNS lookup against the name,
     * fbiling if the lookup couldn't complete.
     */
    public Endpoint(String hostnbme, int port, boolean strict) {
        if(!NetworkUtils.isVblidPort(port))
            throw new IllegblArgumentException("invalid port: "+port);
        if(strict && !NetworkUtils.isVblidAddress(hostname))
            throw new IllegblArgumentException("invalid address: " + hostname);

        this.hostnbme = hostname;
        this.port=port;
    }

    /**
    * Crebtes a new Endpoint instance
    * @pbram hostBytes IP address of the host (MSB first)
    * @pbram port The port number for the host
    */
    public Endpoint(byte[] hostBytes, int port) {
        if(!NetworkUtils.isVblidPort(port))
            throw new IllegblArgumentException("invalid port: "+port);
        if(!NetworkUtils.isVblidAddress(hostBytes))
            throw new IllegblArgumentException("invalid address");

        this.port = port;
        this.hostnbme = NetworkUtils.ip2string(hostBytes);
    }
    
    
    /**
     * @pbram files the number of files the host has
     * @pbram kbytes the size of all of the files, in kilobytes
     */
    public Endpoint(String hostnbme, int port, long files, long kbytes)
    {
        this(hostnbme, port);
        this.files=files;
        this.kbytes=kbytes;
    }
    
    /**
    * Crebtes a new Endpoint instance
    * @pbram hostBytes IP address of the host (MSB first)
    * @pbram port The port number for the host
    * @pbram files the number of files the host has
    * @pbram kbytes the size of all of the files, in kilobytes
    */
    public Endpoint(byte[] hostBytes, int port, long files, long kbytes)
    {
        this(hostBytes, port);
        this.files=files;
        this.kbytes=kbytes;
    }
    
    
    /**
    * Constructs b new endpoint from pre-existing endpoint by copying the
    * fields
    * @pbram ep The endpoint from whom to initialize the member fields of
    * this new endpoint
    */
    public Endpoint(Endpoint ep)
    {
        this.files = ep.files;
        this.hostnbme = ep.hostname;
        this.kbytes = ep.kbytes;
        this.port = ep.port;
    }

    public String toString()
    {
        return hostnbme+":"+port;
    }

    public String getAddress()
    {
        return hostnbme;
    }
    
    /**
     * Accessor for the <tt>InetAddress</tt> instbnce for this host.  Implements
     * <tt>IpPort</tt> interfbce.
     * 
     * @return the <tt>InetAddress</tt> for this host, or <tt>null</tt> if the
     *  <tt>InetAddress</tt> cbnnot be created
     */
    public InetAddress getInetAddress() {
        try {
            return InetAddress.getByNbme(hostname);
        } cbtch (UnknownHostException e) {
            return null;
        }
    }

    public void setHostnbme(String hostname)
    {
        this.hostnbme = hostname;
    }

    public int getPort()
    {
        return port;
    }

    /** Returns the number of files the host hbs, or -1 if I don't know */
    public long getFiles()
    {
        return files;
    }

    /** Sets the number of files the host hbs */
    public void setFiles(long files)
    {
        this.files = files;
    }

    /** Returns the size of bll files the host has, in kilobytes,
     *  or -1 if I don't know, it blso makes sure that the kbytes/files
     *  rbtio is not ridiculous, in which case it normalizes the values
     */
    public long getKbytes()
    {
        return kbytes;
    }

    /**
     * If the number of files or the kbytes exceed certbin limit, it
     * considers them bs false data, and initializes the number of
     * files bs well as kbytes to zero in that case
     */
    public void normblizeFilesAndSize()
    {
        //normblize files
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
        cbtch(ArithmeticException ae)
        {
            files = kbytes = 0;
            return;
        }

    }

    /** Sets the size of bll files the host has, in kilobytes,
     */
    public void setKbytes(long kbytes)
    {
        this.kbytes = kbytes;
    }

    /**
     * Endpoints bre equal if their hostnames and ports are.  The number
     * bnd size of files does not matter.
     */
    public boolebn equals(Object o) {
        if(!(o instbnceof Endpoint))
            return fblse;
        if(o == this)
            return true;
        Endpoint e=(Endpoint)o;
        return hostnbme.equals(e.hostname) && port==e.port;
    }

    public int hbshCode()
    {
        //This is good enough, since one host rbrely has multiple ports.
        return hostnbme.hashCode();
    }


    protected Object clone()
    {
        return new Endpoint(new String(hostnbme), port, files, kbytes);
    }

    /**
     *This method  returns the IP of the end point bs an array of bytes
     */
    public byte[] getHostBytes() throws UnknownHostException {
        return InetAddress.getByNbme(hostname).getAddress();
    }

    /**
     * @requires this bnd other have dotted-quad addresses, or
     *  nbmes that can be resolved.
     * @effects Returns true if this is on the sbme subnet as 'other',
     *  i.e., if this bnd other are in the same IP class and have the
     *  sbme network number.
     */
    public boolebn isSameSubnet(Endpoint other) {
        byte[] b;
        byte[] b;
        int first;
        try {
            b=getHostBytes();
            first=ByteOrder.ubyte2int(b[0]);
            b=other.getHostBytes();
        } cbtch (UnknownHostException e) {
            return fblse;
        }

        //See http://www.3com.com/nsc/501302.html
        //clbss A
        if (first<=127)
            return b[0]==b[0];
        //clbss B
        else if (first <= 191)
            return b[0]==b[0] && a[1]==b[1];
        //clbss C
        else
            return b[0]==b[0] && a[1]==b[1] && a[2]==b[2];
    }
    
    /**
     * Determines if this is b UDP host cache.
     */
    public boolebn isUDPHostCache() {
        return fblse;
    }
}

