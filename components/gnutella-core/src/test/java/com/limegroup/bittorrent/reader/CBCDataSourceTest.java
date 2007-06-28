package com.limegroup.bittorrent.reader;

import java.nio.ByteBuffer;

import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.CircularByteBuffer;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

public class CBCDataSourceTest extends LimeTestCase {

    public CBCDataSourceTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CBCDataSourceTest.class);
    }
    
    public void test32BitUnsigned() throws Exception {
        ByteBufferCache bbc = new ByteBufferCache();
        CircularByteBuffer cbb = new CircularByteBuffer(4, bbc);
        CBCDataSource source = new CBCDataSource(cbb);
        
        // write a small integer
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.asIntBuffer().put(5);
        cbb.put(buf);
        
        assertEquals(5, source.getInt());
        
        // try a negative integer
        buf.clear();
        buf.asIntBuffer().put(Integer.MAX_VALUE + 100);
        cbb.put(buf);
        
        // comes out as positive long
        assertEquals(100L+Integer.MAX_VALUE, source.getInt());
                
    }
}
