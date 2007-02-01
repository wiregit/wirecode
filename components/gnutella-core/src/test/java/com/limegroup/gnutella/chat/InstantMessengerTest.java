package com.limegroup.gnutella.chat;

import java.net.ServerSocket;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class InstantMessengerTest extends BaseTestCase {

    private static final int CHAT_PORT = 9999;
    private static Acceptor acceptThread;
    private ChatManager chatManager;
    private MyActivityCallback outCallback;
//    private MyActivityCallback inCallback;
//    private ManagedThread listenerThread;
    private InstantMessenger incoming;
    private InstantMessenger outgoing;
    private ServerSocket listenerSocket;
    
    public InstantMessengerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(InstantMessengerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        new RouterService(new MyActivityCallback());
        //RouterService.getConnectionManager().initialize();
        ChatManager.instance().initialize();
        
        // start it up!
        acceptThread = new Acceptor();
        acceptThread.start();
        
        // Give thread time to find and open it's sockets.   This race condition
        // is tolerated in order to speed up LimeWire startup
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {}                
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        acceptThread.setListeningPort(CHAT_PORT);
        
        chatManager = new ChatManager();
        outCallback = new MyActivityCallback();
//        inCallback = new MyActivityCallback();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        acceptThread.setListeningPort(0);
        
        if (incoming != null) {
            try {
                incoming.stop();
            } catch (Exception e) {
            }
        }
        
        if (outgoing != null) {
            try {
                outgoing.stop();
            } catch (Exception e) {
            }
        }
        
        if (listenerSocket != null) {
            try {
                listenerSocket.close();
            } catch (Exception e) {
            }
        }
    }
    
//    public void testDirectChat() throws Exception {
//        listenerThread = new ManagedThread(new Runnable() {
//            public void run() {
//                try {
//                    listenerSocket = new ServerSocket(CHAT_PORT);
//                    Socket socket = listenerSocket.accept();
//                    
//                    incoming = new InstantMessenger(socket, chatManager, inCallback);
//                    incoming.start();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }            
//        });
//        listenerThread.start();
//        
//        outgoing = new InstantMessenger("localhost", CHAT_PORT, chatManager, outCallback);
//        outgoing.start();
//        Thread.sleep(1000);
//        assertTrue("Could not establish connection", outCallback.acceptChat);
//
//        sendMessage("foo", outgoing, inCallback);
//        sendMessage("bar", outgoing, inCallback);
//        sendMessage("baz", incoming, outCallback);
//        sendMessage("Umlaut\u00E4", outgoing, inCallback);
//    }
    
    public void testChatThroughAcceptor() throws Exception {
        outgoing = new InstantMessenger("localhost", CHAT_PORT, chatManager, outCallback);
        outgoing.start();
        Thread.sleep(1000);
        assertTrue("Could not establish connection", outCallback.acceptChat); 
        sendMessage("foo", outgoing, (MyActivityCallback) RouterService.getCallback());
        sendMessage("bar", outgoing, (MyActivityCallback) RouterService.getCallback());
    }
    
    private void sendMessage(String message, InstantMessenger sender, MyActivityCallback receiver) {
        sender.send(message);
        synchronized (receiver) {
            if (receiver.message == null) {
                try {
                    receiver.wait(1000);
                } catch (InterruptedException e) {
                    fail("Message not received");
                }
            }
        }
        assertEquals(message, receiver.message);
        receiver.message = null;
    }
    
    private static class MyActivityCallback extends ActivityCallbackStub {
        
        private String message;
        private boolean acceptChat;

        @Override
        public synchronized void acceptChat(Chatter chatter) {
            acceptChat = true;
            notify();
        }
        
        @Override
        public synchronized void receiveMessage(Chatter chatter, String message) {
            this.message = message;
            notify();
        }
        
    }
    
}
