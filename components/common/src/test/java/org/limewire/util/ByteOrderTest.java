package org.limewire.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteOrder;


import junit.framework.Test;


/**
 * Unit tests for ByteOrder
 */
public class ByteOrderTest extends BaseTestCase {
    
    short result1, result4;
    int result2, result3;
    byte in;
    int out;
    short in2;
    int out2;
    int in3;
    long out4;
    ByteArrayInputStream sin;
    ByteArrayOutputStream sout;

    
	public ByteOrderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ByteOrderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testReverse() {
	    byte[] x1 = { (byte)1, (byte)2, (byte)3, (byte)4 };
	    byte[] x2 = ByteOrder.reverse(x1);
	    assertEquals(4, x2[0]);
	    assertEquals(3, x2[1]);
        assertEquals(2, x2[2]);
        assertEquals(1, x2[3]);
        
        byte[] x3 = new byte[0];
        byte[] x4 = ByteOrder.reverse(x3);
        assertEquals(x3, x4);
    }
	
	public void testLeb2ShortAndShort2Leb() throws Exception {
    
        byte[] x1={(byte)0x2, (byte)0x1};  //{x1[0], x1[1]}
        result1=ByteOrder.leb2short(x1,0);
        assertEquals((short)258, result1);  //256+2;
        byte[] x1b=new byte[2];
        ByteOrder.short2leb(result1, x1b, 0);
        for (int i=0; i<2; i++) 
            assertEquals(x1[i], x1b[i]);
            
        // stream version
        sin = new ByteArrayInputStream(x1);
        result1 = ByteOrder.leb2short(sin);
        assertEquals(258, result1);
        
        sout = new ByteArrayOutputStream();
        ByteOrder.short2leb(result1, sout);
        assertEquals(x1, sout.toByteArray());
        
    }
    
    public void testLeb2IntAndInt2Leb() throws Exception {
        byte[] x2={(byte)0x2, (byte)0, (byte)0, (byte)0x1};
        //2^24+2 = 16777216+2 = 16777218
        result2=ByteOrder.leb2int(x2,0);
        assertEquals(16777218, result2);

        byte[] x2b=new byte[4];
        ByteOrder.int2leb(result2, x2b, 0);
        for (int i=0; i<4; i++) 
            assertEquals(x2[i], x2b[i]);
    
        byte[] x3={(byte)0x00, (byte)0xF3, (byte)0, (byte)0xFF};
        result3=ByteOrder.leb2int(x3,0);
        assertEquals("unexpected result3 ("+Integer.toHexString(result3)+")",
            0xFF00F300, result3);
            
        // stream version.
        sin = new ByteArrayInputStream(x2);
        result2 = ByteOrder.leb2int(sin);
        assertEquals(16777218, result2);
        
        sout = new ByteArrayOutputStream();
        ByteOrder.int2leb(result3, sout);
        assertEquals(x3, sout.toByteArray());
    }
    
    public void testBeb2ShortAndShort2Beb() throws Exception {
        byte[] x2 = { (byte)0x1, (byte)0x2 };
        result1 = ByteOrder.beb2short(x2, 0);
        assertEquals(Integer.toHexString(result1), 258, result1);

        byte[] x3={(byte)0x0F, (byte)0xA3 };
        result4=ByteOrder.beb2short(x3,0);
        assertEquals("unexpected result3 ("+Integer.toHexString(result4)+")",
            0x0FA3, result4);
            
        short a = 515;
        byte[] x4 = new byte[2];
        ByteOrder.short2beb(a, x4, 0);
        assertEquals(new byte[] { 0x2, 0x3 }, x4);
        
        // stream version.
        sin = new ByteArrayInputStream(x2);
        result1 = ByteOrder.beb2short(sin);
        assertEquals(Integer.toHexString(result1), 258, result1);
        
        sout = new ByteArrayOutputStream();
        ByteOrder.short2beb(a, sout);
        assertEquals(x4, sout.toByteArray());        
    }        
    
