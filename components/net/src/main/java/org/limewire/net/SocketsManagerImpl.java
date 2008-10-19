package org.limewire.net;


import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.io.Address;
import org.limewire.io.NetworkUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.address.AddressConnector;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.net.address.AddressResolver;
import org.limewire.nio.NBSocket;
import org.limewire.nio.NBSocketFactory;
import org.limewire.nio.observer.ConnectObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/** Factory for creating Sockets. */
@Singleton
public class SocketsManagerImpl implements SocketsManager, EventBroadcaster<ConnectivityChangeEvent>, ListenerSupport<ConnectivityChangeEvent> {
    
    private final SocketController socketController;
    
    private final List<AddressResolver> addressResolvers = new CopyOnWriteArrayList<AddressResolver>();
    
    private final List<AddressConnector> addressConnectors = new CopyOnWriteArrayList<AddressConnector>();
    
    private final EventMulticaster<ConnectivityChangeEvent> connectivityEventMulticaster = new EventMulticasterImpl<ConnectivityChangeEvent>();
    
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

    private AddressResolver getResolver(Address address) {
        for (AddressResolver resolver : addressResolvers) {
            if (resolver.canResolve(address)) {
                return resolver;
            }
        }
        return null;
    }
    
    private AddressConnector getConnector(Address address) {
        for (AddressConnector connector : addressConnectors) {
            if (connector.canConnect(address)) {
                return connector;
            }
        }
        return null;
    }
    
    @Override
    public boolean canConnect(Address address) {
        return getConnector(address) != null;
    }

    @Override
    public boolean canResolve(Address address) {
        return getResolver(address) != null;
    }
    
    @Override
    public void connect(Address address, final int timeout, final ConnectObserver observer) {
        // feel free to rework this logic with more use cases that don't fit the model
        // for example we're only doing one cycle of address resolution, might have to 
        // be done iteratively if addresses are resolved to address that need more resolution
        if (address == null) { 
            throw new NullPointerException("address must not be null");
        }
        AddressResolutionObserver proxy = new AddressResolutionObserver() {
            @Override
            public void resolved(Address... addresses) {
                MultiAddressConnector connector = new MultiAddressConnector(addresses, timeout, observer);
                connector.connectNext();
            }
            @Override
            public void handleIOException(IOException iox) {
                observer.handleIOException(iox);
            }
            @Override
            public void shutdown() {
                observer.shutdown();
            }
        };
        resolve(address, timeout, proxy);
    }
    
    private void connectUnresolved(Address address, int timeout, ConnectObserver observer) {
        AddressConnector connector = getConnector(address);
        if (connector != null) {
            connector.connect(address, timeout, observer);
        } else {
            observer.handleIOException(new ConnectException("no connector ready to connect to: " + address));
            observer.shutdown();
        }
    }

    @Override
    public void resolve(Address address, int timeout, AddressResolutionObserver observer) {
        // feel free to rework this logic with more use cases that don't fit the model
        if (address == null) { 
            throw new NullPointerException("address must not be null");
        }
        // this can also be changed to allow multiple resolvers to resolve the same
        // address and re-resolve the resolved addresses too
        AddressResolver resolver = getResolver(address);
        if (resolver != null) {
            resolver.resolve(address, timeout, observer);
        } else {
            observer.resolved(address);
        }
    }
    
    private class MultiAddressConnector implements ConnectObserver {

        private final ConnectObserver delegate;
        private final Address[] addresses;
        private int index = 0;
        private final int timeout;

        public MultiAddressConnector(Address[] addresses, int timeout, ConnectObserver delegate) {
            this.addresses = addresses;
            this.timeout = timeout;
            this.delegate = delegate;
        }
        
        public void connectNext() {
            connectUnresolved(addresses[index++], timeout, this);
        }
        
        @Override
        public void handleConnect(Socket socket) throws IOException {
            delegate.handleConnect(socket);
        }

        @Override
        public void handleIOException(IOException iox) {
            if (index < addresses.length) {
                connectNext();
            } else {
                delegate.handleIOException(iox);
            }
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }
        
    }

    @Override
    public void registerConnector(AddressConnector connector) {
        addressConnectors.add(connector);
    }

    @Override
    public void registerResolver(AddressResolver resolver) {
        addressResolvers.add(resolver);
    }

    @Override
    public void addListener(EventListener<ConnectivityChangeEvent> listener) {
        connectivityEventMulticaster.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<ConnectivityChangeEvent> listener) {
        return connectivityEventMulticaster.removeListener(listener);
    }

    @Override
    public void broadcast(ConnectivityChangeEvent event) {
        connectivityEventMulticaster.broadcast(event);
    }
}
