package org.limewire.io;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/** A default implementation of Connectable. */
public class ConnectableImpl implements Connectable {
    
    private final IpPort ipPort;
    private final boolean tlsCapable;
    
    /** Constructs a Connectable that delegates to the IpPort and may be tls capable. */
    public ConnectableImpl(IpPort ipPort, boolean tlsCapable) {
        this.ipPort = ipPort;
        this.tlsCapable = tlsCapable;
    }
    
    /** Constructs a Connectable based on the given InetSocketAddress. */
    public ConnectableImpl(InetSocketAddress addr, boolean tlsCapable) {
        this(new IpPortImpl(addr), tlsCapable);
    }
    
    /** Constructs a Connectable based on the given host data. */
    public ConnectableImpl(String host, int port, boolean tlsCapable) throws UnknownHostException {
        this(new IpPortImpl(host, port), tlsCapable);
    }
    
    /** Copy-constructor for Connectables. */
    public ConnectableImpl(Connectable connectable) {
        this(new IpPortImpl(connectable.getInetSocketAddress(), connectable.getAddress()),
             connectable.isTLSCapable());
    }

    public boolean isTLSCapable() {
        return tlsCapable; 
    }

    public String getAddress() {
        return ipPort.getAddress();
    }

    public InetAddress getInetAddress() {
        return ipPort.getInetAddress();
    }

    public int getPort() {
        return ipPort.getPort();
    }
    
    public InetSocketAddress getInetSocketAddress() {
        return ipPort.getInetSocketAddress();
    }
    
    @Override
    public String toString() {
        return ipPort + ", tlsCapable: " + tlsCapable;
    }

}
