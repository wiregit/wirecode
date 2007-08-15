package com.limegroup.gnutella.rudp.messages;

import org.limewire.rudp.messages.AckMessage;
import org.limewire.rudp.messages.DataMessage;
import org.limewire.rudp.messages.FinMessage;
import org.limewire.rudp.messages.KeepAliveMessage;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.SynMessage;
import org.limewire.rudp.messages.RUDPMessage.OpCode;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.rudp.LimeRUDPContext;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageFactory;
import com.limegroup.gnutella.util.LimeTestCase;

public class LimeRUDPMessageFactoryTest extends LimeTestCase {

    public LimeRUDPMessageFactoryTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LimeRUDPMessageFactoryTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testContextUsesCorrectFactoryAndDelegate() {
        RUDPMessageFactory f = new LimeRUDPContext().getMessageFactory();
        assertEquals(LimeRUDPMessageFactory.class, f.getClass());
        assertEquals(DefaultMessageFactory.class, ((LimeRUDPMessageFactory)f).getDelegate().getClass());
    }
    
    public void testCreateLocalMessages() {
        LimeRUDPMessageFactory f = new LimeRUDPMessageFactory(new StubRUDPMessageFactory());
        checkMessage(f.createAckMessage((byte)1, 1, 1, 1), LimeAckMessageImpl.class);
        checkMessage(f.createDataMessage((byte)1, 1, null), LimeDataMessageImpl.class);
        checkMessage(f.createFinMessage((byte)1, 1, (byte)1), LimeFinMessageImpl.class);
        checkMessage(f.createKeepAliveMessage((byte)1, 1, 1), LimeKeepAliveMessageImpl.class);
        checkMessage(f.createSynMessage((byte)1), LimeSynMessageImpl.class);
        checkMessage(f.createSynMessage((byte)1, (byte)1), LimeSynMessageImpl.class);
    }
    
    public void testCreateNetworkMessages() throws Exception {
        StubRUDPMessageFactory s = new StubRUDPMessageFactory();
        LimeRUDPMessageFactory f = new LimeRUDPMessageFactory(s);
        
        s.setNextMessageToCreate(OpCode.OP_ACK);
        checkMessage(f.createMessage(), LimeAckMessageImpl.class, AckMessage.class);
        s.setNextMessageToCreate(OpCode.OP_DATA);
        checkMessage(f.createMessage(), LimeDataMessageImpl.class, DataMessage.class);
        s.setNextMessageToCreate(OpCode.OP_FIN);
        checkMessage(f.createMessage(), LimeFinMessageImpl.class, FinMessage.class);
        s.setNextMessageToCreate(OpCode.OP_KEEPALIVE);
        checkMessage(f.createMessage(), LimeKeepAliveMessageImpl.class, KeepAliveMessage.class);
        s.setNextMessageToCreate(OpCode.OP_SYN);
        checkMessage(f.createMessage(), LimeSynMessageImpl.class, SynMessage.class);
        
        s.setNextMessageToCreate(null);
        try {
            f.createMessage();
            fail("expected exception");
        } catch(IllegalArgumentException iae) {
            assertEquals(StubRUDPMessage.class + " is unhandled", iae.getMessage());
        }
    }
    
    private RUDPMessage checkMessage(RUDPMessage m, Class... clazz) {
        assertInstanceof(Message.class, m);
        assertInstanceof(AbstractLimeRUDPMessage.class, m);
        for(int i = 0; i < clazz.length; i++)
            assertInstanceof(clazz[i], m);
        return m;
    }
}
