package com.limegroup.gnutella.util;

import java.io.IOException;

import org.limewire.util.ByteOrder;

import junit.framework.Test;


/**
 * Tests COBSUtil
 */
public class COBSUtilTest extends com.limegroup.gnutella.util.LimeTestCase {
    public COBSUtilTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(COBSUtilTest.class);
    }  


    public void testBoundCase() throws IOException {
        byte[] bytes = new byte[254];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) 7;
        byte[] after = COBSUtil.cobsEncode(bytes);
        assertTrue("after[0] is " + after[0], after[0] == ((byte)0xFF));
        assertEquals(256, after.length); // 2 bytes of overhead for 254 bytes
        byte[] afterOptimized = new byte[255];
        // some people leave off that last 0, we should react OK
        System.arraycopy(after, 0, afterOptimized, 0, afterOptimized.length);
        byte[] decoded = COBSUtil.cobsDecode(afterOptimized);
        assertEquals(254, decoded.length);
        for (int i = 0; i < bytes.length; i++)
            assertTrue(bytes[i] == decoded[i]);
    }

    public void testEncodeAndDecode() throws IOException {
        for (int num = 1; num < 260; num++) 
            encodeAndDecode(num);
    }
    

    private void encodeAndDecode(int num) throws IOException {
        // test all 0s...
        debug("COBSUtilTest.encode(): num = " +
              num);
        byte[] bytes = new byte[num];
        for (int i = 0; i < bytes.length; i++)
            bytes[i] = 0;
        byte[] after = COBSUtil.cobsEncode(bytes);
        assertEquals(bytes.length , (after.length-1));
        for (int i = 0; i < after.length; i++)
            assertEquals(0x01, after[i]);
        byte[] decoded = COBSUtil.cobsDecode(after);
        for (int i = 0; i < bytes.length; i++)
            assertEquals(bytes[i], decoded[i]);


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
            assertEquals("num = " + num + ", i = " + i, 
                       bytes[i], decoded[i]);
        
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
            assertEquals(bytes.length,(after.length-1));
            for (int i = 0; i < after.length; i++) {
                debug("COBSUtilTest.encode(): i = " + i);
                if (i == 0)
                    assertEquals(1,after[0]);
                else if ((i == 1) ||
                         ((((i-1) % j) == 0) && (num > i))
                         )
                    assertGreaterThan(1, ByteOrder.ubyte2int(after[i]));
                else
                    assertEquals(1, after[i]);
            }
            decoded = COBSUtil.cobsDecode(after);
            for (int i = 0; i < bytes.length; i++)
                assertEquals(bytes[i], decoded[i]);
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
            assertEquals(bytes.length ,(after.length - 1));
            for (int i = 0; i < bytes.length; i++)
                if ((i == 0) ||
                    (i % j == 0)
                    )
                    assertGreaterThan(1, ByteOrder.ubyte2int(after[i]));
                else
                    assertEquals(1, after[i]);
            decoded = COBSUtil.cobsDecode(after);
            for (int i = 0; i < bytes.length; i++)
                assertEquals(bytes[i] , decoded[i]);
        }
        // ----------------------------------
    }


    private static final boolean debugOn = false;
    private static final void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }


    public void testSymmetry() throws IOException {
        // a quick test for symmetry - but symmetry was actually tested above,
        // so no need for much testing...
        byte[] bytes = (new String("Sush Is Cool!")).getBytes();
        byte[] after = COBSUtil.cobsDecode(COBSUtil.cobsEncode(bytes));
        assertEquals(bytes.length , after.length);
    }


    public void testBadCOBSBlock() throws Exception {
        byte[] badBlock = new byte[10];
        badBlock[0] = (byte) 11;
        for (int i = 1; i < badBlock.length; i++)
            badBlock[i] = (byte)1;
        try {
            COBSUtil.cobsDecode(badBlock);
            assertTrue(false);
        }
        catch (IOException expected) {}

        badBlock = new byte[10];
        badBlock[0] = (byte) 10;
        for (int i = 1; i < badBlock.length; i++)
            badBlock[i] = (byte)1;
        COBSUtil.cobsDecode(badBlock);

        badBlock = new byte[4];
        badBlock[0] = (byte) 2;
        badBlock[1] = (byte) 1;
        badBlock[2] = (byte) 1;
        badBlock[3] = (byte) 2;
        try {
            COBSUtil.cobsDecode(badBlock);
            assertTrue(false);
        }
        catch (IOException expected) {
        }

    }
    

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }


}
