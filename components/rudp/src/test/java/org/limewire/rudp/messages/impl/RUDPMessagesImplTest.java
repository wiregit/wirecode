package org.limewire.rudp.messages.impl;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import junit.framework.Test;

import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.util.Base32;
import org.limewire.util.BaseTestCase;

public class RUDPMessagesImplTest extends BaseTestCase {
    
    public RUDPMessagesImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RUDPMessagesImplTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testMessageFormat() throws Exception {
        // 3, 5, 8, 3
        byte[] ackData = Base32.decode("AMKAABIABAAAGAAAAAAAAAAAABAQCAAAAAAAA");
        // data for dataData
        byte[] bufferData = Base32.decode("VNMMPZX6DDIHKZ6UHU34ZYHHVK7TO2FQVRL755B2WS6SSOSQFSALBLJKHCNSJ2WCEKNPGDEGWIBVNQFKZWYKMBW753ERHQXZPJW6N7LAOJA2EIJQZ3CY3OX6XKZ62CXFAYK5G6Y4222UK43TREZYOGNR7RCDHNPH443Q");
        // 5, 2, bufferData
        byte[] dataData = Base32.decode("AU6AAATIMTSUTKDECK2ETNFLOZAQCAC2AAAABLLFW2JG2YRVSOOE3A32WQL5BYNYM6KNOR2V7OTEHVXXYVBYTCERJFPFWVKDQYHAY4AMVZTJZLGMQWD3XCECPAZK6TXB43NKWPHAPXP4TFLQ43RVQXYXSBRF4KBOGLQVIA7XYLP5HRXXJVE2C");
        // 254, 21, 5
        byte[] finData = Base32.decode("7ZAQAFIFAAAAAAAAAAAAAAAAABAQCAAAAAAAA");
        // 203, 5810, 52
        byte[] keepAliveData = Base32.decode("ZMSAAAAWWIADIAAAAAAAAAAAABAQCAAAAAAAA");
        // 123, 153
        byte[] synData = Base32.decode("TEBQAAD3AAAAAAAAAAAAAAAAABAQCAAAAAAAA");
        
        RUDPMessageFactory factory = new DefaultMessageFactory();
        
        checkMessage(factory.createAckMessage((byte)3, 5, 8, 3),
                     factory.createMessage(ByteBuffer.wrap(ackData)));
        
        checkMessage(factory.createDataMessage((byte)5, 2, ByteBuffer.wrap(bufferData)),
                     factory.createMessage(ByteBuffer.wrap(dataData)));
        
        checkMessage(factory.createFinMessage((byte)254, 21, (byte)5),
                     factory.createMessage(ByteBuffer.wrap(finData)));
        
        checkMessage(factory.createKeepAliveMessage((byte)203, 5810, 52),
                     factory.createMessage(ByteBuffer.wrap(keepAliveData)));
        
        checkMessage(factory.createSynMessage((byte)123, (byte)153),
                     factory.createMessage(ByteBuffer.wrap(synData)));
    }
    
    private void checkMessage(RUDPMessage a, RUDPMessage b) throws Exception {
        assertEquals(b.getClass(), a.getClass());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        a.write(out);
        byte[] ba = out.toByteArray();
        out.reset();
        b.write(out);
        byte[] bb = out.toByteArray();
        assertEquals(bb.length, ba.length);
        assertEquals(bb, ba);
    }
}
