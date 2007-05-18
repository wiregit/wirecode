package org.limewire.io;



import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
/**
 * Provides a default {@link IpPort} implementation to return IP information
 * ({@link InetAddress}, host name, and port number). <code>IpPortImpl</code>
 * uses constructor arguments (either directly or via parsing) to set IP 
 * information.
 */
public class IpPortImpl implements IpPort {
    
    private final InetAddress addr;
    private final String addrString;
    private final int port;
    
    /** Constructs a new IpPort based on the given SocketAddress. */
    public IpPortImpl(InetSocketAddress addr) {
        this(addr.getAddress(), addr.getHostName(), addr.getPort());
    }
    
    /**
     * Constructs a new IpPort using the given addr & port.
     */
    public IpPortImpl(InetAddress addr, int port) {
        this.addr = addr;
        this.addrString = addr.getHostName();
        this.port = port;
    }
    
    /**
     * Constructs a new IpPort using the given addr, host & port.
     */
    public IpPortImpl(InetAddress addr, String host, int port) {
        this.addr = addr;
        this.addrString = host;
        this.port = port;
    }
    
    /**
     * Constructs a new IpPort using the given host & port.
     */
    public IpPortImpl(String host, int port) throws UnknownHostException {
        this(InetAddress.getByName(host), host, port);
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
        
        this.addr = InetAddress.getByName(host);
        this.addrString = host;
        this.port = port;
    }
    
    public InetAddress getInetAddress() {
        return addr;
    }
    
    public String getAddress() {
        return addrString;
    }
    
    public int getPort() {
        return port;
    }
    
    public String toString() {
        return "host: " + getAddress() + ", port: " + getPort();
    }
}