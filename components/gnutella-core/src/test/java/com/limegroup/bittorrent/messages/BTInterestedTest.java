package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import junit.framework.Assert;

import org.limewire.util.BaseTestCase;

public class BTInterestedTest extends BaseTestCase {

    public BTInterestedTest(String name) {
        super(name);
    }

    public void testBTInterested() {
        BTInterested btInterested = BTInterested.createMessage();
        Assert.assertEquals(BTMessage.INTERESTED, btInterested.getType());
        ByteBuffer testPayload = ByteBuffer.allocate(0);
        Assert.assertEquals(testPayload, btInterested.getPayload());
        Assert.assertTrue(btInterested.isUrgent());

    }

}
