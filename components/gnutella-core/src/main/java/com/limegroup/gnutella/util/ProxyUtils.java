package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.statemachine.BlockingStateMachine;
import org.limewire.nio.statemachine.IOState;
import org.limewire.nio.statemachine.IOStateMachine;
import org.limewire.nio.statemachine.IOStateObserver;
import org.limewire.nio.statemachine.PossibleIOState;
import org.limewire.nio.statemachine.ReadSkipState;
import org.limewire.nio.statemachine.ReadState;
import org.limewire.nio.statemachine.SimpleReadState;
import org.limewire.nio.statemachine.SimpleWriteState;
import org.limewire.util.BufferUtils;

import com.limegroup.gnutella.settings.ConnectionSettings;


/**
 * A collection of utilities for proxy connection establishment.
 * Used with Sockets.
 */
class ProxyUtils {
    
    private static final Log LOG = LogFactory.getLog(ProxyUtils.class);
    
    private ProxyUtils() {}
    
    /**
     * Determines the kind of proxy to use for connecting to the given address.
     */
    static int getProxyType(InetAddress address) {
		// if the user specified that he wanted to use a proxy to connect
		// to the network, we will use that proxy unless the host we
		// want to connect to is a private address
		int connectionType = ConnectionSettings.CONNECTION_METHOD.getValue();
		boolean valid =  connectionType != ConnectionSettings.C_NO_PROXY &&
                        (!NetworkUtils.isPrivateAddress(address) ||
                         ConnectionSettings.USE_PROXY_FOR_PRIVATE.getValue());
        if(valid)
            return connectionType;
        else
            return ConnectionSettings.C_NO_PROXY;
    }
    
    /**
     * Establishes a proxy connection on the given socket.
     */
    static Socket establishProxy(int type, Socket proxySocket, InetSocketAddress addr, int timeout) throws IOException {
        proxySocket.setSoTimeout(timeout);
        List<IOState> states = getProxyStates(type, addr);
        InputStream in = proxySocket.getInputStream();
        OutputStream out = proxySocket.getOutputStream();
        BlockingStateMachine bsm = new BlockingStateMachine(states, in, out);
        try {
            bsm.process();
        } finally {
            bsm.shutdown(); // release the buffer.
        }
        
        proxySocket.setSoTimeout(0);
        return proxySocket;
    }
    
    /** Returns the correct states for the given proxy. */
    private static List<IOState> getProxyStates(int type, InetSocketAddress addr) throws IOException {
        switch(type) {
        case ConnectionSettings.C_HTTP_PROXY: return getHttpStates(addr);
        case ConnectionSettings.C_SOCKS4_PROXY: return getSocksV4States(addr);
        case ConnectionSettings.C_SOCKS5_PROXY: return getSocksV5States(addr);
        default:
            throw new IOException("Unknown proxy type.");
        }
    }
    
    /** Returns a list of states for processing a SOCKSv4 proxy. */
    private static List<IOState> getSocksV4States(final InetSocketAddress addr) {
        List<IOState> states = new LinkedList<IOState>();

        byte[] hostBytes = addr.getAddress().getAddress();
        int port = addr.getPort();
        byte[] portBytes = new byte[2];
        portBytes[0] = ((byte) (port >> 8));
        portBytes[1] = ((byte) port);
        
        boolean auth = ConnectionSettings.PROXY_AUTHENTICATE.getValue();
        String authName = ConnectionSettings.PROXY_USERNAME.getValue();
        byte[] authData = auth ? authName.getBytes() : DataUtils.EMPTY_BYTE_ARRAY;
        ByteBuffer outgoing = ByteBuffer.allocate(2 + portBytes.length + hostBytes.length + authData.length + 1);
        outgoing.put((byte)0x04);
        outgoing.put((byte)0x01);
        outgoing.put(portBytes);
        outgoing.put(hostBytes);
        outgoing.put(authData);
        outgoing.put((byte)0x00);
        outgoing.flip();
        states.add(new SimpleWriteState(outgoing));

		// read response
        states.add(new SimpleReadState(8) {
            @Override
            public void validateBuffer(ByteBuffer buffer) throws IOException {
                // version should be 0 but some socks proxys answer 4
                int version = buffer.get(0);
                if (version != 0x00 && version != 0x04)
                    throw new IOException("Invalid version from socks proxy: " + version + " expected 0 or 4");
                
                // read the status, 0x5A is success
                int status = buffer.get(1);
                if (status != 0x5A)
                    throw new IOException("Request rejected with status: " + status);
            }
        });
        return states;
    }
    
