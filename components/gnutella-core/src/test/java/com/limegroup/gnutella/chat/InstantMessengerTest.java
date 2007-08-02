package com.limegroup.gnutella.chat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.util.AssertComparisons;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class InstantMessengerTest extends BaseTestCase {

    private static final int CHAT_PORT = 9999;
    private static Acceptor acceptThread;
    private static MyActivityCallback receiver;
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
        
        receiver = new MyActivityCallback();
        new RouterService(receiver);
        //RouterService.getConnectionManager().initialize();
        ChatManager.instance().initialize();
        
        // start it up!
        acceptThread = new Acceptor(ProviderHacks.getNetworkManager());
        acceptThread.start();
    }

    public static void globealTearDown() throws Exception {
        acceptThread.setListeningPort(0);
        receiver = null;
        acceptThread = null;
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
        callback.waitForConnect(1000);
        assertTrue(messenger.isConnected());
        assertTrue(messenger.send("foo"));
        receiver.assertMessageReceived("foo", 1000);
        assertTrue(messenger.send("bar"));
        receiver.assertMessageReceived("bar", 1000);
    }
    
    public void testSendHugeMessage() throws Exception {
        callback = new MyActivityCallback();
        messenger = new InstantMessenger("localhost", CHAT_PORT, ChatManager.instance(), callback);
        messenger.start();
        callback.waitForConnect(1000);
        assertFalse(messenger.send(new String(new char[10000])));
    }
    
    private static class MyActivityCallback extends ActivityCallbackStub {
        
        private volatile String message;
        private volatile CountDownLatch connectLatch = new CountDownLatch(1);

        @Override
        public void acceptChat(Chatter chatter) {
            connectLatch.countDown();
        }
        
        @Override
        public synchronized void receiveMessage(Chatter chatter, String message) {
            this.message = message;
            notify();
        }

        public void waitForConnect(long timeout) throws Exception {
            AssertComparisons.assertTrue("Timeout while waiting for connect", connectLatch.await(timeout, TimeUnit.MILLISECONDS));
        }
        
        public void assertMessageReceived(String message, long timeout) throws Exception {
            synchronized (this) {
                if (this.message == null) {
                    this.wait(1000);
                }
            }
            assertEquals("Message not received", message, this.message);
            this.message = null;
        }
        
    }
    
}