    public void testBeb2IntAndInt2Beb() throws Exception {
        byte[] x2 = { (byte)0x1, (byte)0, (byte)0, (byte)0x2 };
         //2^24+2 = 16777216+2 = 16777218
        result2 = ByteOrder.beb2int(x2, 0);
        assertEquals(16777218, result2);

        byte[] x3={(byte)0xFF, (byte)0x01, (byte)0xF3, (byte)0x00};
        result3=ByteOrder.beb2int(x3,0);
        assertEquals("unexpected result3 ("+Integer.toHexString(result3)+")",
            0xFF01F300, result3);
        
        byte[] buf = new byte[4];
        ByteOrder.int2beb(result3, buf, 0);
        assertEquals(buf, x3);
            
        // stream version
        sin = new ByteArrayInputStream(x2);
        result2 = ByteOrder.beb2int(sin);
        assertEquals(16777218, result2);
        
        sout = new ByteArrayOutputStream();
        ByteOrder.int2beb(result3, sout);
        assertEquals(x3, sout.toByteArray());
        
        // limited version.
        byte[] x4 = { (byte)0x1, (byte)0x2, (byte)0x04, (byte)0x3 };
        int res1, res2, res3, res4;
        res1 = ByteOrder.beb2int(x4, 3, 1);
        res2 = ByteOrder.beb2int(x4, 2, 2);
        res3 = ByteOrder.beb2int(x4, 1, 3);
        res4 = ByteOrder.beb2int(x4, 0, 4);
        assertEquals(0x3, res1);
        assertEquals(0x403, res2);
        assertEquals(0x20403, res3);
        assertEquals(0x1020403, res4);
        
        try {
            ByteOrder.beb2int(x4, 3, 5);
            fail("expected exception");
        } catch(IllegalArgumentException expected) {}
        
        // limited version of stream int2beb.
        sout = new ByteArrayOutputStream();
        ByteOrder.int2beb(1, sout, 1);
        assertEquals(new byte[] { (byte)1 }, sout.toByteArray());
        sout = new ByteArrayOutputStream();
        ByteOrder.int2beb(258, sout, 2);
        assertEquals(new byte[] { (byte)0x01, (byte)0x02 }, sout.toByteArray());
        sout = new ByteArrayOutputStream();
        ByteOrder.int2beb(259, sout, 3);
        assertEquals(new byte[] { 0, (byte)0x01, (byte)0x03 },
                     sout.toByteArray());
        sout = new ByteArrayOutputStream();
        ByteOrder.int2beb(Integer.MAX_VALUE, sout, 4);
        assertEquals(
            new byte[] {(byte)0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF },
            sout.toByteArray()
        );
    }
    
    public void testLeb2Short() {
        byte[] x4={(byte)0xFF, (byte)0xF3};
        result4=ByteOrder.leb2short(x4,0);
        assertEquals("unexpected result4 ("+Integer.toHexString(result4)+")",
            (short)0xF3FF, result4);
    }
    
    public void testUByte2Int() {

        in=(byte)0xFF; //255 if unsigned, -1 if signed.
        out=in;
        assertEquals(-1, out);
        out=ByteOrder.ubyte2int(in);
        assertEquals(255, out);
        out=ByteOrder.ubyte2int((byte)1);
        assertEquals(1, out);

        in2=(short)0xFFFF;
        assertLessThan(0,in2);
        assertEquals(0x0000FFFF, ByteOrder.ushort2int(in2));
        assertGreaterThan(0, ByteOrder.ushort2int(in2));
    }
    
    public void testUBytes2Long() {
    
        in3=0xFFFFFFFF;
        assertLessThan(0, in3);
        assertEquals(0x00000000FFFFFFFFl, ByteOrder.uint2long(in3));
        assertGreaterThan(0, ByteOrder.uint2long(in3));
    }
    
