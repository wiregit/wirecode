package com.limegroup.gnutella;

import junit.framework.*;
import com.limegroup.gnutella.util.BaseTestCase;
import com.sun.java.util.collections.*;

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

    
	public ByteOrderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ByteOrderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLeb2ShortAndShort2Leb() {
    
        byte[] x1={(byte)0x2, (byte)0x1};  //{x1[0], x1[1]}
        result1=ByteOrder.leb2short(x1,0);
        assertEquals((short)258, result1);  //256+2;
        byte[] x1b=new byte[2];
        ByteOrder.short2leb(result1, x1b, 0);
        for (int i=0; i<2; i++) 
            assertEquals(x1[i], x1b[i]);
    }
    
    public void testLeb2IntAndInt2Leb() {
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
    }
    
    public void testBeb2Int() {
        byte[] x2 = { (byte)0x1, (byte)0, (byte)0, (byte)0x2 };
         //2^24+2 = 16777216+2 = 16777218
        result2 = ByteOrder.beb2int(x2, 0);
        assertEquals(16777218, result2);

        byte[] x3={(byte)0xFF, (byte)0, (byte)0xF3, (byte)0x00};
        result3=ByteOrder.beb2int(x3,0);
        assertEquals("unexpected result3 ("+Integer.toHexString(result3)+")",
            0xFF00F300, result3);

    }
    
    public void testLeb2Short() {

        byte[] x4={(byte)0xFF, (byte)0xF3};
        result4=ByteOrder.leb2short(x4,0);
        assertEquals("unexpected result4 ("+Integer.toHexString(result4)+")",
            (short)0xF3FF, result4);
    }
    
    public void testUByte2Int() {

        in=(byte)0xFF; //255 if unsigned, -1 if signed.
        out=(int)in;
        assertEquals(-1, out);
        out=ByteOrder.ubyte2int(in);
        assertEquals(255, out);
        out=ByteOrder.ubyte2int((byte)1);
        assertEquals(1, out);

        in2=(short)0xFFFF;
        assertLessThan(0,in2);
        assertEquals(0x0000FFFF, ByteOrder.ubytes2int(in2));
        assertGreaterThan(0, ByteOrder.ubytes2int(in2));
    }
    
    public void testUBytes2Long() {
    
        in3=(int)0xFFFFFFFF;
        assertLessThan(0, in3);
        assertEquals(0x00000000FFFFFFFFl, ByteOrder.ubytes2long(in3));
        assertGreaterThan(0, ByteOrder.ubytes2long(in3));
    }
    
    public void testLeb2ShortAndUBytes2Int() {

        byte[] buf={(byte)0xFF, (byte)0xFF};
        in2=ByteOrder.leb2short(buf,0);
        assertEquals(-1, in2);
        out2=ByteOrder.ubytes2int(in2);
        assertEquals(0x0000FFFF, out2);
    }
    
    public void testLeb2IntAndUBytes2Long() {

        byte[] buf2={(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
        in3=ByteOrder.leb2int(buf2,0);
        assertEquals(-1, in3);
        out4=ByteOrder.ubytes2long(in3);
        assertEquals(0x00000000FFFFFFFFl, out4);
    }
    
    public void testLong2Int() {        
        assertEquals(5, ByteOrder.long2int(5l));
        assertEquals(-10, ByteOrder.long2int(-10l));
        assertEquals(0, ByteOrder.long2int(0l));
        assertEquals(0x7FFFFFFF, ByteOrder.long2int(0xABFFFFFFFFl));  //Integer.MAX_VALUE
        assertEquals(0x80000000, ByteOrder.long2int(-0xABFFFFFFFFl)); //Integer.MIN_VALUE
    }

    public void testInt2MinLeb() {
        try {
            ByteOrder.int2minLeb(-1);
            fail("exception should have been thrown");
        } catch (IllegalArgumentException e) { }

        assertTrue(Arrays.equals(ByteOrder.int2minLeb(0), new byte[] {(byte)0}));
        assertTrue(Arrays.equals(ByteOrder.int2minLeb(1), new byte[] {(byte)1}));
        assertTrue(Arrays.equals(ByteOrder.int2minLeb(7), new byte[] {(byte)7}));
        assertTrue(Arrays.equals(ByteOrder.int2minLeb(72831), 
            new byte[] {(byte)0x7f, (byte)0x1c, (byte)0x1}));
        assertTrue(Arrays.equals(ByteOrder.int2minLeb(731328764), 
            new byte[] {(byte)0xFC, (byte)0x30, (byte)0x97, (byte)0x2B}));
    }
    
    public void testLeb2Int() {
          assertEquals(0, ByteOrder.leb2int(new byte[] {(byte)0}, 0, 1));
          assertEquals(1, ByteOrder.leb2int(new byte[] {(byte)1}, 0, 1));
          assertEquals(7, ByteOrder.leb2int(new byte[] {(byte)7}, 0, 1));
          assertEquals(0x11c7f,
            ByteOrder.leb2int(new byte[] {(byte)0x7f, (byte)0x1c, (byte)0x1}, 0, 3));
          assertEquals(0x1c,
            ByteOrder.leb2int(new byte[] {(byte)0x7f, (byte)0x1c, (byte)0x1}, 1, 1));
          //TODO: expand tests to cover exceptional cases and negative values
   }    
}        