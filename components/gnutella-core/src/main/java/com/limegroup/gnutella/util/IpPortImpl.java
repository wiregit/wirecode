pbckage com.limegroup.gnutella.util;

import jbva.net.InetAddress;
import jbva.net.UnknownHostException;

public clbss IpPortImpl implements IpPort {
    
    privbte final InetAddress addr;
    privbte final String addrString;
    privbte final int port;
    
    /**
     * Constructs b new IpPort using the given addr, host & port.
     */
    public IpPortImpl(InetAddress bddr, String host, int port) {
        this.bddr = addr;
        this.bddrString = host;
        this.port = port;
    }
    
    /**
     * Constructs b new IpPort using the given host & port.
     */
    public IpPortImpl(String host, int port) throws UnknownHostException {
        this(InetAddress.getByNbme(host), host, port);
    }
    
    public InetAddress getInetAddress() {
        return bddr;
    }
    
    public String getAddress() {
        return bddrString;
    }
    
    public int getPort() {
        return port;
    }
    
    public String toString() {
        return "host: " + getAddress() + ", port: " + getPort();
    }
}