package com.limegroup.gnutella.messages;

import junit.framework.Test;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteOrder;

import com.limegroup.gnutella.settings.SharingSettings;

public class IntervalEncoderTest extends BaseTestCase {
    
    public IntervalEncoderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(IntervalEncoderTest.class);
    }
    
    public void testEncode() throws Exception {
        SharingSettings.MAX_PARTIAL_ENCODING_SIZE.setValue(40);
        GGEP g = new GGEP();
        IntervalSet s = new IntervalSet();
        s.add(Range.createRange(0,1024 * 1024L - 1)); // [0, 1MB]
        s.add(Range.createRange(3 * 1024L * 1024L, 5 * 1024L * 1024L * 1024L - 1)); // [3MB, 5GB]
        s.add(Range.createRange(7 * 1024L * 1024L * 1024L ,7 * 1024L * 1024L * 1024L + 1023)); // [7GB, 7GB+1KB]
        IntervalEncoder.encode(8 * 1024L * 1024L * 1024L, g, s); // 8 GB file
        
        // L1: [5, 9, 12, 17, 33, 65, 129]
        // L2: [257, 513, 1025, 2049, 8192, 8195]
        // L3: [15728640] -> the [7GB, 7GB+1KB] range
        // L4: empty
        
        assertNull(g.get("PR4"));
        
        byte [] l3 = g.get("PR3");
        assertEquals(3,l3.length);
        byte [] tmp = new byte[4];
        System.arraycopy(l3,0,tmp,1,3);
        int carried = ByteOrder.beb2int(tmp, 0);
        assertEquals(15728640,carried);
        
        byte [] l2 = g.get("PR2");
        assertEquals(12, l2.length);
        assertEquals(257, ByteOrder.beb2short(l2, 0));
        assertEquals(513, ByteOrder.beb2short(l2, 2));
        assertEquals(1025, ByteOrder.beb2short(l2, 4));
        assertEquals(2049, ByteOrder.beb2short(l2, 6));
        assertEquals(8192, ByteOrder.beb2short(l2, 8));
        assertEquals(8195, ByteOrder.beb2short(l2, 10));
        
        byte [] l1 = g.get("PR1");
        assertEquals(7, l1.length);
        assertEquals(5, l1[0]);
        assertEquals(9, l1[1]);
        assertEquals(12, l1[2]);
        assertEquals(17, l1[3]);
        assertEquals(33, l1[4]);
        assertEquals(65, l1[5]);
        assertEquals(129, l1[6] & 0xFF);
    }
    
    public void testLimitedEncode() throws Exception {
        // same as above test but the size is limited.
        // with size 20, the PR3 list will be empty.
        SharingSettings.MAX_PARTIAL_ENCODING_SIZE.setValue(20);
        
        GGEP g = new GGEP();
        IntervalSet s = new IntervalSet();
        s.add(Range.createRange(0,1024 * 1024L - 1)); // [0, 1MB]
        s.add(Range.createRange(3 * 1024L * 1024L, 5 * 1024L * 1024L * 1024L - 1)); // [3MB, 5GB]
        s.add(Range.createRange(7 * 1024L * 1024L * 1024L ,7 * 1024L * 1024L * 1024L + 1023)); // [7GB, 7GB+1KB]
        IntervalEncoder.encode(8 * 1024L * 1024L * 1024L, g, s); // 8 GB file
        
        assertNull(g.get("PR4"));
        assertNull(g.get("PR3"));
        
        byte [] l2 = g.get("PR2");
        assertEquals(12, l2.length);
        assertEquals(257, ByteOrder.beb2short(l2, 0));
        assertEquals(513, ByteOrder.beb2short(l2, 2));
        assertEquals(1025, ByteOrder.beb2short(l2, 4));
        assertEquals(2049, ByteOrder.beb2short(l2, 6));
        assertEquals(8192, ByteOrder.beb2short(l2, 8));
        assertEquals(8195, ByteOrder.beb2short(l2, 10));
        
        byte [] l1 = g.get("PR1");
        assertEquals(7, l1.length);
        assertEquals(5, l1[0]);
        assertEquals(9, l1[1]);
        assertEquals(12, l1[2]);
        assertEquals(17, l1[3]);
        assertEquals(33, l1[4]);
        assertEquals(65, l1[5]);
        assertEquals(129, l1[6] & 0xFF);
        
        // with size 10, the PR3 list will be empty and the
        // PR2 list will only have 1 element
        SharingSettings.MAX_PARTIAL_ENCODING_SIZE.setValue(10);
        g = new GGEP();
        s = new IntervalSet();
        s.add(Range.createRange(0,1024 * 1024L - 1)); // [0, 1MB]
        s.add(Range.createRange(3 * 1024L * 1024L, 5 * 1024L * 1024L * 1024L - 1)); // [3MB, 5GB]
        s.add(Range.createRange(7 * 1024L * 1024L * 1024L ,7 * 1024L * 1024L * 1024L + 1023)); // [7GB, 7GB+1KB]
        IntervalEncoder.encode(8 * 1024L * 1024L * 1024L, g, s); // 8 GB file
        
        assertNull(g.get("PR4"));
        assertNull(g.get("PR3"));
        
        l2 = g.get("PR2");
        assertEquals(2, l2.length);
        assertEquals(257, ByteOrder.beb2short(l2, 0));
        
        l1 = g.get("PR1");
        assertEquals(7, l1.length);
        assertEquals(5, l1[0]);
        assertEquals(9, l1[1]);
        assertEquals(12, l1[2]);
        assertEquals(17, l1[3]);
        assertEquals(33, l1[4]);
        assertEquals(65, l1[5]);
        assertEquals(129, l1[6] & 0xFF);
    }
    
    public void testDecode() throws Exception {
        // same examples as above test
        byte [] tmp = new byte[4];
        ByteOrder.int2beb(15728640, tmp, 0);
        byte [] l3 = new byte[3];
        System.arraycopy(tmp,1,l3,0,3);
        
        byte [] l2 = new byte[12];
        ByteOrder.short2beb((short)257, l2, 0);
        ByteOrder.short2beb((short)513, l2, 2);
        ByteOrder.short2beb((short)1025, l2, 4);
        ByteOrder.short2beb((short)2049, l2, 6);
        ByteOrder.short2beb((short)8192, l2, 8);
        ByteOrder.short2beb((short)8195, l2, 10);
        
        byte [] l1 = new byte[7];
        l1[0] = 5;
        l1[1] = 9;
        l1[2] = 12;
        l1[3] = 17;
        l1[4] = 33;
        l1[5] = 65;
        l1[6] = (byte)(129);
        
        GGEP g = new GGEP();
        g.put("PR1",l1);
        g.put("PR2",l2);
        g.put("PR3",l3);
        
        IntervalSet s = IntervalEncoder.decode(8 * 1024L * 1024L * 1024L, g);
        assertEquals(3,s.getNumberOfIntervals());
        assertTrue(s.contains(Range.createRange(0,1024 * 1024L - 1)));
        assertTrue(s.contains(Range.createRange(3 * 1024L * 1024L, 5 * 1024L * 1024L * 1024L - 1)));
        assertTrue(s.contains(Range.createRange(7 * 1024L * 1024L * 1024L ,7 * 1024L * 1024L * 1024L + 1023)));
    }

}
