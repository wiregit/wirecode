package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.message.*;
import java.io.*;
import com.sun.java.util.collections.*;
import junit.framework.*;

public class GGEPTest extends TestCase {

    private Map hashMap = null;

    public GGEPTest(String name) {
        super(name);
    }

    protected void setUp() {
        hashMap = new HashMap();
    }

    // tests map constructor, should pass....
    public void testMapConstructor1() {
        hashMap.clear();
        hashMap.put("A","B");
        hashMap.put("C",null);
        hashMap.put(GGEP.GGEP_HEADER_BROWSE_HOST,"");
        try {
            GGEP temp = new GGEP(hashMap);
        }
        catch (BadGGEPPropertyException hopefullyNot) {
            Assert.assertTrue("Test 1 Constructor Failed!", false);
        }
    }

    // tests that map constructor doesn't accept keys that are too long, should
    // throw an exception
    public void testMapConstructor2() {
        hashMap.clear();

        hashMap.put("THIS KEY IS WAY TO LONG!", "");

        try {
            GGEP temp = new GGEP(hashMap);
            Assert.assertTrue("Test 2 Constructor Failed!", false);
        }
        catch (BadGGEPPropertyException hopefullySo) {
        }        
    }

    // tests that map constructor doesn't accept data that is too long, should
    // throw an exception
    public void testMapConstructor3() {
        hashMap.clear();

        StringBuffer bigBoy = new StringBuffer(GGEP.MAX_VALUE_SIZE_IN_BYTES+10);
        for (int i = 0; i < GGEP.MAX_VALUE_SIZE_IN_BYTES+10; i++)
            bigBoy.append("1");
        
        hashMap.put("WHATEVER", bigBoy);
        try {
            GGEP temp = new GGEP(hashMap);
            Assert.assertTrue("Test 3 Constructor Failed!", false);
        }
        catch (BadGGEPPropertyException hopefullySo) {
        }
    }


    // tests that map constructor doesn't accept data that has a 0x0, should
    // throw an exception
    public void testMapConstructor4() {
        hashMap.clear();
        byte[] bytes = new byte[2];
        bytes[0] = (byte)'S';
        bytes[1] = (byte)0x0;
        String hasANull = new String(bytes);

        hashMap.put("WHATEVER", hasANull);
        try {
            GGEP temp = new GGEP(hashMap);
            Assert.assertTrue("Test 4 Constructor Failed!", false);
        }
        catch (BadGGEPPropertyException hopefullySo) {
        }
    }

    public void testStaticReadMethod() {
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
        try {
            GGEP[] temp = GGEP.read(bytes,0);
            Assert.assertTrue("read() test - WRONG SIZE: " + temp.length, 
                              temp.length == 1);
        }
        catch (BadGGEPBlockException hopefullyNot) {
            Assert.assertTrue("Test 5 Constructor Failed!", false);
        }
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
        try {
            GGEP[] temp = GGEP.read(bytes,0);
            Assert.assertTrue("read() test - WRONG SIZE: " + temp.length, 
                              temp.length == 2);
            Assert.assertTrue("read() test - first ggep wrong size: " +
                              temp[0].getHeaders().size(),
                              temp[0].getHeaders().size() == 1);
            Assert.assertTrue("read() test - second ggep wrong size: " +
                              temp[1].getHeaders().size(),
                              temp[1].getHeaders().size() == 2);
        }
        catch (BadGGEPBlockException hopefullyNot) {
            Assert.assertTrue("Test 5 Constructor Failed!", false);
        }


    }


    // tests normal behavior of the byte[] constructor
    public void testByteArrayConstructor1() {
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
        try {
            GGEP temp = new GGEP(bytes,0, endOffset);
            Set headers = temp.getHeaders();
            Assert.assertTrue("Test 5 - NO BHOST!", headers.contains("BHOST"));
            Object shouldBeNull = temp.getData("BHOST");
            Assert.assertTrue("Test 5 - NOT NULL!", shouldBeNull == null);
            Assert.assertTrue("Test 5 - NO SUSH!", headers.contains("SUSHEEL"));
            Object shouldNotBeNull = temp.getData("SUSHEEL");
            Assert.assertTrue("Test 5 - NULL!", shouldNotBeNull != null);
            Assert.assertTrue("Test 5 - endOffset off: " + endOffset[0], 
                              endOffset[0] == 24);
        }
        catch (BadGGEPBlockException hopefullyNot) {
            Assert.assertTrue("Test 5 Constructor Failed!", false);
        }
    }


    // tests abnormal behavior of the byte[] constructor - tries to give
    // compressed data, COBS encoded data, a 0 header length, and a data length
    // which is stored in more than 3 bytes...
    public void testByteArrayConstructor2() {
        byte[] bytes = new byte[24];
        bytes[0] = GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x05;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = (byte)0xC7; // encoded
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
        try {
            GGEP temp = new GGEP(bytes,0,null);
            Set headers = temp.getHeaders();
            Assert.assertTrue("Test 6 - ENCODED!", !headers.contains("SUSHEEL"));
        }
        catch (BadGGEPBlockException hopefullyNot) {
            Assert.assertTrue("Test 6 Constructor Failed!", false);
        }

        bytes[8] = (byte)0xA5; // compressed
        try {
            GGEP temp = new GGEP(bytes,0,null);
            Set headers = temp.getHeaders();
            Assert.assertTrue("Test 6 - COMPRESSED!", 
                              !headers.contains("SUSHEEL"));
        } 
        catch (BadGGEPBlockException hopefullyNot) {
            Assert.assertTrue("Test 6 Constructor Failed!", false);
        }

        bytes[8] = (byte)0x80; // 0 len header
        try {
            GGEP temp = new GGEP(bytes,0,null);
            Assert.assertTrue("Test 6 - 0 LEN HEADER!", false);
        } 
        catch (BadGGEPBlockException hopefullySo) {
        }

        bytes[8] = (byte)0x87; 
        bytes[16] = (byte)0xBF;
        bytes[17] = (byte)0xBF;
        bytes[18] = (byte)0xBF;
        bytes[19] = (byte)0xBF;
        try {
            GGEP temp = new GGEP(bytes,0,null);
            Assert.assertTrue("Test 6 - >3 DATA LEN!", false);
        } 
        catch (BadGGEPBlockException hopefullySo) {
        }

        
    }


    public void testWriteMethod() {
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
        try {
            one = new GGEP(bytes,0,null);
        }
        catch (BadGGEPBlockException hopefullyNot) {
            Assert.assertTrue("Write Test Failed!", false);
        }
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        try {
            one.write(oStream);
            two = new GGEP(oStream.toByteArray(), 0, null);
        }
        catch (IOException hopefullyNot1) {
            Assert.assertTrue("Instance Write Failed!!", false);
        }
        catch (BadGGEPBlockException hopefullyNot2) {
            Assert.assertTrue("Write wrote incorrectly!!", false);
        }

        Assert.assertTrue("One is not Two!!", one.equals(two));
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite =  new TestSuite("GGEP Unit Tests");
        suite.addTest(new TestSuite(GGEPTest.class));
        return suite;
    }



}