    /** Returns all states necessary for a SOCKSv5 proxy connection. */
    private static List<IOState> getSocksV5States(InetSocketAddress addr) {
        List<IOState> states = new LinkedList<IOState>();
        // If authenticating, write: # of auth methods, support no auth, support usr/password auth
        // If not authenticating, write: # of auth methods, support no auth.
        byte[] auths = ConnectionSettings.PROXY_AUTHENTICATE.getValue() ?
                new byte[] { 0x02, 0x00, 0x02 } : new byte[] { 0x01, 0x00 };
        ByteBuffer outgoing = ByteBuffer.allocate(1 + auths.length);
        outgoing.put((byte)0x05);
        outgoing.put(auths);
        outgoing.flip();        
        states.add(new SimpleWriteState(outgoing));
        
        final AtomicBoolean authSwitch = new AtomicBoolean(false);        
        states.add(new SimpleReadState(2) {
            @Override
            public void validateBuffer(ByteBuffer buffer) throws IOException {
                int version = buffer.get(0);
                if (version != 0x05)
                    throw new IOException("Invalid version from socks proxy: " + version + " expected 5");
                
                // Turn on authentication, if told to.
                int auth_method = buffer.get(1);
                if(auth_method == 0x02)
                    authSwitch.set(true);
            }
        });
        
		// username/password 
		String username = ConnectionSettings.PROXY_USERNAME.getValue();
		String password = ConnectionSettings.PROXY_PASS.getValue();
        outgoing = ByteBuffer.allocate(1 + 1 + username.length() + 1 + password.length());
        outgoing.put((byte)0x01);
        outgoing.put((byte)username.length());
        outgoing.put(username.getBytes());
        outgoing.put((byte)password.length());
        outgoing.put(password.getBytes());
        outgoing.flip();
        states.add(new PossibleIOState(authSwitch, new SimpleWriteState(outgoing)));
        states.add(new PossibleIOState(authSwitch, new SimpleReadState(2) {
            @Override
            public void validateBuffer(ByteBuffer buffer) throws IOException {
                int version = buffer.get(0);
                if (version != 0x01)
                    throw new IOException("Invalid version for authentication: " + version + " expected 1");
                
                int status = buffer.get(1);
                if (status != 0x00)
                    throw new IOException("Authentication failed with status: " + status);
            }
        }));
        
        
        byte[] hostBytes = addr.getAddress().getAddress();
        int port = addr.getPort();
        byte[] portBytes = new byte[2];
        portBytes[0] = ((byte) (port >> 8));
        portBytes[1] = ((byte) port);
        outgoing = ByteBuffer.allocate(1 + 1 + 1 + 1 + hostBytes.length + portBytes.length);
        outgoing.put((byte)0x05); // version again
        outgoing.put((byte)0x01); // connect command, 
		// 0x02 would be bind, 0x03 UDP associate
        outgoing.put((byte)0x00); // reserved field, must be 0x00
        outgoing.put((byte)0x01); // address type: 0x01 is IPv4, 0x04 would be IPv6
        outgoing.put(hostBytes); //host to connect to
        outgoing.put(portBytes); //port to connect to
        outgoing.flip();
        states.add(new SimpleWriteState(outgoing));

        final AtomicLong amountToSkip = new AtomicLong(0);
        final AtomicBoolean domainLengthSwitch = new AtomicBoolean(false);
        states.add(new SimpleReadState(4) {
            @Override
            public void validateBuffer(ByteBuffer buffer) throws IOException {
                int version = buffer.get(0);
                if (version != 0x05)
                    throw new IOException("Invalid version from socks proxy: " + version + " expected 5");
                
                int status = buffer.get(1);
                if (status != 0x00)
                    throw new IOException("Request rejected with status: " + status);
                
                int addrType = buffer.get(3);
                switch(addrType) {
                case 1: amountToSkip.set(6); break; // IPv4
                case 3: domainLengthSwitch.set(true); break; // Domain Name
                case 4: amountToSkip.set(18); break; // IPv6
                }
            }
        });
        
        states.add(new PossibleIOState(domainLengthSwitch, new SimpleReadState(1) {
            @Override
            public void validateBuffer(ByteBuffer buffer) throws IOException {
                amountToSkip.set(buffer.get(0) + 2);
            }
        }));
        
        states.add(new ReadSkipState(amountToSkip));
        

		return states;
    }    

