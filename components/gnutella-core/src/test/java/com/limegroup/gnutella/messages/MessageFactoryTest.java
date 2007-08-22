package com.limegroup.gnutella.messages;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.ProviderHacks;

public class MessageFactoryTest extends BaseTestCase {

    public MessageFactoryTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MessageFactoryTest.class);
    }

    public void testGetParser() {
        assertNull(ProviderHacks.getMessageFactory().getParser((byte) 0xFF));
        assertNotNull(ProviderHacks.getMessageFactory().getParser((byte) 0x0));
        assertNotNull(ProviderHacks.getMessageFactory().getParser((byte) 0x100));
    }

}
