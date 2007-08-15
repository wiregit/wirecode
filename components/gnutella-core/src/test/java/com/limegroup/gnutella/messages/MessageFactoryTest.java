package com.limegroup.gnutella.messages;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class MessageFactoryTest extends BaseTestCase {

    public MessageFactoryTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MessageFactoryTest.class);
    }

    public void testGetParser() {
        assertNull(MessageFactory.getParser((byte) 0xFF));
        assertNotNull(MessageFactory.getParser((byte) 0x0));
        assertNotNull(MessageFactory.getParser((byte) 0x100));
    }

}
