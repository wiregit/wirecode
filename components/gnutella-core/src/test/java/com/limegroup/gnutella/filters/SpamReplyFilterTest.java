package com.limegroup.gnutella.filters;

import junit.framework.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.*;
import java.io.*;

/**
 * Unit tests for SpamReplyFilter
 */
public class SpamReplyFilterTest extends BaseTestCase {
        
    private final QueryReply _reply = 
         new QueryReply(GUID.makeGuid(), (byte) 1, 6346, new byte[4], (long) 3,
                        new Response[] { new Response((long) 2, (long) 2,
                                                      "Susheel") },
                        GUID.makeGuid(), true, true, true, true, true, false);

    private byte[] _replyBytes = null;
    private int _indexOfVendor = -1;
    private SpamReplyFilter _filter = new SpamReplyFilter();

	public SpamReplyFilterTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(SpamReplyFilterTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
    public void setUp() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            _reply.write(baos);
            _replyBytes = baos.toByteArray();
            boolean notFound = true;
            int index = 0;
            while (notFound && ((index+3) < _replyBytes.length)) {
                if ((_replyBytes[index+0] == (byte) 76) &&
                    (_replyBytes[index+1] == (byte) 73) &&
                    (_replyBytes[index+2] == (byte) 77) &&
                    (_replyBytes[index+3] == (byte) 69)) {
                    notFound = false;
                    _indexOfVendor = index;
                }
                index++;
            }
            // this should NEVER happen
            assertTrue(!notFound);
        }
        catch (IOException damn) {
            assertTrue(false);
        }
    }

    public void testReplies() {
        assertTrue( allow("LIME"));
        assertTrue( allow("BEAR"));
        assertTrue( allow("RAZA"));
        assertTrue(!allow("MUTE"));
        assertTrue( allow("GTKG"));
        assertTrue( allow("GNUC"));
    }
    

    private boolean allow(String vendorCode) {
        byte[] vendorBytes = vendorCode.getBytes();
        assertTrue(vendorBytes.length == 4);
        for (int i = 0; i < vendorBytes.length; i++)
            _replyBytes[_indexOfVendor+i] = vendorBytes[i];
        try {
            QueryReply qr = 
                (QueryReply) Message.read(new ByteArrayInputStream(_replyBytes));
            return _filter.allow(qr);
        }
        catch (Exception why) {
            assertTrue(false);
        }
        return false;
    }

}
