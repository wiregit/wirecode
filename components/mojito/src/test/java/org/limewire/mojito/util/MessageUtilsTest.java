package org.limewire.mojito.util;

import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.MojitoDHT2;
import org.limewire.mojito.MojitoFactory2;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.message2.DefaultMessageFactory;
import org.limewire.mojito.message2.MessageFactory;
import org.limewire.mojito.message2.PingRequest;
import org.limewire.mojito.routing.Contact;

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
        
        MojitoDHT2 dht = MojitoFactory2.createDHT(transport, factory);
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
