package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.limewire.nio.NBSocket;
import org.limewire.nio.NBSocketFactory;
import org.limewire.nio.observer.ConnectObserver;

import com.limegroup.gnutella.settings.ConnectionSettings;

class SimpleSocketController implements SocketController {    

    /** The possibly null address to bind to. */
    private InetSocketAddress lastBindAddr;
    
    /**
     * Makes a connection to the given InetSocketAddress.
     * If observer is null, this will block.
     * Otherwise, the observer will be notified of success or failure.
     */
    public Socket connect(NBSocketFactory factory, InetSocketAddress addr, int timeout, ConnectObserver observer) 
      throws IOException {  
        int proxyType = ProxyUtils.getProxyType(addr.getAddress());  
                       
        if (proxyType != ConnectionSettings.C_NO_PROXY)  
            return connectProxy(factory, proxyType, addr, timeout, observer);  
        else
            return connectPlain(factory, addr, timeout, observer);  
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
    protected Socket connectPlain(NBSocketFactory factory, InetSocketAddress addr, int timeout, ConnectObserver observer)
        throws IOException {
        
        NBSocket socket = factory.createSocket();
        bindSocket(socket);
        
        if(observer == null)
            socket.connect(addr, timeout); // blocking
        else
            socket.connect(addr, timeout, observer); // non-blocking
        
        return socket;
    }
    
    /** Attempts to bind the Socket using the values from ConnectionSettings. */
    protected void bindSocket(Socket socket) {
        if(ConnectionSettings.CUSTOM_NETWORK_INTERFACE.getValue()) {
            String bindAddrString = ConnectionSettings.CUSTOM_INETADRESS.getValue();
            try {
                if(lastBindAddr == null || !lastBindAddr.getAddress().getHostAddress().equals(bindAddrString))
                    lastBindAddr = new InetSocketAddress(bindAddrString, 0);
                socket.bind(lastBindAddr);
            } catch(IOException iox) {
                ConnectionSettings.CUSTOM_NETWORK_INTERFACE.setValue(false);
            }
        }
    }
    
    /**
     * Connects to a host using a proxy.
     */
    protected Socket connectProxy(NBSocketFactory factory, int type, InetSocketAddress addr, int timeout, ConnectObserver observer)
      throws IOException {
        String proxyHost = ConnectionSettings.PROXY_HOST.getValue();
        int proxyPort = ConnectionSettings.PROXY_PORT.getValue();
        InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
        
        if(observer != null) {
            return connectPlain(factory, proxyAddr, timeout, new ProxyUtils.ProxyConnector(type, observer, addr, timeout));
        } else {
            Socket proxySocket = connectPlain(factory, proxyAddr, timeout, null);
            try {
                return ProxyUtils.establishProxy(type, proxySocket, addr, timeout);
            } catch(IOException iox) {
                // Ensure the proxySocket is closed.  Not all proxies cleanup correctly.
                try { proxySocket.close(); } catch(IOException ignored) {}
                throw iox;
            }
        }
    }

}
