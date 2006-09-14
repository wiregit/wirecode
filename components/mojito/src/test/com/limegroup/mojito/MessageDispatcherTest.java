package com.limegroup.mojito;

import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.messages.MessageFactory;
import com.limegroup.mojito.messages.MessageHelper;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.routing.ContactFactory;

public class MessageDispatcherTest extends BaseTestCase {
    
    public MessageDispatcherTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(MessageDispatcherTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSendMethod() throws Exception {
        MojitoDHT dht = MojitoFactory.createDHT("Test");
        try {
            dht.bind(5000);
            dht.start();
            
            boolean sent = false;
            
            Context context = (Context)dht;
            MessageHelper helper = context.getMessageHelper();
            MessageFactory factory = context.getMessageFactory();
            MessageDispatcher dispatcher = context.getMessageDispatcher();
            
            // Send to an address
            sent = dispatcher.send(new InetSocketAddress("www.google.com", 5000), 
                    helper.createPingRequest(new InetSocketAddress("www.google.com", 5000)), null);
            assertTrue(sent);
            
            // Send to local Node's contact address
            sent = dispatcher.send(context.getContactAddress(), 
                    helper.createPingRequest(context.getContactAddress()), null);
            assertFalse(sent);
            
            // Send to a non local Node
            Contact node = ContactFactory.createUnknownContact(0, 0, 
                    KUID.createRandomID(), new InetSocketAddress("www.google.com", 5000));
            sent = dispatcher.send(node, helper.createPingRequest(node.getContactAddress()), null);
            assertTrue(sent);
            
            // Sent to local Node
            sent = dispatcher.send(context.getLocalNode(), 
                    helper.createPingRequest(context.getContactAddress()), null);
            assertFalse(sent);
            
            // Send to a Node that has the local Node's ID
            node = ContactFactory.createUnknownContact(0, 0, 
                    context.getLocalNodeID(), new InetSocketAddress("www.google.com", 5000));
            sent = dispatcher.send(node, helper.createPingRequest(node.getContactAddress()), null);
            assertFalse(sent);
            
            // Sender is not firewalled
            Contact sender = ContactFactory.createLiveContact(
                    new InetSocketAddress("www.google.com", 5000), 0, 0, 
                    context.getLocalNodeID().invert(), 
                    new InetSocketAddress("www.google.com", 5000), 0, false);
            
            RequestMessage request = factory.createPingRequest(
                    sender, MessageID.create(new InetSocketAddress("www.google.com", 5000)));
            sent = dispatcher.send(node, request, null);
            assertFalse(sent);
            
            // Same as abvobe but sender is firewalled
            sender = ContactFactory.createLiveContact(
                    new InetSocketAddress("www.google.com", 5000), 0, 0, 
                    context.getLocalNodeID().invert(), 
                    new InetSocketAddress("www.google.com", 5000), 0, true);
            
            request = factory.createPingRequest(
                    sender, MessageID.create(new InetSocketAddress("www.google.com", 5000)));
            sent = dispatcher.send(node, request, null);
            assertTrue(sent);
            
        } finally {
            dht.stop();
        }
    }
}
