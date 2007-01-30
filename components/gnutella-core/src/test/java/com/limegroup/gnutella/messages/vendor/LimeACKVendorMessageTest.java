package com.limegroup.gnutella.messages.vendor;

import java.net.InetAddress;

import org.limewire.security.QueryKey;
import org.limewire.security.SecurityToken;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;

public class LimeACKVendorMessageTest extends BaseTestCase {

    private SecurityToken securityToken;
    
    public LimeACKVendorMessageTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        securityToken = QueryKey.getQueryKey(InetAddress.getLocalHost(), 5904);
    }
    
    public void testSecurityTokenBytesAreSet() {
        LimeACKVendorMessage msg = new LimeACKVendorMessage(new GUID(), 10, securityToken);
        assertEquals(securityToken.getBytes(), msg.getSecurityTokenBytes());
    }
    
    public void testSecurityTokenBytesFromNetWork() throws BadPacketException {
        LimeACKVendorMessage in = new LimeACKVendorMessage(new GUID(), 10, securityToken);
        LimeACKVendorMessage msg = new LimeACKVendorMessage(GUID.makeGuid(), (byte)1, (byte)1, 3, in.getPayload());
        assertEquals(securityToken.getBytes(), msg.getSecurityTokenBytes());
        assertEquals(10, msg.getNumResults());
    }
    
    public void testResultNum() {
        LimeACKVendorMessage msg = new LimeACKVendorMessage(new GUID(), 10, securityToken);
        assertEquals(10, msg.getNumResults());
            
        for (int illegalNum : new int[] { 256, 0, -1 }) {
            try {
                msg = new LimeACKVendorMessage(new GUID(), illegalNum, securityToken);
                fail("Expected IllegalArgumentException for " + illegalNum);
            }
            catch (IllegalArgumentException iae) {
            }
        }
    }

    public void testInvalidPayloadLengths() {
        for (int i = 0; i < 7; i++) {
            try {
                new LimeACKVendorMessage(GUID.makeGuid(), (byte)1, (byte)1, 3, new byte[i]);
                fail("payload is too short but no exception thrown");
            } catch (BadPacketException e) {
            }
        }
    }
}
