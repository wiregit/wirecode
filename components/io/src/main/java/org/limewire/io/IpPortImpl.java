package org.limewire.io;



import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/** A default implementation of IpPort. */
public class IpPortImpl implements IpPort {
    
    private final InetSocketAddress addr;
    private final String addrString;
    
    /** Constructs a new IpPort based on the given SocketAddress. */
    public IpPortImpl(InetSocketAddress addr) {
        this(addr, addr.getAddress().getHostAddress());
    }
    
    /** Constructs a new IpPort with the given SocketAddress, explicitly defining the string-addr. */
    public IpPortImpl(InetSocketAddress addr, String addrString) {
        this.addr = addr;
        this.addrString = addrString;
    }
    
    /** Constructs a new IpPort using the addr & port. */
    public IpPortImpl(InetAddress addr, int port) {
        this(new InetSocketAddress(addr, port));
    }
    
    /** Constructs a new IpPort using the given host & port.*/
    public IpPortImpl(String host, int port) throws UnknownHostException {
        this(new InetSocketAddress(InetAddress.getByName(host), port), host);
    }
    
    /** Constructs an IpPort using the given host:port */
    public IpPortImpl(String hostport) throws UnknownHostException {
        int colonIdx = hostport.indexOf(":");
        if(colonIdx == hostport.length() -1)
            throw new UnknownHostException("invalid hostport: " + hostport);
        
        String host = hostport;
        int port = 80;
        if(colonIdx != -1) {
            host = hostport.substring(0, colonIdx);
            try {
                port = Integer.parseInt(hostport.substring(colonIdx+1).trim());
            } catch(NumberFormatException nfe) {
                throw new UnknownHostException("invalid hostport: " + hostport);
            }
        }
        
        this.addr = new InetSocketAddress(InetAddress.getByName(host), port);
        this.addrString = host;
    }
    
    public InetSocketAddress getInetSocketAddress() {
        return addr;
    }
    
    public InetAddress getInetAddress() {
        return addr.getAddress();
    }
    
    public String getAddress() {
        return addrString;
    }
    
    public int getPort() {
        return addr.getPort();
    }
    
    public String toString() {
        return "host: " + getAddress() + ", port: " + getPort();
    }
}