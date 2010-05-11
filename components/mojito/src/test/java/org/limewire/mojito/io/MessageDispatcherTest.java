package org.limewire.mojito.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import junit.framework.TestSuite;

import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.MojitoFactory;
import org.limewire.mojito2.io.MessageDispatcher;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.message.MessageHelper;
import org.limewire.mojito2.message.RequestMessage;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.ContactFactory;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.util.IoUtils;


public class MessageDispatcherTest extends MojitoTestCase {
    
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
        
        MojitoDHT dht = null;
        try {
            
            dht = MojitoFactory.createDHT("Test", 5000);
            
            Context context = dht.getContext();
            MessageHelper helper = context.getMessageHelper();
            MessageFactory factory = context.getMessageFactory();
            MessageDispatcher dispatcher = context.getMessageDispatcher();
            
            // Send to an address
            dispatcher.send(null, null, new InetSocketAddress("www.google.com", 5000),
                    helper.createPingRequest(new InetSocketAddress("www.google.com", 5000)), 1, TimeUnit.SECONDS);
            
            // Send to local Node's contact address
            try {
                dispatcher.send(null, null, context.getContactAddress(), 
                        helper.createPingRequest(context.getContactAddress()), 1, TimeUnit.SECONDS);
                fail("Should have failed!");
            } catch (IOException expected) {
            }
            
            // Send to a non local Node
            Contact node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(), new InetSocketAddress("www.google.com", 5000));
            dispatcher.send(null, node, helper.createPingRequest(node.getContactAddress()), 1, TimeUnit.SECONDS);
            //assertTrue(sent);
            
            // Sent to local Node
            try {
                dispatcher.send(null, context.getLocalNode(), 
                        helper.createPingRequest(context.getContactAddress()), 1, TimeUnit.SECONDS);
                fail("Should have failed!");
            } catch (IOException expected) {
            }
            
            // Send to a Node that has the local Node's ID
            try {
                node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                        context.getLocalNodeID(), new InetSocketAddress("www.google.com", 5000));
                dispatcher.send(null, node, helper.createPingRequest(node.getContactAddress()), 1, TimeUnit.SECONDS);
                fail("Should have failed!");
            } catch (IOException expected) {
            }
            
            // Sender is not firewalled
            try {
                Contact sender = ContactFactory.createLiveContact(
                        new InetSocketAddress("www.google.com", 5000), Vendor.UNKNOWN, Version.ZERO, 
                        context.getLocalNodeID().invert(), 
                        new InetSocketAddress("www.google.com", 5000), 0, Contact.DEFAULT_FLAG);
                
                RequestMessage request = factory.createPingRequest(
                        sender, new InetSocketAddress("www.google.com", 5000));
                dispatcher.send(null, node, request, 1, TimeUnit.SECONDS);
                fail("Should have failed!");
            } catch (IOException expected) {
            }
            
            // Same as above but sender is firewalled
            Contact sender = ContactFactory.createLiveContact(
                    new InetSocketAddress("www.google.com", 5000), Vendor.UNKNOWN, Version.ZERO, 
                    context.getLocalNodeID().invert(), 
                    new InetSocketAddress("www.google.com", 5000), 0, Contact.FIREWALLED_FLAG);
            
            RequestMessage request = factory.createPingRequest(
                    sender, new InetSocketAddress("www.google.com", 5000));
            dispatcher.send(null, node, request, 1, TimeUnit.SECONDS);
            
        } finally {
            IoUtils.close(dht);
        }
    }
}
