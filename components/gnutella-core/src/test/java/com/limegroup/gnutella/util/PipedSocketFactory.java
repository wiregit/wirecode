package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import com.limegroup.gnutella.io.ChannelReadObserver;
import com.limegroup.gnutella.io.ChannelWriter;
import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.io.NBSocket;
import com.limegroup.gnutella.io.NIOMultiplexor;
import com.limegroup.gnutella.io.NIOServerSocket;
import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.io.ReadWriteObserver;

/** 
 * Returns two sockets A and B, so that the input of A is connected to 
 * the output of B and vice versa.  Typical use:
 * <pre>
 *     PipedSocketFactory factory=new PipedSocketFactory("1.1.1.1", "2.2.2.2");
 *     Connection cA=new Connection(factory.getSocketA());
 *     Connection cB=new Connection(factory.getSocketB());
 *     cA.send(m1);
 *     cB.receive();
 *     cB.send(m2);
 *     cA.receive();
 * </pre>
 */
public class PipedSocketFactory {
    
    private final ServerSocket ss;
    private final String hostA;
    private final String hostB;
    private NIOSocket socketA;
    private NIOSocket socketB;
    
        
    /**
     * @param hostA the address to use for socket A
     * @param hostB the address to use for socket B
     */
    public PipedSocketFactory(String hostA, String hostB) 
      throws IOException, UnknownHostException {
        this.hostA = hostA;
        this.hostB = hostB;
        ss = new NIOServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(0));
    }

    public Socket getSocketA() throws Exception {
        if(socketA == null)
            setupSockets();
        
        return socketA;
    }

    public Socket getSocketB() throws Exception {
        if(socketB == null)
            setupSockets();
        
        return socketB;
    }
    
    private void setupSockets() throws Exception {
        socketA = new FakedNIOSocket(InetAddress.getLocalHost(), ss.getLocalPort(), hostA, hostB);
        socketB = (NIOSocket)ss.accept();
    }
    
    private static class FakedNIOSocket extends NIOSocket {
        private final String local;
        private final String remote;
        
        FakedNIOSocket(InetAddress host, int port, String local, String remote) throws IOException {
            super(host, port);
            this.local = local;
            this.remote = remote;
        }

        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByName(remote);
            } catch(UnknownHostException uhe) {
                throw new RuntimeException(uhe);
            }
        }

        public InetAddress getLocalAddress() {
            try {
                return InetAddress.getByName(local);
            } catch(UnknownHostException uhe) {
                throw new RuntimeException(uhe);
            }
        }
    }
}