    /** Returns the states associated with an HTTP proxy. */
    private static List<IOState> getHttpStates(InetSocketAddress addr) {
        List<IOState> states = new LinkedList<IOState>();
        
		String connectString = "CONNECT " + addr.getAddress().getHostAddress() + ":" + addr.getPort() + " HTTP/1.0\r\n\r\n";
        ByteBuffer outgoing = ByteBuffer.wrap(connectString.getBytes());
        states.add(new SimpleWriteState(outgoing));
        
        // Reads until it encounters \r\n
        states.add(new ReadState() {
            private StringBuilder sb = new StringBuilder();
            private boolean found200 = false;
            private ByteBuffer buffer;
            
            @Override
            protected boolean processRead(ReadableByteChannel channel, ByteBuffer scratchBuffer) throws IOException {
              //  LOG.debug("Entered read state");
                
                if(buffer == null) {
                    buffer = scratchBuffer.slice();
                    buffer.limit(1); //process 1 byte at a time.
                }
                
                int read;
                while((read = channel.read(buffer)) > 0) {
                    buffer.flip();
                    if(BufferUtils.readLine(buffer, sb)) {
                        if(!found200) {
                            // Make sure the first line has a '200' in it.
                            if(sb.indexOf("200") == -1)
                                throw new IOException("HTTP connection failed");
                            found200 = true;
                        }
                        
                        // Once we find an empty line, we're done.
                        if(sb.length() == 0)
                            return false;
                        else
                            sb = new StringBuilder();
                    }
                    
                    if(sb.length() > 2048)
                        throw new IOException("header too big.");
                    buffer.position(0);
                    buffer.limit(1);
                }
                                
                if(read == -1)
                    throw new IOException("EOF");
                
                return true;
            }

            public long getAmountProcessed() { return -1; }
            
        });

		return states;
	}
    
    /**
     * ConnectObserver that will establish a proxy prior to delegating the connect back
     * to the delegate.
     */
    static class ProxyConnector implements ConnectObserver, IOStateObserver {
        private final int proxyType;
        private final ConnectObserver delegate;
        private final InetSocketAddress addr;
        private final int timeout;
        private volatile Socket socket;
        
        ProxyConnector(int type, ConnectObserver observer, InetSocketAddress host, int tout) {
            proxyType = type;
            delegate = observer;
            addr = host;
            timeout = tout;
        }
        
        public void handleConnect(final Socket s) throws IOException {
            this.socket = s;
            s.setSoTimeout(timeout);
            if(LOG.isDebugEnabled())
                LOG.debug("Connected to proxy, beginning proxy handshake for addr: " + addr);
            IOStateMachine machine = new IOStateMachine(this, getProxyStates(proxyType, addr));
            ((NIOMultiplexor)socket).setReadObserver(machine);
            ((NIOMultiplexor)socket).setWriteObserver(machine);
        }
       
        public void shutdown() {
            if(LOG.isDebugEnabled())
                LOG.debug("Failed to connect with proxy to addr: " + addr);
            delegate.shutdown();
        }
        
        public void handleIOException(IOException iox) {
            if(LOG.isDebugEnabled())
                LOG.debug("Failed to connect with proxy to addr: " + addr, iox);
            delegate.shutdown();
        }
        
        public ConnectObserver getDelegateObserver() {
            return delegate;
        }

        public void handleStatesFinished() {
            try {
                socket.setSoTimeout(0);
            } catch(IOException ignored) {}
            
            if(LOG.isDebugEnabled())
                LOG.debug("Finished proxy handshake, notifying connector for address: " + addr);
            try {
                delegate.handleConnect(socket);
            } catch(IOException iox) {
                IOUtils.close(socket);
                // Do not call shutdown on the delegate, since it already got the handleConnect
            }
            
        }
    }
}