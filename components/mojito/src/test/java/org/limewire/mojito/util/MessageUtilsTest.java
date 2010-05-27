package org.limewire.mojito.util;

import java.io.IOException;
import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.DHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.message.PingRequest;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.mojito.util.MessageUtils;
import org.limewire.mojito.util.NopTransport;

public class MessageUtilsTest extends MojitoTestCase {
    
    public MessageUtilsTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(MessageUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testIsCollisionPing() throws IOException {
        DHT dht = MojitoFactory.createDHT("DHT-1");
        dht.bind(NopTransport.NOP);
        
        MessageFactory factory = dht.getMessageFactory();
        Contact localhost = dht.getLocalNode();
        
        PingRequest ping = null;
        
        assertFalse(ContactUtils.isCollisionPingSender(localhost.getNodeID(), dht.getLocalNode()));
        ping = factory.createPingRequest(dht.getLocalNode(), new InetSocketAddress("localhost", 2000));
        assertFalse(MessageUtils.isCollisionPingRequest(localhost.getNodeID(), ping));
        
        Contact sender = ContactUtils.createCollisionPingSender(dht.getLocalNode());
        assertTrue(ContactUtils.isCollisionPingSender(localhost.getNodeID(), sender));
        ping = factory.createPingRequest(sender, new InetSocketAddress("localhost", 2000));
        assertTrue(MessageUtils.isCollisionPingRequest(localhost.getNodeID(), ping));
    }
}
