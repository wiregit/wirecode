package org.limewire.net;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.limewire.io.NetworkUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.nio.NBSocket;
import org.limewire.nio.NBSocketFactory;
import org.limewire.nio.observer.ConnectObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/** Factory for creating Sockets. */
@Singleton
public class SocketsManagerImpl implements SocketsManager {
    
    private final SocketController socketController;
    
    public SocketsManagerImpl() {
        this(new SimpleSocketController(new ProxyManagerImpl(new EmptyProxySettings(), new SimpleNetworkInstanceUtils()), new EmptySocketBindingSettings()));
    }
    
    @Inject
    public SocketsManagerImpl(SocketController socketController) {
        this.socketController = socketController;
    }

    public Socket create(ConnectType type) throws IOException {
        return type.getFactory().createSocket();
    }

    public Socket connect(NBSocket socket, InetSocketAddress localAddr, InetSocketAddress addr, int timeout, ConnectType type) throws IOException {
        return connect(socket, localAddr, addr, timeout, null, type);    
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
        return connect(null, null, addr, timeout, observer, type);    
    }

    public Socket connect(final NBSocket socket, InetSocketAddress localAddr, InetSocketAddress addr, int timeout, ConnectObserver observer, ConnectType type) throws IOException {
        if(!NetworkUtils.isValidPort(addr.getPort()))  
            throw new IllegalArgumentException("port out of range: "+addr.getPort());
        if(addr.isUnresolved())
            throw new IOException("address must be resolved!");
        
        if(socket == null) {
            return socketController.connect(type.getFactory(), addr, null, timeout, observer);
	    } else {
	        NBSocketFactory factory = new NBSocketFactory() {
                @Override
                public NBSocket createSocket() throws IOException {
                    return socket;
                }

                @Override
                public NBSocket createSocket(String host, int port) throws IOException,
                        UnknownHostException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public NBSocket createSocket(InetAddress host, int port) throws IOException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public NBSocket createSocket(String host, int port, InetAddress localHost,
                        int localPort) throws IOException, UnknownHostException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public NBSocket createSocket(InetAddress address, int port,
                        InetAddress localAddress, int localPort) throws IOException {
                    throw new UnsupportedOperationException();
                }
	            
	        };
            return socketController.connect(factory, addr, localAddr, timeout, observer);
        }
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
