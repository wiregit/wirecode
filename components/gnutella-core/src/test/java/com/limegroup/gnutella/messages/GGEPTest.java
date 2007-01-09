package com.limegroup.gnutella.messages;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.limewire.collection.NameValue;
import org.limewire.io.IOUtils;

import junit.framework.Test;


@SuppressWarnings( { "unchecked", "cast" } )
public class GGEPTest extends com.limegroup.gnutella.util.LimeTestCase {
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
            assertEquals(nulls, shouldBeNulls);
            assertEquals(someNulls, two.getString("Daswani"));
            assertEquals(10, two.getInt("Number"));
        }
        catch (IllegalArgumentException illegal) {
            fail("The .put method failed!! ", illegal);
        }
    }
    
    public void testPutAll() throws Exception {
        List putm = new LinkedList();
        putm.add(new NameValue("int", new Integer(1)));
        putm.add(new NameValue("header"));
        putm.add(new NameValue("long", new Long(2)));
        putm.add(new NameValue("string", "value"));
        putm.add(new NameValue("byte[]", new byte[] { 1, 2 } ));
        
        GGEP temp = new GGEP(true);
        temp.putAll(putm);
        
        assertTrue(temp.hasKey("int"));
        assertEquals(1, temp.getInt("int"));
        assertTrue(temp.hasKey("header"));
        assertTrue(temp.hasKey("long"));
        assertEquals(2, temp.getLong("long"));
        assertTrue(temp.hasKey("string"));
        assertEquals("value", temp.getString("string"));
        assertTrue(temp.hasKey("byte[]"));
        assertEquals(new byte[] { 1, 2 }, temp.getBytes("byte[]"));
    }

    public void testStringKeys() throws Exception {
        GGEP temp = new GGEP(true);
        temp.put("A", "B");
        temp.put("C", (String)null);
        temp.put(GGEP.GGEP_HEADER_BROWSE_HOST, "");
        assertTrue(temp.hasKey("A"));
        assertEquals("B", temp.getString("A"));
        assertTrue(temp.hasKey("C"));
        assertTrue(temp.hasKey(GGEP.GGEP_HEADER_BROWSE_HOST));
        assertEquals("", temp.getString(GGEP.GGEP_HEADER_BROWSE_HOST));
    }

    public void testByteKeys() throws Exception {
        GGEP temp = new GGEP(true);
        temp.put("A", new byte[] { (byte)3 });
        assertTrue(temp.hasKey("A"));
        assertEquals(temp.getBytes("A"), new byte[] { (byte)3 });
    }

    public void testIntKeys() throws Exception {
        GGEP temp = new GGEP(true);
        temp.put("A", 527);
        assertTrue(temp.hasKey("A"));
        assertEquals(527, temp.getInt("A"));
        assertEquals(temp.getBytes("A"),
                     new byte[] { (byte)0x0F, (byte)0x02 });
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
            assertEquals(currValue, bigBoy.toString());
        }
    }


    /** Null bytes allowed, e.g., in ping replies */
    public void testValueContainsLegalNull() {
        byte[] bytes = new byte[2];
        bytes[0] = (byte)'S';
        bytes[1] = (byte)0x0;
        String hasANull = new String(bytes);

        GGEP temp = new GGEP(true);
        temp.put("WHATEVER", hasANull);
    }

    /** Null bytes are always allowed now.
     */
    public void testValueContainsIllegalNull() {
        byte[] bytes = new byte[2];
        bytes[0] = (byte)'S';
        bytes[1] = (byte)0x0;
        String hasANull = new String(bytes);

        GGEP temp = new GGEP(false);
        temp.put("WHATEVER", hasANull);
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

        assertEquals(a1, a1);
        assertEquals(a1, a2);
        assertEquals(b1, b1);
        assertEquals(b1, b2);
        assertNotEquals(a1, b1);
        assertNotEquals(b1, a1);
        
        GGEP c1=new GGEP(true);
        c1.put("K1", "V1");
        c1.put("K2", "V2");
        GGEP c2=new GGEP(true);
        c2.put("K1", "V1");
        c2.put("K2", "V2");        

        assertEquals(c1, c1);
        assertEquals(c1, c2);
        assertNotEquals(a1, c1);
        assertNotEquals(b1, c1);
    }
    
    public void testPutCompressed() throws Exception {
        byte[] middleValue = "middle".getBytes();
        GGEP g = new GGEP();
        g.put("1", "begin");
        g.putCompressed("2", middleValue);
        g.put("3", "end");
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        g.write(out);
        
        byte[] bytes = out.toByteArray();
        int i = 0;
        assertEquals(GGEP.GGEP_PREFIX_MAGIC_NUMBER, bytes[i++]);
        assertEquals( (byte)0x01, bytes[i++] );
        assertEquals( (byte)'1',  bytes[i++] );
        assertEquals( (byte)0x45, bytes[i++] );
        assertEquals( (byte)'b',  bytes[i++] );
        assertEquals( (byte)'e',  bytes[i++] );
        assertEquals( (byte)'g',  bytes[i++] );
        assertEquals( (byte)'i',  bytes[i++] );
        assertEquals( (byte)'n',  bytes[i++] );
        assertEquals( (byte)0x21, bytes[i++] );
        assertEquals( (byte)'2',  bytes[i++] );
        
        // now, we have to figure out how many bytes are used up in the
        // compressed version of 'middle'.
        byte[] compressed = IOUtils.deflate(middleValue);
        assertNotEquals(middleValue, compressed);
        int length = compressed.length;
        if((length & 0x3F000) != 0)
            assertEquals( (byte)0x80 | ((length & 0x3F000) >> 12), bytes[i++]);
        if((length & 0xFC0) != 0)
            assertEquals( (byte)0x80 | ((length & 0xFC0) >> 6), bytes[i++]);
        assertEquals((byte)0x40 | (length & 0x3F), bytes[i++]);
        
        for(int j = 0; j < compressed.length; j++)
            assertEquals(compressed[j], bytes[i++]);
        // end of checking for the compressed data.
            
        assertEquals( (byte)0x81, bytes[i++] );
        assertEquals( (byte)'3',  bytes[i++] );
        assertEquals( (byte)0x43, bytes[i++] );
        assertEquals( (byte)'e',  bytes[i++] );
        assertEquals( (byte)'n',  bytes[i++] );
        assertEquals( (byte)'d',  bytes[i++] );
    }
    
    public void testReadCompressedAndWritesCompressed() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(GGEP.GGEP_PREFIX_MAGIC_NUMBER);
        out.write(0x01);
        out.write('1');
        out.write(0x45);
        out.write('b');
        out.write('e');
        out.write('g');
        out.write('i');
        out.write('n');
        out.write(0x21);
        out.write('2');
        byte[] middleValue = "the middle value, compressed.".getBytes();
        byte[] compressed = IOUtils.deflate(middleValue);
        assertNotEquals(middleValue, compressed);
        int length = compressed.length;
        if((length & 0x3F000) != 0)
            out.write( (byte)0x80 | ((length & 0x3F000) >> 12));
        if((length & 0xFC0) != 0)
            out.write( (byte)0x80 | ((length & 0xFC0) >> 6));
        out.write((byte)0x40 | (length & 0x3F));
        out.write(compressed);
        out.write(0x81);
        out.write('3');
        out.write(0x43);
        out.write('e');
        out.write('n');
        out.write('d');
        
        int offsets[] = new int[1];
        byte[] bytes = out.toByteArray();
        GGEP ggep = new GGEP(bytes, 0, offsets);
        assertEquals(bytes.length, offsets[0]);
        assertEquals(3, ggep.getHeaders().size());
        assertEquals("begin", ggep.getString("1"));
        assertEquals(new String(middleValue), ggep.getString("2"));
        assertEquals("end", ggep.getString("3"));
        
        // Make sure that also write the data as compressed, if it was read that way.
        ByteArrayOutputStream toWrite = new ByteArrayOutputStream();
        ggep.write(toWrite);
        assertEquals(bytes, toWrite.toByteArray());
    }

    public void testBasicConstruction() throws Exception {
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
        GGEP ggep = new GGEP(bytes, 0);
                          
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
        ggep = new GGEP(bytes, 0);
        assertEquals(1, ggep.getHeaders().size());
        assertTrue(ggep.hasKey("BHOST"));
        ggep = new GGEP(bytes, 8);
        assertEquals(2, ggep.getHeaders().size());
        assertTrue(ggep.hasKey("BHOST"));
        assertTrue(ggep.hasKey("SUSHEEL"));
        assertEquals("DASWANI", ggep.getString("SUSHEEL"));

        bytes = new byte[24];
        bytes[0] = (byte)GGEP.GGEP_PREFIX_MAGIC_NUMBER;
        bytes[1] = (byte)0x15;
        bytes[2] = (byte)'B';
        bytes[3] = (byte)'H';
        bytes[4] = (byte)'O';
        bytes[5] = (byte)'S';
        bytes[6] = (byte)'T';
        bytes[7] = (byte)0x40;
        bytes[8] = (byte)0x87;
        bytes[9] = (byte)'S';
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
            ggep = new GGEP(bytes, 0);
            fail("should have constructed");
        } catch (BadGGEPBlockException expected) {}
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
        assertNotNull(shouldNotBeNull);
        assertEquals(24, endOffset[0]);
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

        bytes[8] = (byte)0xA5; // compressed, without valid data.
        try {
            new GGEP(bytes,0,null);
            fail("shoulda failed");
        } catch(BadGGEPBlockException bgbe) {}


        bytes[8] = (byte)0x80; // 0 len header
        try {
            new GGEP(bytes,0,null);
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
            new GGEP(bytes,0,null);
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

        assertEquals(one.getHeaders(), two.getHeaders());
        assertEquals(one, two);
    }


    public void testMalformedGGEP() throws Exception {
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
            new GGEP(bytes,0,null);
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
            new GGEP(bytes,0,null);
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
            new GGEP(bytes,0,null);
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
            new GGEP(bytes,0,null);
            fail("need to fail");
        }
        catch (BadGGEPBlockException hopefullySo) {
        }


        // not enough bytes!
        bytes = new byte[0];
        try {
            new GGEP(bytes,0,null);
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
            new GGEP(bytes,0,null);
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
            new GGEP(bytes,0,null);
            fail("need to fail");            
        }
        catch (BadGGEPBlockException hopefullySo) {
        }

    }

    public void testMissingMiddleValueSize() throws Exception {
    	
    	int length = 0x0003f03f;    // in the size 258111, the 6 bytes in the middle are missing 00000000 00000011 11110000 00111111
    	GGEP ggep = new GGEP(true); // don't do COBS encoding
    	byte[] value = new byte[length];
    	ggep.put("TEST", value);
    	
    	// serialize it to data
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	ggep.write(out);
    	byte[] serialized = out.toByteArray();
    	
    	// turn that back into a new GGEP block and make sure it's the same
    	GGEP ggep2 = new GGEP(serialized, 0);
    	byte[] value2 = ggep2.getBytes("TEST");
    	assertEquals(value.length, value2.length);
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
}
