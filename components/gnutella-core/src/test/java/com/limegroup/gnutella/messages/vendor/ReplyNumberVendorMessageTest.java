package com.limegroup.gnutella.messages.vendor;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;

public class ReplyNumberVendorMessageTest extends BaseTestCase {

    public ReplyNumberVendorMessageTest(String name) {
        super(name);
    }
    
    public void testNumResults() {
        ReplyNumberVendorMessage msg = ReplyNumberVendorMessage.createV3ReplyNumberVendorMessage(new GUID(), 10);
        assertEquals(10, msg.getNumResults());
        
        for (int illegalResultNum : new int[] { 256, 0, -1 }) {
            try { 
                msg = ReplyNumberVendorMessage.createV3ReplyNumberVendorMessage(new GUID(), illegalResultNum);
                fail("Expected illegal argument exception for result: " + illegalResultNum);
            }
            catch (IllegalArgumentException iae) {
            }
        }
    }
    
    public void testVersion3AllowsLargerMessagesFromNetwork() throws BadPacketException {
        new ReplyNumberVendorMessage(GUID.makeGuid(), (byte)1, (byte)1, 3, new byte[11]);
        try {
            new ReplyNumberVendorMessage(GUID.makeGuid(), (byte)1, (byte)1, 2, new byte[11]);
            fail("BadPacketException expected, message too large");
        }
        catch (BadPacketException e) {
        }
    }
    
    public void testOldVersionIsNotAcceptedFromNetwork() {
        try {
            new ReplyNumberVendorMessage(GUID.makeGuid(), (byte)0x01, (byte)0x01, 2, new byte[2]);
            fail("Old message versions should not be accpeted");            
        }
        catch (BadPacketException bpe) {
        }
        try {
            new ReplyNumberVendorMessage(GUID.makeGuid(), (byte)0x01, (byte)0x01, 1, new byte[2]);
            fail("Old message versions should not be accpeted");            
        }
        catch (BadPacketException bpe) {
        }
    }

}
