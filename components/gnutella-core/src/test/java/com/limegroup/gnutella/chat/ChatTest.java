package com.limegroup.gnutella.chat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.concurrent.Providers;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.ConnectionDispatcherImpl;
import org.limewire.net.SocketAcceptor;
import org.limewire.util.AssertComparisons;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.StubSpamServices;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.util.SocketsManager;

//ITEST
public class ChatTest extends BaseTestCase {

    private static final int CHAT_PORT = 9999;

    private MyActivityCallback receiverCallback;

    private ChatManager chatManager;

    private ConnectionDispatcher connectionDispatcher;

    private MyActivityCallback callback;

    private InstantMessenger messenger;

    private InstantMessengerFactory factory;

    private SocketAcceptor acceptor;

    public ChatTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ChatTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        // make sure local connections are accepted
        LocalSocketAddressService.setSocketAddressProvider(new LocalSocketAddressProviderStub());

        receiverCallback = new MyActivityCallback();
        
        connectionDispatcher = new ConnectionDispatcherImpl();

        factory = new InstantMessengerFactoryImpl(Providers.of((ActivityCallback) receiverCallback),
                Providers.of(new SocketsManager()));
        
        chatManager = new ChatManager(new StubSpamServices(), factory);
        connectionDispatcher.addConnectionAcceptor(chatManager, false, "CHAT");
        
        acceptor = new SocketAcceptor(connectionDispatcher);
        acceptor.bind(CHAT_PORT);
    }

    @Override
    protected void tearDown() throws Exception {
        if (messenger != null) {
            try {
                messenger.stop();
            } catch (Exception e) {
            }
        }
        
        if (acceptor != null) {
            acceptor.unbind();
        }
    }

    public void testChatThroughAcceptor() throws Exception {
        callback = new MyActivityCallback();
        messenger = new InstantMessengerImpl("localhost", CHAT_PORT, callback, new SocketsManager());
        messenger.start();
        callback.waitForConnect(1000);
        assertTrue(messenger.isConnected());
        assertTrue(messenger.send("foo"));
        receiverCallback.assertMessageReceived("foo", 1000);
        assertTrue(messenger.send("bar"));
        receiverCallback.assertMessageReceived("bar", 1000);
    }

    public void testSendHugeMessage() throws Exception {
        callback = new MyActivityCallback();
        messenger = new InstantMessengerImpl("localhost", CHAT_PORT, callback, new SocketsManager());
        messenger.start();
        callback.waitForConnect(1000);
        assertFalse(messenger.send(new String(new char[10000])));
    }

    private static class MyActivityCallback extends ActivityCallbackStub {

        private volatile String message;

        private volatile CountDownLatch connectLatch = new CountDownLatch(1);

        @Override
        public void acceptChat(InstantMessenger chatter) {
            connectLatch.countDown();
        }

        @Override
        public synchronized void receiveMessage(InstantMessenger chatter, String message) {
            this.message = message;
            notify();
        }

        public void waitForConnect(long timeout) throws Exception {
            AssertComparisons.assertTrue("Timeout while waiting for connect",
                    connectLatch.await(timeout, TimeUnit.MILLISECONDS));
        }

        public void assertMessageReceived(String message, long timeout)
                throws Exception {
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
