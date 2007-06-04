package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;

import junit.framework.Test;

import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;

public class HeadPingTest extends LimeTestCase {
    
    public HeadPingTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HeadPingTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testVersion1PingIsNotPongGGEPCapable() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0);
        out.write(HugeTestUtils.SHA1.httpStringValue().getBytes());
        
        HeadPing ping = new HeadPing(new byte[16], (byte)0, (byte)0, 1, out.toByteArray());
        assertFalse(ping.isPongGGEPCapable());
    }
    
    public void testVersion2PingIsPongGGEPCapable() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0);
        out.write(HugeTestUtils.SHA1.httpStringValue().getBytes());
        
        HeadPing ping = new HeadPing(new byte[16], (byte)0, (byte)0, 2, out.toByteArray());
        assertTrue(ping.isPongGGEPCapable());
    }

}
