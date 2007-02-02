package com.limegroup.gnutella.chat;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class InstantMessengerTest extends BaseTestCase {

    private static final int CHAT_PORT = 9999;
    private static Acceptor acceptThread;
    private MyActivityCallback callback;
    private InstantMessenger messenger;
    
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
        doSettings();
        
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

    public static void globealTearDown() throws Exception {
        acceptThread.setListeningPort(0);
    }
    
    private static void doSettings() {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        doSettings();
        
        acceptThread.setListeningPort(CHAT_PORT);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (messenger != null) {
            try {
                messenger.stop();
            } catch (Exception e) {
            }
        }
    }
      
    public void testChatThroughAcceptor() throws Exception {
        callback = new MyActivityCallback();
        messenger = new InstantMessenger("localhost", CHAT_PORT, ChatManager.instance(), callback);
        messenger.start();
        Thread.sleep(500);
        assertTrue("Could not establish connection", callback.acceptChat); 
        sendMessage("foo", messenger, (MyActivityCallback) RouterService.getCallback());
        sendMessage("bar", messenger, (MyActivityCallback) RouterService.getCallback());
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
