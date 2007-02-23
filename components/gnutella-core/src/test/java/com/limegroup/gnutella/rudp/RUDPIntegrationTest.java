package com.limegroup.gnutella.rudp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;

import junit.framework.Test;

import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.IOErrorObserver;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.UDPConnectionProcessor;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.StandardMessageRouter;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageHandler;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageHelper;
import com.limegroup.gnutella.rudp.messages.RUDPMessageHandlerHelper;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class RUDPIntegrationTest extends LimeTestCase {
    
    private static final int PORT = 9313;
    
    private MessageHandler ackHandler;
    private MessageHandler dataHandler;
    private MessageHandler finHandler;
    private MessageHandler keepAliveHandler;
    private MessageHandler synHandler;
    private static DatagramSocket UDP_SOCKET;

    public RUDPIntegrationTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RUDPIntegrationTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void setSettings() throws Exception {
        String localIP = InetAddress.getLocalHost().getHostAddress();
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] {"127.*.*.*", localIP});
        ConnectionSettings.PORT.setValue(PORT);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);    
        ConnectionSettings.USE_GWEBCACHE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
    }
    
    public static void globalSetUp() throws Exception {
        setSettings();
        RouterService rs = new RouterService(new ActivityCallbackStub(), new StandardMessageRouter());
        rs.start();
        Thread.sleep(1000);
        UDP_SOCKET = new DatagramSocket();
    }
    
    public void setUp() throws Exception {
        setSettings();
    }
    
    public static void globalTearDown() throws Exception {
        if(UDP_SOCKET != null)
            UDP_SOCKET.close();
    }
    
    public void testLimeRUDPMessageHandlerIsInstalled() throws Exception {
        storeHandlers();
        assertEquals(LimeRUDPMessageHandler.class, ackHandler.getClass());
        assertEquals(LimeRUDPMessageHandler.class, dataHandler.getClass());
        assertEquals(LimeRUDPMessageHandler.class, finHandler.getClass());
        assertEquals(LimeRUDPMessageHandler.class, keepAliveHandler.getClass());
        assertEquals(LimeRUDPMessageHandler.class, synHandler.getClass());
    }
    
    public void testIncomingUDPPacketsSentToHandler() throws Exception {
        storeHandlers();
        try {
            StubRUDPMessageHandler handler = new StubRUDPMessageHandler();
            RUDPMessageHandlerHelper.addHandler(handler);
            checkMessage(handler, LimeRUDPMessageHelper.getAck(1));
            checkMessage(handler, LimeRUDPMessageHelper.getData(1));
            checkMessage(handler, LimeRUDPMessageHelper.getFin(1));
            checkMessage(handler, LimeRUDPMessageHelper.getKeepAlive(1));
            checkMessage(handler, LimeRUDPMessageHelper.getSyn(1));
        } finally {
            revertToStoredHandlers();
        }
    }
    
    public void testPacketsSentToMultiplexorAndGoesToProcessor() throws Exception {
        StubUDPConnectionProcessor stub = new StubUDPConnectionProcessor(RouterService.getUDPSelectorProvider().getContext());
        SelectableChannel channel = createUDPSocketChannel(stub);
        assertEquals(0, stub.id);
        // This is done only to allow the multiplexor to learn about the processor.
        NIODispatcher.instance().register(channel,  new StubIOErrorObserver());
        Thread.sleep(100);
        assertNotEquals(0, stub.id);
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", UDP_SOCKET.getLocalPort());
        stub.addr = addr;
        stub.isConnecting = false;
        checkMessage(stub, LimeRUDPMessageHelper.getAck(stub.id));
        checkMessage(stub, LimeRUDPMessageHelper.getData(stub.id));
        checkMessage(stub, LimeRUDPMessageHelper.getFin(stub.id));
        checkMessage(stub, LimeRUDPMessageHelper.getKeepAlive(stub.id));
        checkMessage(stub, LimeRUDPMessageHelper.getSyn(stub.id));
        channel.close();
    }
    
    private SelectableChannel createUDPSocketChannel(UDPConnectionProcessor stub) throws Exception {
        Class clazz = Class.forName("org.limewire.rudp.UDPSocketChannel");
        return (SelectableChannel)PrivilegedAccessor.invokeConstructor(clazz, new Object[] { stub }, new Class[] { UDPConnectionProcessor.class });
    }
    
    private void checkMessage(MessageObserver handler, Message m) throws Exception {
        assertNull(handler.getCurrentMessage());
        sendToUDP(m);
        Message read = handler.getLastMessage(100);
        assertNotNull(read);
        assertEquals(m.getClass(), read.getClass());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.write(out);
        byte[] b1 = out.toByteArray();
        out.reset();
        read.write(out);
        byte[] b2 = out.toByteArray();
        assertEquals(b1, b2);
    }
    
    private void sendToUDP(Message m) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.write(out);
        DatagramPacket p = new DatagramPacket(out.toByteArray(), out.size(), new InetSocketAddress("127.0.0.1", PORT));
        UDP_SOCKET.send(p);
    }
    
    private void storeHandlers() throws Exception {
        RUDPMessageHandlerHelper.setHandlerFields(this, "ackHandler", "dataHandler", "finHandler", "keepAliveHandler", "synHandler");
    }
    
    private void revertToStoredHandlers() throws Exception {
        RUDPMessageHandlerHelper.setHandlers(ackHandler, dataHandler, finHandler, keepAliveHandler, synHandler);
    }
    
    private interface MessageObserver {
        public Message getCurrentMessage();
        public Message getLastMessage(int waitTime) throws Exception;
    }
    
    private class StubRUDPMessageHandler implements MessageHandler, MessageObserver {
        private Message lastMessage;
        public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            synchronized(this) {
                lastMessage = msg;
                notify();
            }
        }
        
        public synchronized Message getCurrentMessage() {
            return lastMessage;
        }
        
        public Message getLastMessage(int waitTime) throws Exception {
            synchronized(this) {
                if(lastMessage == null)
                    wait(waitTime);
                Message m = lastMessage;
                lastMessage = null;
                return m;
            }
        }
    }
    
    private class StubUDPConnectionProcessor extends UDPConnectionProcessor implements MessageObserver {
        private volatile byte id;
        private volatile InetSocketAddress addr;
        private volatile RUDPMessage lastMessage;
        private volatile boolean isConnecting;
        
        StubUDPConnectionProcessor(RUDPContext context) {
            super(null, context);
        }
        
        @Override
        protected void setConnectionId(byte id) {
            this.id = id;
            super.setConnectionId(id);
        }

        @Override
        protected InetSocketAddress getSocketAddress() {
            return addr;
        }

        @Override
        protected synchronized void handleMessage(RUDPMessage msg) {
            this.lastMessage = msg;
            notify();
        }

        @Override
        protected synchronized boolean isConnecting() {
            return isConnecting;
        }
        
        public synchronized Message getLastMessage(int waitTime) throws Exception {
            if(lastMessage == null)
                wait(waitTime);
            Message m = (Message)lastMessage;
            lastMessage = null;
            return m;
        }

        public synchronized Message getCurrentMessage() {
            return (Message)lastMessage;
        }
    }
    
    private class StubIOErrorObserver implements IOErrorObserver {
        public void handleIOException(IOException iox) {
        }

        public void shutdown() {
        }
        
    }
}
