package org.limewire.net;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.limewire.io.NetworkUtils;
import org.limewire.nio.NBSocket;
import org.limewire.nio.observer.ConnectObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/** Factory for creating Sockets. */
@Singleton
public class SocketsManagerImpl implements SocketsManager {
    
    private final SocketController socketController;
    
    public SocketsManagerImpl() {
        this(new SimpleSocketController(new ProxyManagerImpl(new EmptyProxySettings()), new EmptySocketBindingSettings()));
    }
    
    @Inject
    public SocketsManagerImpl(SocketController socketController) {
        this.socketController = socketController;
    }

    public Socket create(ConnectType type) throws IOException {
        return type.getFactory().createSocket();
    }

    public Socket connect(NBSocket socket, String localAddress, int localPort, InetSocketAddress addr, int timeout, ConnectType type) throws IOException {
        return connect(socket, localAddress, localPort, addr, timeout, null, type);    
    }

    public Socket connect(InetSocketAddress addr, int timeout) throws IOException {
        return connect(addr, timeout, ConnectType.PLAIN);
    }

    public Socket connect(InetSocketAddress addr, int timeout, ConnectType type) throws IOException {
        return connect(addr, timeout, null, type);
    }

    public Socket connect(InetSocketAddress addr, int timeout, ConnectObserver observer) throws IOException {
        return connect(addr, timeout, observer, ConnectType.PLAIN);
    }
    
    public Socket connect(InetSocketAddress addr, int timeout, ConnectObserver observer, ConnectType type) throws IOException {
        return connect(null, null, -1, addr, timeout, observer, type);    
    }

    public Socket connect(NBSocket socket, String localAddress, int localPort, InetSocketAddress addr, int timeout, ConnectObserver observer, ConnectType type) throws IOException {
        if(!NetworkUtils.isValidPort(addr.getPort()))  
            throw new IllegalArgumentException("port out of range: "+addr.getPort());
        if(addr.isUnresolved())
            throw new IOException("address must be resolved!");
        
        return socketController.connect(socket, localAddress, localPort, type.getFactory(), addr, timeout, observer);
	}    

    public boolean removeConnectObserver(ConnectObserver observer) {
        return socketController.removeConnectObserver(observer);
    }	

	public int getNumAllowedSockets() {
        return socketController.getNumAllowedSockets();
	}

    public int getNumWaitingSockets() {
        return socketController.getNumWaitingSockets();
    }
}
