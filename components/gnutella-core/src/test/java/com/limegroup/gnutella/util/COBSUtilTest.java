package com.limegroup.gnutella.util;

import junit.framework.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.ByteOrder;

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


    public void testEncodeAndDecode() throws java.io.IOException {
        for (int num = 1; num < 260; num++) 
            encodeAndDecode(num);
    }
    

    private void encodeAndDecode(int num) throws java.io.IOException {
        // test all 0s...
        debug("COBSUtilTest.encode(): num = " +
              num);
        byte[] bytes = new byte[num];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = 0;
        byte[] after = COBSUtil.cobsEncode(bytes);
        assertTrue(bytes.length == (after.length-1));
        for (int i = 0; i < after.length; i++)
            assertTrue(after[i] == 0x01);
        byte[] decoded = COBSUtil.cobsDecode(after);
        for (int i = 0; i < bytes.length; i++)
            assertTrue(bytes[i] == decoded[i]);
        assertTrue(decoded[bytes.length] == 0);


        // test all 1s....
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = 1;
        after = COBSUtil.cobsEncode(bytes);
        assertTrue("bytes.length = " + bytes.length + ", after.length + " +
                   after.length + ", num = " + num,
                   bytes.length == (after.length-((num / 254) + 1)));
        for (int i = 1; i < after.length; i++) 
            if (i % 255 != 0)
                assertTrue(after[i] == 0x01);
        assertTrue("after[0] = " + after[0], 
                   (ByteOrder.ubyte2int(after[0]) == (num+1)) || 
                   (ByteOrder.ubyte2int(after[0]) == 255)
                   );
        decoded = COBSUtil.cobsDecode(after);
        for (int i = 0; i < bytes.length; i++)
            assertTrue("num = " + num + ", i = " + i, 
                       bytes[i] == decoded[i]);
        assertTrue(decoded[bytes.length] == 0);
        
        // ----------------------------------
        // build up 'induction' case for 0(1..).  we can trust 'induction' due
        // to nature of the COBS algorithm...

        // test 0 and 1s, specifically 0(1)^(j-1)s....
        for (int j = 2; (j < 255) && (num > 1); j++) { 
            debug("COBSUtilTest.encode(): j = " +
                               j);
            for (int i = 0; i < bytes.length; i++) 
                if (i % j == 0)
                    bytes[i] = 0;
                else
                    bytes[i] = 1;
            after = COBSUtil.cobsEncode(bytes);
            assertTrue(bytes.length == (after.length-1));
            for (int i = 0; i < after.length; i++) {
                debug("COBSUtilTest.encode(): i = " + i);
                if (i == 0)
                    assertTrue(after[0] == 1);
                else if ((i == 1) ||
                         ((((i-1) % j) == 0) && (num > i))
                         )
                    assertTrue(ByteOrder.ubyte2int(after[i]) > 1);
                else
                    assertTrue(after[i] == 1);
            }
            decoded = COBSUtil.cobsDecode(after);
            for (int i = 0; i < bytes.length; i++)
                assertTrue(bytes[i] == decoded[i]);
            assertTrue(decoded[bytes.length] == 0);            
        }
        // ----------------------------------

        // ----------------------------------
        // build up 'induction' case for (1..)0.  we can trust 'induction' due
        // to nature of the COBS algorithm...

        // test 1s and 0, specifically (1)^(j-1s)0....
        for (int j = 2; j < 255; j++) {
            for (int i = 0; i < bytes.length; i++) 
                if (i % j == 0)
                    bytes[i] = 1;
                else
                    bytes[i] = 0;
            after = COBSUtil.cobsEncode(bytes);
            assertTrue(bytes.length == (after.length - 1));
            for (int i = 0; i < bytes.length; i++)
                if ((i == 0) ||
                    (i % j == 0)
                    )
                    assertTrue(ByteOrder.ubyte2int(after[i]) > 1);
                else
                    assertTrue(after[i] == 1);
            decoded = COBSUtil.cobsDecode(after);
            for (int i = 0; i < bytes.length; i++)
                assertTrue(bytes[i] == decoded[i]);
            assertTrue(decoded[bytes.length] == 0);
        }
        // ----------------------------------
    }


    private static final boolean debugOn = false;
    private static final void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }


    public void testSymmetry() throws java.io.IOException {
        // a quick test for symmetry - but symmetry was actually tested above,
        // so no need for much testing...
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
