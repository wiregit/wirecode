package org.limewire.lws.server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.limewire.lws.server.DispatcherSupport;

/**
 * A simple class to implement {@link DispatcherSupport.OpensSocket#open(String, int)}.
 */
public class URLSocketOpenner implements DispatcherSupport.OpensSocket {

    public Socket open(String host, int port) throws IOException {
        final Socket res = new Socket();
        res.connect(new InetSocketAddress(host, port), 1000);
        return res;
    }

}
