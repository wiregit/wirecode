package com.limegroup.gnutella.gui.chat;

import java.awt.event.WindowEvent;

import junit.framework.Test;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.chat.ChatManager;
import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUITestUtils;
import com.limegroup.gnutella.settings.ConnectionSettings;

public class ChatUIManagerTest extends GUIBaseTestCase {

    private static final int CHAT_PORT = 9999;

    private static Acceptor acceptThread;

    private ChatUIManager chatManager;

    private ChatFrame outgoing;

    private ChatFrame incoming;

    public ChatUIManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ChatUIManagerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        doSettings();

        GUITestUtils.initializeUI();
        ChatManager.instance().initialize();

        // start it up!
        acceptThread = new Acceptor(ProviderHacks.getNetworkManager());
        acceptThread.start();
        acceptThread.setListeningPort(CHAT_PORT);
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

        chatManager = ChatUIManager.instance();
    }

    private void setupChat() throws Exception {
        // the outgoing chat frame can not be registered with ChatUIManager
        // otherwise the connection will be closed since there can only be
        // one chat frame per host
        InstantMessenger chat = new InstantMessenger("localhost", CHAT_PORT,
                ChatManager.instance(), RouterService.getCallback());
        outgoing = new ChatFrame(chat);
        chat.start();
        chat.waitForConnect(4000);
        assertTrue("Could not establish outgoing chat connection", chat.isConnected());
        LimeTestUtils.waitForNIO();
        GUITestUtils.waitForSwing();
        incoming = getIncomingChat();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (outgoing != null) {
            outgoing.getChat().stop();
            outgoing.dispose();
            outgoing = null;
        }

        chatManager.clear();
    }

    // tests that chats to localhost are closed right away
    public void testChatLocalhost() throws Exception {
        outgoing = GUIMediator.createChat("127.0.0.1", CHAT_PORT);
        ((InstantMessenger) outgoing.getChat()).waitForConnect(4000);
        LimeTestUtils.waitForNIO();
        // wait for SwingUtilities.invokeLater() to create chat frame
        GUITestUtils.waitForSwing();
        // wait for socket disconnect
        LimeTestUtils.waitForNIO();
        assertTrue(!outgoing.getChat().isConnected());
    }

    public void testChatThroughAcceptor() throws Exception {
        setupChat();
        assertTrue(outgoing.getChat().isConnected());
        assertTrue(incoming.getChat().isConnected());
        incoming.setMessage("foo");
        assertEquals("foo", incoming.getMessage());
        incoming.send();
        Thread.sleep(1000);
        assertTrue(incoming.getText().indexOf("foo") > 0);
        assertEquals("", incoming.getMessage());
        outgoing.setMessage("bar");
        outgoing.send();
        LimeTestUtils.waitForNIO();
        GUITestUtils.waitForSwing();
        assertTrue(incoming.getText().indexOf("bar") > 0);
        assertTrue(outgoing.getText().indexOf("bar") > 0);
    }

    public void testCloseIncoming() throws Exception {
        setupChat();
        incoming.dispatchEvent(new WindowEvent(incoming,
                WindowEvent.WINDOW_CLOSING));
        GUITestUtils.waitForSwing();
        LimeTestUtils.waitForNIO();
        assertTrue(!outgoing.getChat().isConnected());
        assertTrue(!incoming.getChat().isConnected());
    }

    public void testCloseOutgoing() throws Exception {
        setupChat();
        outgoing.getChat().stop();
        LimeTestUtils.waitForNIO();
        assertTrue(!outgoing.getChat().isConnected());
        assertTrue(!incoming.getChat().isConnected());
        GUITestUtils.waitForSwing();
        assertTrue(incoming.getText().indexOf(
                GUIMediator.getStringResource("CHAT_HOST_UNAVAILABLE")) != -1);
    }

    private ChatFrame getIncomingChat() {
        for (ChatFrame frame : chatManager.getChatFrames()) {
            if (!frame.getChat().isOutgoing()) {
                return frame;
            }
        }
        fail("Incoming chat not found");
        return null;
    }

}
