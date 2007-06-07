package org.limewire.store.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A simple class to implement {@link #open(String, int)} by
 * <code>return new URL(url).openStream();</code>.
 */
public class URLSocketOpenner implements DispatcherSupport.OpensSocket {

    public Socket open(String host, int port) throws IOException {
        final Socket res = new Socket();
        res.connect(new InetSocketAddress(host, port), 1000);
        return res;
    }

}
