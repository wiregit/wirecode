package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.io.NBSocket;
import com.limegroup.gnutella.io.SocketFactory;
import com.limegroup.gnutella.settings.ConnectionSettings;

class SimpleSocketController implements SocketController {

    /**
     * Makes a connection to the given InetSocketAddress.
     * If observer is null, this will block.
     * Otherwise, the observer will be notified of success or failure.
     */
    public Socket connect(InetSocketAddress addr, int timeout, ConnectObserver observer) 
      throws IOException {  
        int proxyType = ProxyUtils.getProxyType(addr.getAddress());  
                       
        if (proxyType != ConnectionSettings.C_NO_PROXY)  
            return connectProxy(proxyType, addr, timeout, observer);  
        else
            return connectPlain(addr, timeout, observer);  
    }

    /** Allows endless # of sockets. */
    public int getNumAllowedSockets() {
        return Integer.MAX_VALUE;
    }

    /** Does nothing. */
    public boolean removeConnectObserver(ConnectObserver observer) {
        return false;
    }

    /** 
     * Establishes a connection to the given host.
     *
     * If observer is null, this will block until a connection is established or an IOException is thrown.
     * Otherwise, this will return immediately and the Observer will be notified of success or failure.
     */
    protected Socket connectPlain(InetSocketAddress addr, int timeout, ConnectObserver observer)
        throws IOException {
        
        NBSocket socket = SocketFactory.newSocket();
        
        if(observer == null)
            socket.connect(addr, timeout); // blocking
        else
            socket.connect(addr, timeout, observer); // non-blocking
        
        return socket;
    }
    
    /**
     * Connects to a host using a proxy.
     */
    protected Socket connectProxy(int type, InetSocketAddress addr, int timeout, ConnectObserver observer)
      throws IOException {
        String proxyHost = ConnectionSettings.PROXY_HOST.getValue();
        int proxyPort = ConnectionSettings.PROXY_PORT.getValue();
        InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
        
        if(observer != null) {
            return connectPlain(proxyAddr, timeout, new ProxyUtils.ProxyConnector(type, observer, addr, timeout));
        } else {
            Socket proxySocket = connectPlain(proxyAddr, timeout, null);
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
