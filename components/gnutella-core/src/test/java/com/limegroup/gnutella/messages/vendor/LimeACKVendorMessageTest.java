package com.limegroup.gnutella.messages.vendor;

import java.net.InetAddress;

import org.limewire.security.QueryKey;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;

public class LimeACKVendorMessageTest extends BaseTestCase {

    private QueryKey queryKey;
    
    public LimeACKVendorMessageTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        queryKey = QueryKey.getQueryKey(InetAddress.getLocalHost(), 5904);
    }
    
    public void testQueryKeyIsSet() {
        LimeACKVendorMessage msg = new LimeACKVendorMessage(new GUID(), 10, queryKey);
        assertEquals(queryKey.getBytes(), msg.getSecurityTokenBytes());
    }
    
    public void testQueryKeyFromNetWork() throws BadPacketException {
        LimeACKVendorMessage in = new LimeACKVendorMessage(new GUID(), 10, queryKey);
        LimeACKVendorMessage msg = new LimeACKVendorMessage(GUID.makeGuid(), (byte)1, (byte)1, 3, in.getPayload());
        assertEquals(queryKey.getBytes(), msg.getSecurityTokenBytes());
        assertEquals(10, msg.getNumResults());
    }
    
    public void testResultNum() {
        LimeACKVendorMessage msg = new LimeACKVendorMessage(new GUID(), 10, queryKey);
        assertEquals(10, msg.getNumResults());
            
        for (int illegalNum : new int[] { 256, 0, -1 }) {
            try {
                msg = new LimeACKVendorMessage(new GUID(), illegalNum, queryKey);
                fail("Expected IllegalArgumentException for " + illegalNum);
            }
            catch (IllegalArgumentException iae) {
            }
        }
    }

}
