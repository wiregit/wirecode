package com.limegroup.gnutella.filters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.Test;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests for SpamReplyFilter
 */
@SuppressWarnings( { "unchecked", "cast" } )
public class SpamReplyFilterTest extends LimeTestCase {
        
    /**
     * A non blank IP
     */
    private static final byte[] IP = new byte[] {1, 1, 1, 1};

    private final QueryReply _reply = 
         new QueryReply(GUID.makeGuid(), (byte) 1, 6346, IP, (long) 3,
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
	
    public void setUp() throws Exception {
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

    public void testReplies() throws Exception{
        assertTrue( allow("LIME"));
        assertTrue( allow("BEAR"));
        assertTrue( allow("RAZA"));
        assertTrue(!allow("MUTE"));
        assertTrue( allow("GTKG"));
        assertTrue( allow("GNUC"));
    }
    

    private boolean allow(String vendorCode) throws Exception {
        byte[] vendorBytes = vendorCode.getBytes();
        assertEquals(4, vendorBytes.length);
        for (int i = 0; i < vendorBytes.length; i++)
            _replyBytes[_indexOfVendor+i] = vendorBytes[i];
        
        QueryReply qr = 
            (QueryReply) MessageFactory.read(new ByteArrayInputStream(_replyBytes));
        return _filter.allow(qr);
    }

}
