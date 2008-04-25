package org.limewire.http.httpclient;

import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;

import org.apache.http.conn.SocketFactory;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.params.HttpParams;

import com.google.inject.Inject;

/**
 * a <code>SocketFactory</code> that can be given 
 * a <code>Socket</code> to use
 */
class SocketWrapperProtocolSocketFactory implements SocketFactory {

        private Socket socket;

        @Inject
        SocketWrapperProtocolSocketFactory() {
        }
        
        void setSocket(Socket s) {
            this.socket = s;
        }

        public Socket createSocket() throws IOException {
            return socket;
        }

        public Socket connectSocket(Socket socket, String s, int i, InetAddress inetAddress, int i1, HttpParams httpParams) throws IOException, UnknownHostException, ConnectTimeoutException {
            return socket; // TODO validate parameters actually match those of the socket
        }

        public boolean isSecure(Socket socket) throws IllegalArgumentException {
            return false; // TODO
        }
    }
