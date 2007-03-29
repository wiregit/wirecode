package com.limegroup.gnutella.chat;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.nio.channel.AbstractChannelInterestReader;
import org.limewire.nio.channel.AbstractChannelWriter;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.statemachine.IOState;
import org.limewire.nio.statemachine.IOStateMachine;
import org.limewire.nio.statemachine.IOStateObserver;
import org.limewire.util.BufferUtils;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.SimpleHTTPHeaderValue;
import com.limegroup.gnutella.http.SimpleReadHeaderState;
import com.limegroup.gnutella.http.SimpleWriteHeaderState;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.Sockets;

/**
 * This class implements a simple chat protocol that allows to exchange text
 * messages over a socket connection.
 * 
 * <p>
 * The protocol is similar to a Gnutella handshake:
 * 
 * <pre>
 *       -&gt; CHAT CONNECT/0.1
 *       -&gt; User-Agent: LimeWire @version@
 *       -&gt;
 *       &lt;- CHAT/0.1 200 OK
 *       &lt;-
 *       -&gt; CHAT/0.1 200 OK
 *       -&gt;
 *       -&gt; [message]\r\n
 *       &lt;- [message]\r\n
 *       ...
 * </pre>
 */
public class InstantMessenger implements Chatter {

    private static final Log LOG = LogFactory.getLog(InstantMessenger.class);

    private static final String CHAT_CONNECT = "CHAT CONNECT/0.1";

    private static final String CHAT_OK = "CHAT/0.1 200 OK";

    private static final String CONNECT = "CONNECT/0.1";
    
    private static final String CHARSET = "UTF-8";

    private Socket socket;

    private final String host;

    private final int port;

    private ActivityCallback callback;

    private ChatManager manager;

    private IOStateMachine shaker;

    private MessageReceiver receiver;

    private MessageSender sender;

    private boolean outgoing;

    private boolean stopped;

    /**
     * Constructor for an incoming chat request.
     */
    public InstantMessenger(Socket socket, ChatManager manager,
            ActivityCallback callback) {
        if (socket == null || manager == null || callback == null) {
            throw new IllegalArgumentException();
        }
        
        this.manager = manager;
        this.socket = socket;
        this.port = socket.getPort();
        this.host = socket.getInetAddress().getHostAddress();
        this.callback = callback;
    }

    /**
     * Constructor for an outgoing chat request
     */
    public InstantMessenger(final String host, final int port,
            ChatManager manager, ActivityCallback callback) {
        if (host == null || manager == null || callback == null) {
            throw new IllegalArgumentException();
        }
        
        this.host = host;
        this.port = port;
        this.manager = manager;
        this.callback = callback;
        this.outgoing = true;
    }

    public void start() {
        if (outgoing) {
            try {
                Sockets.connect(host, port, Constants.TIMEOUT,
                        new ConnectObserver() {
                            public void handleConnect(Socket socket)
                                    throws IOException {
                                InstantMessenger.this.socket = socket;
                                shake(createOutgoingShakeStates());
                            }

                            public void handleIOException(IOException e) {
                                LOG.error("Unexpected exception", e);
                                handleException(e);
                            }

                            public void shutdown() {
                                LOG.warn("Could not establish chat connection to "
                                        + host + ":" + port);
                                stop();
                            }

                        });
            } catch (IOException e) {
                // should never happen since we are connecting in the background
                LOG.warn("Unexpected exception", e);
                handleException(e);
            }
        } else {
            shake(createIncomingShakeStates());
        }
    }

    /**
     * send a message accross the socket to the other host as with stop, this is
     * alway safe to call, but it is recommended that the gui discourage the
     * user from calling it when a connection is not yet established.
     */
    public boolean send(String message) {
        synchronized (this) {
            if (stopped || sender == null) {
                return false;
            }
        }
        
        try {
            sender.sendMessage(message + "\n");
        } catch (IOException e) {
            stop();
            return false;
        }
        return true;
    }

    /**
     * Returns the host name to which the socket is connected.
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port to which the socket is connected.
     */
    public int getPort() {
        return port;
    }

    public void blockHost(String host) {
        manager.blockHost(host);
    }

    private List<IOState> createIncomingShakeStates() {
        List<IOState> states = new ArrayList<IOState>(3);
        states.add(new ReadChatConnectHeaderState());
        states.add(new SimpleWriteHeaderState(CHAT_OK,
                new HashMap<HTTPHeaderName, HTTPHeaderValue>(), null));
        states.add(new ReadChatHeaderState());
        return states;
    }

