package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;

import junit.framework.Test;

import com.sun.java.util.collections.Arrays;
import com.sun.java.util.collections.Set;

public class GGEPTest extends com.limegroup.gnutella.util.BaseTestCase {
    public GGEPTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(GGEPTest.class);
    }


    public void testCOBS() throws Exception {
        byte[] nulls = new byte[10];
        for (int i = 0; i < nulls.length; i++)
            nulls[i] = (byte)(i % 2);
        String someNulls = "Hello" + new String(nulls);
        try {
            GGEP one = new GGEP(false);
            one.put("Susheel", nulls);
            one.put("Daswani", someNulls);
            one.put("Number", 10);
            ByteArrayOutputStream oStream = new ByteArrayOutputStream();
            one.write(oStream);
            GGEP two = new GGEP(oStream.toByteArray(), 0, null);
            assertTrue(two.hasKey("Susheel"));
            byte[] shouldBeNulls = two.getBytes("Susheel");
            assertTrue(Arrays.equals(nulls, shouldBeNulls));
            assertEquals(someNulls, two.getString("Daswani"));
            assertEquals(10, two.getInt("Number"));
        }
        catch (IllegalArgumentException illegal) {
            fail("The .put method failed!! ", illegal);
        }
    }

    public void testStringKeys() throws Exception {
        try {
            GGEP temp = new GGEP(true);
            temp.put("A", "B");
            temp.put("C", (String)null);
            temp.put(GGEP.GGEP_HEADER_BROWSE_HOST, "");
            assertTrue(temp.hasKey("A"));
            assertEquals("B", temp.getString("A"));
            assertTrue(temp.hasKey("C"));
            assertTrue(temp.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
            assertEquals("", temp.getString(GGEP.GGEP_HEADER_BROWSE_HOST));
        } catch (BadGGEPPropertyException failed) {
            fail("Couldn't get property", failed);
        }
    }

    public void testByteKeys() throws Exception {
        try {
            GGEP temp = new GGEP(true);
            temp.put("A", new byte[] { (byte)3 });
            assertTrue(temp.hasKey("A"));
            assertTrue(Arrays.equals(temp.getBytes("A"),
                                     new byte[] { (byte)3 }));
        } catch (BadGGEPPropertyException failed) {
            fail("Couldn't get property", failed);
        }
    }

    public void testIntKeys() throws Exception {
        try {
            GGEP temp = new GGEP(true);
            temp.put("A", 527);
            assertTrue(temp.hasKey("A"));
            assertEquals(527, temp.getInt("A"));
            assertTrue(Arrays.equals(temp.getBytes("A"),
                                     new byte[] { (byte)0x0F, (byte)0x02 }));
        } catch (BadGGEPPropertyException failed) {
            fail("Couldn't get property", failed);
        }
    }
    
    public void testLongKeys() throws Exception {
        GGEP temp = new GGEP(true);
        temp.put("A", 0xABCDL);
        assertTrue(temp.hasKey("A"));
        assertEquals(0xABCDL, temp.getLong("A"));
        assertEquals(temp.getBytes("A"), new byte[] {(byte)0xCD,(byte)0xAB} );
        
        temp.put("A", 0x00ABCDEF12L);
        assertTrue(temp.hasKey("A"));
        assertEquals(0xABCDEF12L, temp.getLong("A"));
        assertEquals(temp.getBytes("A"), new byte[] {
            (byte)0x12, (byte)0xEF, (byte)0xCD, (byte)0xAB });
    }
        

    /** Tests that map constructor doesn't accept keys that are too long, should
     *  throw an exception */
    public void testKeyTooBig() {
        try {
            GGEP temp = new GGEP(true);
            temp.put("THIS KEY IS WAY TO LONG!", "");
            fail("No IllegalArgumentException.");
        } catch (IllegalArgumentException pass) { 
        }
    }

    /** Tests that map constructor doesn't accept data that is too long, should
     *  throw an exception. */
    public void testValueTooBig() {
        StringBuffer bigBoy = new StringBuffer(GGEP.MAX_VALUE_SIZE_IN_BYTES+10);
        for (int i = 0; i < GGEP.MAX_VALUE_SIZE_IN_BYTES+10; i++)
            bigBoy.append("1");
        
        try {
            GGEP temp = new GGEP(true);
            temp.put("WHATEVER", bigBoy.toString());
            fail("No IllegalArgumentException.");
        } catch (IllegalArgumentException pass) { }
    }


    /** Test to see if the GGEP can handle datalens that are pretty big.... 
     */
    public void testBigValue() throws Exception {
        StringBuffer bigBoy = new StringBuffer(GGEP.MAX_VALUE_SIZE_IN_BYTES);
        for (int i = 0; i < GGEP.MAX_VALUE_SIZE_IN_BYTES; i++)
            bigBoy.append("1");
        
        String[] keys = {"Susheel", "is", "an", "Idiot!!"};

        GGEP temp = new GGEP(true);
        for (int i = 0; i < keys.length; i++)
            temp.put(keys[i], bigBoy.toString());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        temp.write(baos);
        byte[] ggepBytes = baos.toByteArray();
        GGEP reconstruct = new GGEP(ggepBytes, 0, null);
        for (int i = 0; i < keys.length; i++) {
            String currValue = reconstruct.getString(keys[i]);
            assertTrue(currValue.equals(bigBoy.toString()));
        }
    }


    /** Null bytes allowed, e.g., in ping replies */
    public void testValueContainsLegalNull() {
        byte[] bytes = new byte[2];
        bytes[0] = (byte)'S';
        bytes[1] = (byte)0x0;
        String hasANull = new String(bytes);

        try {
            GGEP temp = new GGEP(true);
            temp.put("WHATEVER", hasANull);
        } catch (IllegalArgumentException pass) { 
            fail("No IllegalArgumentException.");
        }
    }

    /** Null bytes are always allowed now.
     */
    public void testValueContainsIllegalNull() {
        byte[] bytes = new byte[2];
        bytes[0] = (byte)'S';
        bytes[1] = (byte)0x0;
        String hasANull = new String(bytes);

        try {
            GGEP temp = new GGEP(false);
            temp.put("WHATEVER", hasANull);
        } catch (IllegalArgumentException fail) { 
            fail("IllegalArgumentException.");
        }
    }

    public void testEquals() {
        GGEP a1=new GGEP(true);
        a1.put("K1", "V1");
        GGEP a2=new GGEP(true);
        a2.put("K1", "V1");
        GGEP b1=new GGEP(true);
        b1.put("K1");
        GGEP b2=new GGEP(true);
        b2.put("K1");

        assertTrue(a1.equals(a1));
        assertTrue(a1.equals(a2));
        assertTrue(b1.equals(b1));
        assertTrue(b1.equals(b2));
        assertTrue(! a1.equals(b1));
        assertTrue(! b1.equals(a1));
        
        GGEP c1=new GGEP(true);
        c1.put("K1", "V1");
        c1.put("K2", "V2");
        GGEP c2=new GGEP(true);
        c2.put("K1", "V1");
        c2.put("K2", "V2");        

        assertTrue(c1.equals(c1));
        assertTrue(c1.equals(c2));
        assertTrue(! a1.equals(c1));
        assertTrue(! b1.equals(c1));
    }


    public void testStaticReadMethod() throws Exception {
        byte[] bytes = new byte[24];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x05;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = (byte)0x87;
        bytes[9] =  (byte)'S';
        bytes[10] = (byte)'U';
        bytes[11] = (byte)'S';
        bytes[12] = (byte)'H';
        bytes[13] = (byte)'E';
        bytes[14] = (byte)'E';
        bytes[15] = (byte)'L';
        bytes[16] = (byte)0x47;
        bytes[17] = (byte)'D';
        bytes[18] = (byte)'A';
        bytes[19] = (byte)'S';
        bytes[20] = (byte)'W';
        bytes[21] = (byte)'A';
        bytes[22] = (byte)'N';
        bytes[23] = (byte)'I';        
        GGEP[] temp = GGEP.read(bytes,0);
        assertTrue("read() test - WRONG SIZE: " + temp.length, 
                          temp.length == 1);
                          
        bytes = new byte[32];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x85;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[9] = (byte)0x05;
        bytes[10] = (byte)'B';
        bytes[11] = (byte)'H';
        bytes[12] = (byte)'O';
        bytes[13] = (byte)'S';
        bytes[14] = (byte)'T';
        bytes[15] = (byte)0x40;
        bytes[16] = (byte)0x87;
        bytes[17] = (byte)'S';
        bytes[18] = (byte)'U';
        bytes[19] = (byte)'S';
        bytes[20] = (byte)'H';
        bytes[21] = (byte)'E';
        bytes[22] = (byte)'E';
        bytes[23] = (byte)'L';
        bytes[24] = (byte)0x47;
        bytes[25] = (byte)'D';
        bytes[26] = (byte)'A';
        bytes[27] = (byte)'S';
        bytes[28] = (byte)'W';
        bytes[29] = (byte)'A';
        bytes[30] = (byte)'N';
        bytes[31] = (byte)'I';        
        temp = GGEP.read(bytes,0);
        assertTrue("read() test - WRONG SIZE: " + temp.length, 
                          temp.length == 2);
        assertTrue("read() test - first ggep wrong size: " +
                          temp[0].getHeaders().size(),
                          temp[0].getHeaders().size() == 1);
        assertTrue("read() test - second ggep wrong size: " +
                          temp[1].getHeaders().size(),
                          temp[1].getHeaders().size() == 2);

        bytes = new byte[32];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x85;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = (byte)GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[9] = (byte)0x15;
        bytes[10] = (byte)'B';
        bytes[11] = (byte)'H';
        bytes[12] = (byte)'O';
        bytes[13] = (byte)'S';
        bytes[14] = (byte)'T';
        bytes[15] = (byte)0x40;
        bytes[16] = (byte)0x87;
        bytes[17] = (byte)'S';
        bytes[18] = (byte)'U';
        bytes[19] = (byte)'S';
        bytes[20] = (byte)'H';
        bytes[21] = (byte)'E';
        bytes[22] = (byte)'E';
        bytes[23] = (byte)'L';
        bytes[24] = (byte)0x47;
        bytes[25] = (byte)'D';
        bytes[26] = (byte)'A';
        bytes[27] = (byte)'S';
        bytes[28] = (byte)'W';
        bytes[29] = (byte)'A';
        bytes[30] = (byte)'N';
        bytes[31] = (byte)'I';
        try {
            temp = GGEP.read(bytes,0);
            assertTrue(false);
        }
        catch (BadGGEPBlockException expected) {}

        bytes = new byte[32];
        bytes[0] = (byte)0x0;
        bytes[1] = (byte)0x85;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = (byte)GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[9] = (byte)0x15;
        bytes[10] = (byte)'B';
        bytes[11] = (byte)'H';
        bytes[12] = (byte)'O';
        bytes[13] = (byte)'S';
        bytes[14] = (byte)'T';
        bytes[15] = (byte)0x40;
        bytes[16] = (byte)0x87;
        bytes[17] = (byte)'S';
        bytes[18] = (byte)'U';
        bytes[19] = (byte)'S';
        bytes[20] = (byte)'H';
        bytes[21] = (byte)'E';
        bytes[22] = (byte)'E';
        bytes[23] = (byte)'L';
        bytes[24] = (byte)0x47;
        bytes[25] = (byte)'D';
        bytes[26] = (byte)'A';
        bytes[27] = (byte)'S';
        bytes[28] = (byte)'W';
        bytes[29] = (byte)'A';
        bytes[30] = (byte)'N';
        bytes[31] = (byte)'I';
        try {
            temp = GGEP.read(bytes,0);
            assertTrue(false);
        }
        catch (BadGGEPBlockException expected) {}

    }


    // tests normal behavior of the byte[] constructor
    public void testByteArrayConstructor1() throws Exception {
        int[] endOffset = new int[1];
        byte[] bytes = new byte[24];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x05;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = (byte)0x87;
        bytes[9] =  (byte)'S';
        bytes[10] = (byte)'U';
        bytes[11] = (byte)'S';
        bytes[12] = (byte)'H';
        bytes[13] = (byte)'E';
        bytes[14] = (byte)'E';
        bytes[15] = (byte)'L';
        bytes[16] = (byte)0x47;
        bytes[17] = (byte)'D';
        bytes[18] = (byte)'A';
        bytes[19] = (byte)'S';
        bytes[20] = (byte)'W';
        bytes[21] = (byte)'A';
        bytes[22] = (byte)'N';
        bytes[23] = (byte)'I';        

        GGEP temp = new GGEP(bytes,0, endOffset);
        Set headers = temp.getHeaders();
        assertTrue("Test 5 - NO BHOST!", headers.contains("BHOST"));
        try {
            temp.getString("BHOST");
            fail("No BadGGEPPropertyException.");
        } catch (BadGGEPPropertyException e) {
        }
        assertTrue("Test 5 - NO SUSH!", headers.contains("SUSHEEL"));
        Object shouldNotBeNull = temp.getString("SUSHEEL");
        assertTrue("Test 5 - NULL!", shouldNotBeNull != null);
        assertTrue("Test 5 - endOffset off: " + endOffset[0], 
                          endOffset[0] == 24);
    }


    // tests abnormal behavior of the byte[] constructor - tries to give
    // compressed data, a 0 header length, and a data length
    // which is stored in more than 3 bytes...
    public void testByteArrayConstructor2() throws Exception {
        byte[] bytes = new byte[24];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x05;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = (byte)0x87; 
        bytes[9] =  (byte)'S';
        bytes[10] = (byte)'U';
        bytes[11] = (byte)'S';
        bytes[12] = (byte)'H';
        bytes[13] = (byte)'E';
        bytes[14] = (byte)'E';
        bytes[15] = (byte)'L';
        bytes[16] = (byte)0x47;
        bytes[17] = (byte)'D';
        bytes[18] = (byte)'A';
        bytes[19] = (byte)'S';
        bytes[20] = (byte)'W';
        bytes[21] = (byte)'A';
        bytes[22] = (byte)'N';
        bytes[23] = (byte)'I';        

        bytes[8] = (byte)0xA5; // compressed
        GGEP temp = new GGEP(bytes,0,null);
        Set headers = temp.getHeaders();
        assertTrue("Test 6 - COMPRESSED!", 
                          !headers.contains("SUSHEEL"));


        bytes[8] = (byte)0x80; // 0 len header
        try {
            temp = new GGEP(bytes,0,null);
            assertTrue("Test 6 - 0 LEN HEADER!", false);
        } 
        catch (BadGGEPBlockException hopefullySo) {
        }

        bytes[8] = (byte)0x87; 
        bytes[16] = (byte)0xBF;
        bytes[17] = (byte)0xBF;
        bytes[18] = (byte)0xBF;
        bytes[19] = (byte)0xBF;
        try {
            temp = new GGEP(bytes,0,null);
            assertTrue("Test 6 - >3 DATA LEN!", false);
        } 
        catch (BadGGEPBlockException hopefullySo) {
        }

        
    }


    public void testWriteMethod() throws Exception {
        GGEP one = null, two = null;
        byte[] bytes = new byte[24];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x05;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = (byte)0x87;
        bytes[9] =  (byte)'S';
        bytes[10] = (byte)'U';
        bytes[11] = (byte)'S';
        bytes[12] = (byte)'H';
        bytes[13] = (byte)'E';
        bytes[14] = (byte)'E';
        bytes[15] = (byte)'L';
        bytes[16] = (byte)0x47;
        bytes[17] = (byte)'D';
        bytes[18] = (byte)'A';
        bytes[19] = (byte)'S';
        bytes[20] = (byte)'W';
        bytes[21] = (byte)'A';
        bytes[22] = (byte)'N';
        bytes[23] = (byte)'I';        
        one = new GGEP(bytes,0,null);

        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        one.write(oStream);
        two = new GGEP(oStream.toByteArray(), 0, null);
                
        assertTrue("Different headers",
                   one.getHeaders().equals(two.getHeaders()));
        assertTrue("One is not Two!!", one.equals(two));
    }


    public void testMalformedGGEP() throws Exception {
        GGEP one = null;
        byte[] bytes = new byte[24];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x05;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = (byte)0x87;
        bytes[9] =  (byte)'S';
        bytes[10] = (byte)'U';
        bytes[11] = (byte)'S';
        bytes[12] = (byte)'H';
        bytes[13] = (byte)'E';
        bytes[14] = (byte)'E';
        bytes[15] = (byte)'L';
        bytes[16] = (byte)0x47;
        bytes[17] = (byte)'D';
        bytes[18] = (byte)'A';
        bytes[19] = (byte)'S';
        bytes[20] = (byte)'W';
        bytes[21] = (byte)'A';
        bytes[22] = (byte)'N';
        bytes[23] = (byte)'I';                

        // test first case where no magic number....
        bytes[0] = (byte) 'I';
        try {
            one = new GGEP(bytes,0,null);
            fail("need to fail.");
        }
        catch (BadGGEPBlockException hopefullySo) {
        }

        // now test a ID Len that is lying!
        bytes = new byte[6];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x05;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        try {
            one = new GGEP(bytes,0,null);
            fail("need to fail.");
        }
        catch (BadGGEPBlockException hopefullySo) {
        }

        // too many data length fields...
        bytes = new byte[24];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x05;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0xbf;
        bytes[8] = (byte)0xbf;
        bytes[9] = (byte)0xbf;
        bytes[10] = (byte)'U';
        bytes[11] = (byte)'S';
        bytes[12] = (byte)'H';
        bytes[13] = (byte)'E';
        bytes[14] = (byte)'E';
        bytes[15] = (byte)'L';
        bytes[16] = (byte)0x47;
        bytes[17] = (byte)'D';
        bytes[18] = (byte)'A';
        bytes[19] = (byte)'S';
        bytes[20] = (byte)'W';
        bytes[21] = (byte)'A';
        bytes[22] = (byte)'N';
        bytes[23] = (byte)'I';                
        try {
            one = new GGEP(bytes,0,null);
            fail("need to fail");
        }
        catch (BadGGEPBlockException hopefullySo) {
        }
        

        // not enough data length fields...
        bytes = new byte[9];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x05;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0xbf;
        bytes[8] = (byte)0xbf;
        try {
            one = new GGEP(bytes,0,null);
            fail("need to fail");
        }
        catch (BadGGEPBlockException hopefullySo) {
        }


        // not enough bytes!
        bytes = new byte[0];
        try {
            one = new GGEP(bytes,0,null);
            fail("need to fail");            
        }
        catch (BadGGEPBlockException hopefullySo) {
        }

        // not enough data fields...
        bytes = new byte[22];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x05;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = (byte)0x87;
        bytes[9] =  (byte)'S';
        bytes[10] = (byte)'U';
        bytes[11] = (byte)'S';
        bytes[12] = (byte)'H';
        bytes[13] = (byte)'E';
        bytes[14] = (byte)'E';
        bytes[15] = (byte)'L';
        bytes[16] = (byte)0x47;
        bytes[17] = (byte)'D';
        bytes[18] = (byte)'A';
        bytes[19] = (byte)'S';
        bytes[20] = (byte)'W';
        bytes[21] = (byte)'A';
        try {
            one = new GGEP(bytes,0,null);
            fail("need to fail");
        }
        catch (BadGGEPBlockException hopefullySo) {
        }


        // just a messed up GGEP block...
        bytes = new byte[22];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x05;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = (byte)'S';
        bytes[9] =  (byte)'S';
        bytes[10] = (byte)'U';
        bytes[11] = (byte)'S';
        bytes[12] = (byte)'H';
        bytes[13] = (byte)'E';
        bytes[14] = (byte)'E';
        bytes[15] = (byte)'L';
        bytes[16] = (byte)0x47;
        bytes[17] = (byte)'D';
        bytes[18] = (byte)'A';
        bytes[19] = (byte)'S';
        bytes[20] = (byte)'W';
        bytes[21] = (byte)'A';
        bytes[21] = (byte)'N';
        bytes[21] = (byte)'I';
        try {
            one = new GGEP(bytes,0,null);
            fail("need to fail");            
        }
        catch (BadGGEPBlockException hopefullySo) {
        }


    }


    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
}
