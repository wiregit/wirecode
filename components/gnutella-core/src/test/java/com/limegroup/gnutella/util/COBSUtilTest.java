package com.limegroup.gnutella.util;

import junit.framework.*;
import com.sun.java.util.collections.*;

/**
 * Tests COBSUtil
 */
public class COBSUtilTest extends TestCase {
    public COBSUtilTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(COBSUtilTest.class);
    }  

    public void testEncode() throws java.io.IOException {
        for (int num = 1; num < 512; num++) {
            System.out.println("num = " + num);
        // test all 0s...
        byte[] bytes = new byte[num];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = 0;
        byte[] after = COBSUtil.cobsEncode(bytes);
        assertTrue(bytes.length == (after.length-1));
        for (int i = 0; i < after.length; i++)
            assertTrue(after[i] == 0x01);
        
        // ----------------------------------
        // build up 'induction' case for 0(1..).  we can trust 'induction' due
        // to nature of the COBS algorithm...

        // test 0 and 1s (01010101010....)
        for (int i = 0; i < bytes.length; i++) 
            if (i % 2 == 0)
                bytes[i] = 0;
            else
                bytes[i] = 1;
        after = COBSUtil.cobsEncode(bytes);
        assertTrue(bytes.length == (after.length-1));
        for (int i = 0; i < after.length; i++)
            if (i % 2 == 0)
                after[i] = 2;
            else
                after[i] = 1;

        // test 011011011....
        for (int i = 0; i < bytes.length; i++) 
            if (i % 3 == 0)
                bytes[i] = 0;
            else
                bytes[i] = 1;
        after = COBSUtil.cobsEncode(bytes);
        assertTrue(bytes.length == (after.length-1));
        for (int i = 0; i < after.length; i++)
            if (i % 3 == 0)
                after[i] = 3;
            else
                after[i] = 1;

        // test 0(1^254)0(1^254)....
        for (int i = 0; i < bytes.length; i++) 
            if (i % 255 == 0)
                bytes[i] = 0;
            else
                bytes[i] = 1;
        after = COBSUtil.cobsEncode(bytes);
        assertTrue(bytes.length == (after.length-1));
        for (int i = 0; i < after.length; i++)
            if (i % 255 == 0)
                after[i] = (byte) 0xFF;
            else
                after[i] = 1;
        // ----------------------------------

        // ----------------------------------
        // build up 'induction' case for (1..)0.  we can trust 'induction' due
        // to nature of the COBS algorithm...

        // test 0 and 1s (1010101010....)
        for (int i = 0; i < bytes.length; i++) 
            if (i % 2 == 0)
                bytes[i] = 1;
            else
                bytes[i] = 0;
        after = COBSUtil.cobsEncode(bytes);
        assertTrue(bytes.length == (after.length - 1));
        for (int i = 0; i < bytes.length; i++)
            if (i % 2 == 0)
                after[i] = 2;
            else
                after[i] = 1;
        assertTrue(after[num] == 1);

        // test 11011011....
        for (int i = 0; i < bytes.length; i++) 
            if ((i + 1) % 3 == 0)
                bytes[i] = 0;
            else
                bytes[i] = 1;
        after = COBSUtil.cobsEncode(bytes);
        assertTrue(bytes.length == (after.length - 1));
        for (int i = 0; i < bytes.length; i++)
            if ((i + 1) % 3 == 0)
                after[i] = 3;
            else
                after[i] = 1;
        assertTrue(after[num] == 1);        

        // test (1^254)0(1^254)0....
        for (int i = 0; i < bytes.length; i++) 
            if ((i+1) % 255 == 0)
                bytes[i] = 0;
            else
                bytes[i] = 1;
        after = COBSUtil.cobsEncode(bytes);
        assertTrue(bytes.length == (after.length-1));
        for (int i = 0; i < after.length; i++)
            if ((i +1) % 255 == 0)
                after[i] = (byte) 0xFF;
            else
                after[i] = 1;
        assertTrue(after[num] == 1);        
        // ----------------------------------
        }        
        

        
    }

    public void testDecode() {
    }

    public void testSymmetry() throws java.io.IOException {
        byte[] bytes = (new String("Sush Is Cool!")).getBytes();
        byte[] after = COBSUtil.cobsDecode(COBSUtil.cobsEncode(bytes));
        assertTrue(bytes.length == (after.length - 1));
        byte[] afterTrimmed = (new String(after)).trim().getBytes();
        assertTrue(Arrays.equals(bytes, afterTrimmed));
    }


    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }


}
