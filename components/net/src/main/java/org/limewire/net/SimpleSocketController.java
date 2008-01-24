package org.limewire.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.limewire.net.ProxySettings.ProxyType;
import org.limewire.nio.NBSocket;
import org.limewire.nio.NBSocketFactory;
import org.limewire.nio.observer.ConnectObserver;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A SocketController that blindly connects to the given address
 * (deferring the connection through proxies, if necessary).
 */
@Singleton
class SimpleSocketController implements SocketController {    

    /** The possibly null address to bind to. */
    private InetSocketAddress lastBindAddr;
    
    private final ProxyManager proxyManager;
    private final SocketBindingSettings defaultSocketBindingSettings;
    
    @Inject
    SimpleSocketController(ProxyManager proxyManager, SocketBindingSettings defaultSocketBindingSettings) {
        this.proxyManager = proxyManager;
        this.defaultSocketBindingSettings = defaultSocketBindingSettings;
    }
    
    public Socket connect(NBSocketFactory factory, InetSocketAddress addr, int timeout, ConnectObserver observer) throws IOException {
        return connect(null, null, factory, addr, timeout, observer);
    }
    
    public Socket connect(NBSocket socket, PerCallSocketBindingSettings bindingSettings, InetSocketAddress addr, int timeout, ConnectObserver observer) throws IOException {
        return connect(socket, bindingSettings, null, addr, timeout, observer);    
    }
    
    /**
     * Makes a connection to the given InetSocketAddress.
     * If observer is null, this will block.
     * Otherwise, the observer will be notified of success or failure.
     */
    public Socket connect(NBSocket socket, PerCallSocketBindingSettings bindingSettings, NBSocketFactory factory, InetSocketAddress addr, int timeout, ConnectObserver observer) 
      throws IOException {  
        ProxyType proxyType = proxyManager.getProxyType(addr.getAddress());  
                       
        if (proxyType != ProxyType.NONE)  
            return connectProxy(socket, bindingSettings, factory, proxyType, addr, timeout, observer);  
        else
            return connectPlain(socket, bindingSettings, factory, addr, timeout, observer);  
    }

    /** Allows endless # of sockets. */
    public int getNumAllowedSockets() {
        return Integer.MAX_VALUE;
    }

    /** Does nothing. */
    public boolean removeConnectObserver(ConnectObserver observer) {
        return false;
    }
    
    /** Returns 0. */
    public int getNumWaitingSockets() {
        return 0;
    }

    /** 
     * Establishes a connection to the given host.
     *
     * If observer is null, this will block until a connection is established or an IOException is thrown.
     * Otherwise, this will return immediately and the Observer will be notified of success or failure.
     */
    protected Socket connectPlain(NBSocket socket, PerCallSocketBindingSettings bindingSettings, NBSocketFactory factory, InetSocketAddress addr, int timeout, ConnectObserver observer)
        throws IOException {
        
        if(socket == null) {
            socket = factory.createSocket();
        }
        bindSocket(socket, bindingSettings);
        
        if(observer == null)
            socket.connect(addr, timeout); // blocking
        else
            socket.connect(addr, timeout, observer); // non-blocking
        
        return socket;
    }
    
    /** Attempts to bind the Socket using the values from socketBindingSettings,
     *  or ConnectionSettings if none are specified. */
    protected void bindSocket(NBSocket socket, PerCallSocketBindingSettings socketBindingSettings) {
        if(socketBindingSettings != null && socketBindingSettings.isSocketBindingRequired()) {
            String bindAddrString = socketBindingSettings.getAddressToBindTo();
            int bindPortInt = socketBindingSettings.getPortToBindTo();                
            InetSocketAddress bindAddr = new InetSocketAddress(bindAddrString, bindPortInt);
            bindSocket(socket, socketBindingSettings, bindAddr);
        } else if(defaultSocketBindingSettings.isSocketBindingRequired()) {
            String bindAddrString = defaultSocketBindingSettings.getAddressToBindTo();
            if(lastBindAddr == null
              || !lastBindAddr.getAddress().getHostAddress().equals(bindAddrString))
                lastBindAddr = new InetSocketAddress(bindAddrString, 0);
            bindSocket(socket, defaultSocketBindingSettings, lastBindAddr);
        }
    }

    private void bindSocket(NBSocket socket, SocketBindingSettings socketBindingSettings, InetSocketAddress address) {
        try {            
            socket.bind(address);
        } catch(IOException iox) {
            socketBindingSettings.bindingFailed();
        }
    }

    /**
     * Connects to a host using a proxy.
     */
    protected Socket connectProxy(NBSocket socket, PerCallSocketBindingSettings bindingSettings, NBSocketFactory factory, ProxyType type, InetSocketAddress addr, int timeout, ConnectObserver observer)
      throws IOException {
        InetSocketAddress proxyAddr = proxyManager.getProxyHost();
        
        if(observer != null) {
            return connectPlain(socket, bindingSettings, factory, proxyAddr, timeout, proxyManager.getConnectorFor(type, observer, addr, timeout));
        } else {
            Socket proxySocket = connectPlain(socket, bindingSettings, factory, proxyAddr, timeout, null);
            try {
                return proxyManager.establishProxy(type, proxySocket, addr, timeout);
            } catch(IOException iox) {
                // Ensure the proxySocket is closed.  Not all proxies cleanup correctly.
                try { proxySocket.close(); } catch(IOException ignored) {}
                throw iox;
            }
        }
    }

}
