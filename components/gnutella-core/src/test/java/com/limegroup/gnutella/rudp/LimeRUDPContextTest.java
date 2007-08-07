package com.limegroup.gnutella.rudp;

import junit.framework.Test;

import org.limewire.nio.NIODispatcher;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.messages.RUDPMessage;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageFactory;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageParser;
import com.limegroup.gnutella.util.LimeTestCase;

public class LimeRUDPContextTest extends LimeTestCase {
    
    public LimeRUDPContextTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LimeRUDPContextTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testReferences() {
        // Make sure there's nothing linked up to RUDP beforehand
        assertNull(MessageFactory.getParser(RUDPMessage.F_RUDP_MESSAGE));
        RUDPContext context = ProviderHacks.getRUDPContext();
        assertEquals(LimeRUDPMessageFactory.class, context.getMessageFactory().getClass());
        assertEquals(LimeUDPService.class, context.getUDPService().getClass());
        assertEquals(LimeRUDPSettings.class, context.getRUDPSettings().getClass());
        assertSame(NIODispatcher.instance().getTransportListener(), context.getTransportListener());
        // make sure the parser was installed in the MessageFactory
        assertNotNull(MessageFactory.getParser(RUDPMessage.F_RUDP_MESSAGE));
        assertEquals(LimeRUDPMessageParser.class, MessageFactory.getParser(RUDPMessage.F_RUDP_MESSAGE).getClass());
    }
}
