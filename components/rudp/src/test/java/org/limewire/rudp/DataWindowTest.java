package org.limewire.rudp;

import junit.framework.TestCase;

import com.limegroup.gnutella.rudp.messages.StubDataMessage;

public class DataWindowTest extends TestCase {

    public void testAddData() {
        DataWindow window = new DataWindow(20, 0);
        assertFalse(window.hasReadableData());
        
        StubDataMessage msg1 = new StubDataMessage();
        msg1.setSequenceNumber(0);
        DataRecord rec = window.addData(msg1);
        assertTrue(window.hasReadableData());
        assertSame(msg1, rec.msg);
        
        StubDataMessage msg2 = new StubDataMessage();
        msg2.setSequenceNumber(0);
        rec = window.addData(msg2);
        assertTrue(window.hasReadableData());
        assertSame(msg1, rec.msg);
    }

}
