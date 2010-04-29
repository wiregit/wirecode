package org.limewire.mojito.util;

import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito2.DHT;
import org.limewire.mojito2.MojitoFactory2;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.DefaultMessageFactory;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.message.PingRequest;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.util.ContactUtils;
import org.limewire.mojito2.util.MessageUtils;
import org.limewire.mojito2.util.NopTransport;

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
    
    public void testIsCollisionPing() {
        Transport transport = NopTransport.NOP;
        MessageFactory factory = new DefaultMessageFactory();
        
        DHT dht = MojitoFactory2.createDHT(transport, factory);
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