    private List<IOState> createOutgoingShakeStates() {
        Map<HTTPHeaderName, HTTPHeaderValue> headers = new HashMap<HTTPHeaderName, HTTPHeaderValue>();
        headers.put(HTTPHeaderName.USER_AGENT, new SimpleHTTPHeaderValue(
                LimeWireUtils.getVendor()));

        List<IOState> states = new ArrayList<IOState>(3);
        states.add(new SimpleWriteHeaderState(CHAT_CONNECT, headers, null));
        states.add(new ReadChatHeaderState());
        states.add(new SimpleWriteHeaderState(CHAT_OK,
                new HashMap<HTTPHeaderName, HTTPHeaderValue>(), null));
        return states;
    }

    /**
     * Invoked when the handshake completes.
     */
    private void handshakeCompleted() {
        try {
            socket.setSoTimeout(0);
        } catch (SocketException e) {
            LOG.warn("Could not set socket timeout", e);
        }

        synchronized (this) {
            receiver = new MessageReceiver();
            sender = new MessageSender();
        }

        ((NIOMultiplexor) socket).setReadObserver(receiver);
        ((NIOMultiplexor) socket).setWriteObserver(sender);

        callback.acceptChat(this);
    }

    private void handleException(IOException e) {
        callback.chatErrorMessage(this, e.getMessage());
        stop();
    }

    private void shake(List<IOState> states) {
        this.shaker = new IOStateMachine(new IOStateObserver() {
            public void handleStatesFinished() {
                handshakeCompleted();
            }

            public void handleIOException(IOException e) {
                handleException(e);
            }

            public void shutdown() {
                stop();
            }
        }, states);

        try {
            socket.setSoTimeout(Constants.TIMEOUT);
        } catch (SocketException e) {
            LOG.warn("Could not set socket timeout", e);
        }
        ((NIOMultiplexor) socket).setReadObserver(shaker);
        ((NIOMultiplexor) socket).setWriteObserver(shaker);
    }

    private class MessageReceiver extends AbstractChannelInterestReader {

        @Override
        protected int getBufferSize() {
            return 1024;
        }

        @Override
        public void handleIOException(IOException e) {
            handleException(e);
        }

        public void handleRead() throws IOException {
            int read = 0;
            while (buffer.hasRemaining() && (read = source.read(buffer)) > 0)
                ;
                
            flushBuffer();
            
            if (read == -1) {
                stop();
            }
        }

        private void flushBuffer() {
            buffer.flip();
            StringBuilder sb = new StringBuilder();
            while (BufferUtils.readLine(buffer, sb)) {
                callback.receiveMessage(InstantMessenger.this, sb.toString());
            }

            if (buffer.hasRemaining()) {
                buffer.compact();
            } else {
                buffer.clear();
            }
        }
        
        @Override
        public void shutdown() {
            stop();
        }
    }

    private class MessageSender extends AbstractChannelWriter {

        public MessageSender() {
            super(1024);
        }

        @Override
        public void handleIOException(IOException e) {
            InstantMessenger.this.handleException(e);
        }

        public void sendMessage(String message) throws IOException {
            put(message.getBytes(CHARSET));
        }

        @Override
        public void shutdown() {
            stop();
        }
        
    }

    private class ReadChatConnectHeaderState extends SimpleReadHeaderState {

        public ReadChatConnectHeaderState() {
            super(null, ConnectionSettings.MAX_HANDSHAKE_HEADERS.getValue(),
                    ConnectionSettings.MAX_HANDSHAKE_LINE_SIZE.getValue());
        }

        @Override
        protected void processConnectLine() throws IOException {
            if (!CONNECT.equals(connectLine)) {
                throw new IOException("Invalid handshake: " + connectLine);
            }
        }
    }

    private class ReadChatHeaderState extends SimpleReadHeaderState {

        public ReadChatHeaderState() {
            super(null, ConnectionSettings.MAX_HANDSHAKE_HEADERS.getValue(),
                    ConnectionSettings.MAX_HANDSHAKE_LINE_SIZE.getValue());
        }

        @Override
        protected void processConnectLine() throws IOException {
            if (!CHAT_OK.equals(connectLine)) {
                throw new IOException("Invalid handshake: " + connectLine);
            }
        }
    }

    /**
     * Stop the chat, and close the connections this is always safe to call.
     */
    public void stop() {
        synchronized (this) {
            if (stopped) {
                return;
            }
            stopped = true;
        }
        
        if (socket != null && !socket.isClosed()) {
            IOUtils.close(socket);
            socket = null;
        }
        callback.chatUnavailable(this);
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    public synchronized boolean isConnected() {
        return !stopped;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[host=" + host + ":" + port + ",outgoing=" + outgoing + "]";
    }
    
}