    public void testLeb2ShortAndUBytes2Int() {

        byte[] buf={(byte)0xFF, (byte)0xFF};
        in2=ByteOrder.leb2short(buf,0);
        assertEquals(-1, in2);
        out2=ByteOrder.ushort2int(in2);
        assertEquals(0x0000FFFF, out2);
    }
    
    public void testLeb2IntAndUBytes2Long() {

        byte[] buf2={(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
        in3=ByteOrder.leb2int(buf2,0);
        assertEquals(-1, in3);
        out4=ByteOrder.uint2long(in3);
        assertEquals(0x00000000FFFFFFFFl, out4);
    }
    
    public void testLong2Int() {        
        assertEquals(5, ByteOrder.long2int(5l));
        assertEquals(-10, ByteOrder.long2int(-10l));
        assertEquals(0, ByteOrder.long2int(0l));
        assertEquals(0x7FFFFFFF, ByteOrder.long2int(0xABFFFFFFFFl));  //Integer.MAX_VALUE
        assertEquals(0x80000000, ByteOrder.long2int(-0xABFFFFFFFFl)); //Integer.MIN_VALUE
    }
    
    public void testLong2MinLeb() {
        try {
            ByteOrder.long2minLeb(-1);
            fail("exception should have been thrown");
        } catch(IllegalArgumentException e) {}
        
        assertEquals(ByteOrder.long2minLeb(0), new byte[] {0});
        assertEquals(ByteOrder.long2minLeb(1), new byte[] {(byte)1});
        assertEquals(ByteOrder.long2minLeb(7), new byte[] {(byte)7});
        assertEquals(ByteOrder.long2minLeb(255), new byte[] {(byte)0xFF});
        assertEquals(ByteOrder.long2minLeb(256),
            new byte[] {(byte)0, (byte)0x1});
        assertEquals(ByteOrder.long2minLeb(0x012345L),
            new byte[] {(byte)0x45, (byte)0x23, (byte)0x01});
        assertEquals(ByteOrder.long2minLeb(0x015432FDL),
            new byte[] {(byte)0xFD, (byte)0x32, (byte)0x54, (byte)0x01});
        assertEquals(ByteOrder.long2minLeb(0x5324FCAB01L),
            new byte[] {(byte)0x01, (byte)0xAB, (byte)0xFC, (byte)0x24, 
                        (byte)0x53});
        assertEquals(ByteOrder.long2minLeb(0x123456789ABCL),
            new byte[] {(byte)0xBC, (byte)0x9A, (byte)0x78, (byte)0x56,
                        (byte)0x34, (byte)0x12});
        assertEquals(ByteOrder.long2minLeb(0xFDECBA12345678L),
            new byte[] {(byte)0x78, (byte)0x56, (byte)0x34, (byte)0x12, 
                        (byte)0xBA, (byte)0xEC, (byte)0xFD});
        assertEquals(ByteOrder.long2minLeb(0x0123456789ABCDEFL),
            new byte[] {(byte)0xEF, (byte)0xCD, (byte)0xAB, (byte)0x89, 
                        (byte)0x67, (byte)0x45, (byte)0x23, (byte)0x01});
    }

    public void testInt2MinLeb() {
        try {
            ByteOrder.int2minLeb(-1);
            fail("exception should have been thrown");
        } catch (IllegalArgumentException e) { }

        assertEquals(ByteOrder.int2minLeb(0), new byte[] {(byte)0});
        assertEquals(ByteOrder.int2minLeb(1), new byte[] {(byte)1});
        assertEquals(ByteOrder.int2minLeb(7), new byte[] {(byte)7});
        assertEquals(ByteOrder.int2minLeb(255), new byte[] {(byte)0xFF});
        assertEquals(ByteOrder.int2minLeb(256),
            new byte[] {(byte)0, (byte)0x1});
        assertEquals(ByteOrder.int2minLeb(72831), 
            new byte[] {(byte)0x7f, (byte)0x1c, (byte)0x1});
        assertEquals(ByteOrder.int2minLeb(731328764), 
            new byte[] {(byte)0xFC, (byte)0x30, (byte)0x97, (byte)0x2B});
    }
    
    public void testInt2MinBeb() {
        try {
            ByteOrder.int2minBeb(-1);
            fail("expected exception");
        } catch(IllegalArgumentException expected) {}
        
        assertEquals(ByteOrder.int2minBeb(0), new byte[] {(byte)0});
        assertEquals(ByteOrder.int2minBeb(1), new byte[] {(byte)1});
        assertEquals(ByteOrder.int2minBeb(7), new byte[] {(byte)7});
        assertEquals(ByteOrder.int2minBeb(255), new byte[] {(byte)0xFF});
        assertEquals(ByteOrder.int2minBeb(256),
            new byte[] {(byte)0x1, (byte)0});
        assertEquals(ByteOrder.int2minBeb(72831), 
            new byte[] { (byte)0x1, (byte)0x1c, (byte)0x7f});
        assertEquals(ByteOrder.int2minBeb(731328764), 
            new byte[] {(byte)0x2B, (byte)0x97, (byte)0x30, (byte)0xFC});
        
    }   
    
    public void testLeb2Long() {
        assertEquals(0, ByteOrder.leb2long(new byte[] {(byte)0}, 0, 1));
        assertEquals(1, ByteOrder.leb2long(new byte[] {(byte)1}, 0, 1));
        assertEquals(7, ByteOrder.leb2long(new byte[] {(byte)7}, 0, 1));
        assertEquals(0x1c,
            ByteOrder.leb2long(new byte[] {(byte)0x7f, (byte)0x1c,
                                           (byte)0x1}, 1, 1));

        assertEquals(0x11c,
            ByteOrder.leb2long(new byte[] {(byte)0x7f, (byte)0x1c,
                                           (byte)0x1}, 1, 2));

        assertEquals(0x11c7f,
            ByteOrder.leb2long(new byte[] {(byte)0x7f, (byte)0x1c,
                                           (byte)0x1}, 0, 3));

        assertEquals(0x1011c7f,
            ByteOrder.leb2long(new byte[] {(byte)0xac, (byte)0x7f, (byte)0x1c,
                                           (byte)0x1, (byte)0x1}, 1, 4));

        try {
            ByteOrder.leb2long(new byte[] {}, 0, 0);
            fail("illegalargument expected.");
        } catch(IllegalArgumentException ignored) {}

        try {
            ByteOrder.leb2long(new byte[] {}, 0, 9);
            fail("illegalargument expected.");
        } catch(IllegalArgumentException ignored) {}            
    }
    
    public void testLeb2Int() {
        assertEquals(0, ByteOrder.leb2int(new byte[] {(byte)0}, 0, 1));
        assertEquals(1, ByteOrder.leb2int(new byte[] {(byte)1}, 0, 1));
        assertEquals(7, ByteOrder.leb2int(new byte[] {(byte)7}, 0, 1));
        assertEquals(0x1c,
        ByteOrder.leb2int(new byte[] {(byte)0x7f, (byte)0x1c, (byte)0x1}, 1, 1));
        assertEquals(0x11c,
        ByteOrder.leb2int(new byte[] {(byte)0x7f, (byte)0x1c, (byte)0x1}, 1, 2));
        assertEquals(0x11c7f,
        ByteOrder.leb2int(new byte[] {(byte)0x7f, (byte)0x1c, (byte)0x1}, 0, 3));
        assertEquals(0x1011c7f,
        ByteOrder.leb2int(new byte[] {(byte)0xac, (byte)0x7f, (byte)0x1c,
                                      (byte)0x1, (byte)0x1}, 1, 4));
        
        try {
            ByteOrder.leb2int(new byte[] {}, 0, 0);
            fail("illegalargument expected.");
        } catch(IllegalArgumentException ignored) {}
        
        try {
            ByteOrder.leb2int(new byte[] {}, 0, 5);
            fail("illegalargument expected.");
        } catch(IllegalArgumentException ignored) {}            
    }    
}        