package com.limegroup.gnutella.dime;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import com.sun.java.util.collections.HashMap;

import junit.framework.Test;

import com.limegroup.gnutella.ByteOrder;

/**
 * Tests for DIMERecord.
 */
public final class DIMERecordTest extends com.limegroup.gnutella.util.BaseTestCase {

	/**
	 * Constructs a new test instance for responses.
	 */
	public DIMERecordTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DIMERecordTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	
    // A DIME Record looks like the following:
    ///////////////////////////////////////////////////////////////////
    // 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 
    // ----------------------------------------------------------------
    //  VERSION |M|M|C|  TYPE |  RSRV |          OPTIONS_LENGTH
    //          |B|E|F|       |       |
    // ----------------------------------------------------------------
    //         ID_LENGTH              |          TYPE_LENGTH
    // ----------------------------------------------------------------
    //                           DATA_LENGTH
    // ----------------------------------------------------------------
    //                        OPTIONS + PADDING
    // ----------------------------------------------------------------
    //                          ID + PADDING
    // ----------------------------------------------------------------
    //                         TYPE + PADDING
    // ----------------------------------------------------------------
    //                         DATA + PADDING
    // ----------------------------------------------------------------
    ///////////////////////////////////////////////////////////////////	
    // TYPES:
    //  UNCHANGED: 0x00
    //  MEDIA_TYPE: 0x01
    //  ABSOLUTE_URI: 0x02
    //  UNKNOWN: 0x03
    //  NONE:  0x04
	
	public void testCreateFromStream() throws Exception {
	    // Basic DIMERecord, no data at all, correct version.
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( 0x00 ); // no type + reserved
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    out.write( new byte[] { 0, 0 } ); // type length: 0
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
	    DIMERecord record = DIMERecord.createFromStream(in);
	    assertFalse(record.isFirstRecord());
	    assertFalse(record.isLastRecord());
	    assertEquals(DIMERecord.TYPE_UNCHANGED, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	}
	
	public void testPadding() throws Exception {
	    // Make sure we pad output with the appropriate stuff.
	    DIMERecord record;
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ByteArrayInputStream in;
	    byte[] header = new byte[12];
	    
	    record = DIMERecord.create(DIMERecord.TYPE_MEDIA_TYPE,
	                               "4abc".getBytes(), "1".getBytes(),
	                               "2x".getBytes(), "3yz".getBytes());
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
        assertEquals("1", record.getIdentifier());
        assertEquals("1".getBytes(), record.getId());
        assertEquals("2x", record.getTypeString());
        assertEquals("3yz".getBytes(), record.getData());
        assertEquals("4abc".getBytes(), record.getOptions());
        
        record.write(out);
        in = new ByteArrayInputStream(out.toByteArray());
        assertEquals(12, in.read(header, 0, 12)); // must read 12 bytes of the header.
        assertEquals(0x08, header[0]); // version + flags
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, header[1]); // type + reserve
        int optionsLength = ByteOrder.beb2int(header, 2, 2);
        int idLength = ByteOrder.beb2int(header, 4, 2);
        int typeLength = ByteOrder.beb2int(header, 6, 2);
        int dataLength = ByteOrder.beb2int(header, 8, 4);
        assertEquals(4, optionsLength);
        assertEquals(1, idLength);
        assertEquals(2, typeLength);
        assertEquals(3, dataLength);
        // this is the real padding test -- all data is in multiples of 4,
        // with extra 0s at the end for the padding.
        byte[] info = new byte[4];
        assertEquals(4, in.read(info, 0, 4));
        assertEquals( new byte[] { '4', 'a', 'b', 'c' }, info);
        assertEquals(4, in.read(info, 0, 4));
        assertEquals( new byte[] { '1', 0, 0, 0 }, info );
        assertEquals(4, in.read(info, 0, 4));
        assertEquals( new byte[] { '2', 'x', 0, 0 }, info );
        assertEquals(4, in.read(info, 0, 4));
        assertEquals( new byte[] { '3', 'y', 'z', 0 }, info );
        assertEquals(-1, in.read());
    }
	
	public void testValidTypes() throws Exception {
	    DIMERecord record;
	    ByteArrayOutputStream out;
	    ByteArrayInputStream in;
	    
	    // A - TYPE_UNCHANGED.
	    // Valid only if the 'type' data is 0 length.
	    record = DIMERecord.create(DIMERecord.TYPE_UNCHANGED,
	                               null, null, null, null);
        assertEquals(DIMERecord.TYPE_UNCHANGED, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    // we already tested the network TYPE_UNCHANGED in testCreateFromStream
	    
	    // Now try a bad case -- TYPE_UNCHANGED with a type passed to it.
	    try {
	        record = DIMERecord.create(DIMERecord.TYPE_UNCHANGED,
	                                   null, null, "sam".getBytes(), null);
            fail("expected exception");
        } catch(IllegalArgumentException expected) {
            assertEquals("TYPE_UNCHANGED requires 0 type length",
                         expected.getMessage());
        }
        // Try from network too
        try {
    	    out = new ByteArrayOutputStream();
    	    out.write( 0x08 ); // version + mb, me, cf (all clear)
    	    out.write( DIMERecord.TYPE_UNCHANGED ); // no type + reserved
    	    out.write( new byte[] { 0, 0 } ); // option length: 0
    	    out.write( new byte[] { 0, 0 } ); // id length: 0
    	    ByteOrder.int2beb(3, out, 2); // type length: 3
    	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
    	    out.write( new byte[] { 's', 'a', 'm', 0 } ); // type + padding
    	    in = new ByteArrayInputStream(out.toByteArray());
    	    record = DIMERecord.createFromStream(in);
    	    fail("expected exception.");
        } catch(IOException expected) {
            assertEquals("TYPE_UNCHANGED requires 0 type length",
                         expected.getMessage());
        }
        
        
        // B - TYPE_MEDIA_TYPE
        // Valid always.
        record = DIMERecord.create(DIMERecord.TYPE_MEDIA_TYPE,
                                   null, null, null, null);
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( DIMERecord.TYPE_MEDIA_TYPE ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    out.write( new byte[] { 0, 0 } ); // type length: 0
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    // try with some type data too.
        record = DIMERecord.create(DIMERecord.TYPE_MEDIA_TYPE,
                                   null, null, "sam".getBytes(), null);
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
	    assertEquals(3, record.getType().length);
	    assertEquals("sam", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( DIMERecord.TYPE_MEDIA_TYPE ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    ByteOrder.int2beb(3, out, 2);
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    out.write( new byte[] { 's', 'a', 'm', 0 } ); // type data + padding.
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, record.getTypeId());
	    assertEquals(3, record.getType().length);
	    assertEquals("sam", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    // C - Absolute URI, valid always.
        record = DIMERecord.create(DIMERecord.TYPE_ABSOLUTE_URI,
                                   null, null, null, null);
        assertEquals(DIMERecord.TYPE_ABSOLUTE_URI, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( DIMERecord.TYPE_ABSOLUTE_URI ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    out.write( new byte[] { 0, 0 } ); // type length: 0
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_ABSOLUTE_URI, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    // try with some type data too.
        record = DIMERecord.create(DIMERecord.TYPE_ABSOLUTE_URI,
                                   null, null, "sam".getBytes(), null);
        assertEquals(DIMERecord.TYPE_ABSOLUTE_URI, record.getTypeId());
	    assertEquals(3, record.getType().length);
	    assertEquals("sam", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( DIMERecord.TYPE_ABSOLUTE_URI ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    ByteOrder.int2beb(3, out, 2);
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    out.write( new byte[] { 's', 'a', 'm', 0 } ); // type data + padding.
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_ABSOLUTE_URI, record.getTypeId());
	    assertEquals(3, record.getType().length);
	    assertEquals("sam", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());
	    
	    // D - TYPE_UNKNOWN.
	    // Valid only if the 'type' data is 0 length.
	    record = DIMERecord.create(DIMERecord.TYPE_UNKNOWN,
	                               null, null, null, null);
        assertEquals(DIMERecord.TYPE_UNKNOWN, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());

	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( DIMERecord.TYPE_UNKNOWN ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    out.write( new byte[] { 0, 0 } ); // type length: 0
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_UNKNOWN, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());

	    
	    // Now try a bad case -- TYPE_UNKNOWN with a type passed to it.
	    try {
	        record = DIMERecord.create(DIMERecord.TYPE_UNKNOWN,
	                                   null, null, "sam".getBytes(), null);
            fail("expected exception");
        } catch(IllegalArgumentException expected) {
            assertEquals("TYPE_UNKNOWN requires 0 type length",
                         expected.getMessage());
        }
        // Try from network too
        try {
    	    out = new ByteArrayOutputStream();
    	    out.write( 0x08 ); // version + mb, me, cf (all clear)
    	    out.write( DIMERecord.TYPE_UNKNOWN ); // no type + reserved
    	    out.write( new byte[] { 0, 0 } ); // option length: 0
    	    out.write( new byte[] { 0, 0 } ); // id length: 0
    	    ByteOrder.int2beb(3, out, 2); // type length: 3
    	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
    	    out.write( new byte[] { 's', 'a', 'm', 0 } ); // type + padding
    	    in = new ByteArrayInputStream(out.toByteArray());
    	    record = DIMERecord.createFromStream(in);
    	    fail("expected exception.");
        } catch(IOException expected) {
            assertEquals("TYPE_UNKNOWN requires 0 type length",
                         expected.getMessage());
        }	    
	    
	    
	    // E - TYPE_NONE
	    // Requires no data & no type.
	    record = DIMERecord.create(DIMERecord.TYPE_NONE,
	                               null, null, null, null);
        assertEquals(DIMERecord.TYPE_NONE, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());

	    out = new ByteArrayOutputStream();
	    out.write( 0x08 ); // version + mb, me, cf (all clear)
	    out.write( DIMERecord.TYPE_NONE ); // type
	    out.write( new byte[] { 0, 0 } ); // option length: 0
	    out.write( new byte[] { 0, 0 } ); // id length: 0
	    out.write( new byte[] { 0, 0 } ); // type length: 0
	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	    in = new ByteArrayInputStream(out.toByteArray());
	    record = DIMERecord.createFromStream(in);
        assertEquals(DIMERecord.TYPE_NONE, record.getTypeId());
	    assertEquals(0, record.getType().length);
	    assertEquals("", record.getTypeString());
	    assertEquals(0, record.getData().length);
	    assertEquals(0, record.getId().length);
	    assertEquals(0, record.getOptions().length);
	    assertEquals("", record.getIdentifier());
	    assertEquals(new HashMap(), record.getOptionsMap());

	    
	    // Now try a bad case -- TYPE_NONE with a type passed to it.
	    try {
	        record = DIMERecord.create(DIMERecord.TYPE_NONE,
	                                   null, null, "sam".getBytes(), null);
            fail("expected exception");
        } catch(IllegalArgumentException expected) {
            assertEquals("TYPE_NONE requires 0 type & data length",
                         expected.getMessage());
        }
        // Try from network too
        try {
    	    out = new ByteArrayOutputStream();
    	    out.write( 0x08 ); // version + mb, me, cf (all clear)
    	    out.write( DIMERecord.TYPE_NONE ); // no type + reserved
    	    out.write( new byte[] { 0, 0 } ); // option length: 0
    	    out.write( new byte[] { 0, 0 } ); // id length: 0
    	    ByteOrder.int2beb(3, out, 2); // type length: 3
    	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
    	    out.write( new byte[] { 's', 'a', 'm', 0 } ); // type + padding
    	    in = new ByteArrayInputStream(out.toByteArray());
    	    record = DIMERecord.createFromStream(in);
    	    fail("expected exception.");
        } catch(IOException expected) {
            assertEquals("TYPE_NONE requires 0 type & data length",
                         expected.getMessage());
        }
        
        // Try another bad case -- TYPE_NONE with data passed to it.
	    try {
	        record = DIMERecord.create(DIMERecord.TYPE_NONE,
	                                   null, null, null, "samuel".getBytes());
            fail("expected exception");
        } catch(IllegalArgumentException expected) {
            assertEquals("TYPE_NONE requires 0 type & data length",
                         expected.getMessage());
        }
        // Try from network too
        try {
    	    out = new ByteArrayOutputStream();
    	    out.write( 0x08 ); // version + mb, me, cf (all clear)
    	    out.write( DIMERecord.TYPE_NONE ); // no type + reserved
    	    out.write( new byte[] { 0, 0 } ); // option length: 0
    	    out.write( new byte[] { 0, 0 } ); // id length: 0
    	    out.write( new byte[] { 0, 0 } ); // type length: 0
    	    ByteOrder.int2beb(6, out, 4); // data length: 6
    	    //data + padding
    	    out.write( new byte[] { 's', 'a', 'm', 'u', 'e', 'l', 0, 0 } );
    	    in = new ByteArrayInputStream(out.toByteArray());
    	    record = DIMERecord.createFromStream(in);
    	    fail("expected exception.");
        } catch(IOException expected) {
            assertEquals("TYPE_NONE requires 0 type & data length",
                         expected.getMessage());
        }
    }   
	
	public void testBadType() throws Exception {
	    // 0 through 4 are valid, so start at 5.
	    for(int i = 5; i < 0xF; i++) {
	        int type = i << 4;
    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
    	    out.write( (byte)0x08 ); //version + mb, me & cf.
    	    out.write( (byte)type ); // bad type + reserved
    	    out.write( new byte[] { 0, 0 } ); // option length: 0
    	    out.write( new byte[] { 0, 0 } ); // id length: 0
    	    out.write( new byte[] { 0, 0 } ); // type length: 0
    	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
	        try {
    	        DIMERecord record = DIMERecord.createFromStream(in);
	            fail("expected exception");
            } catch(IOException expected) {
                assertEquals("invalid type: " + i, expected.getMessage());
            }
            
            // try also creating not from network.
            try {
                DIMERecord record = DIMERecord.create((byte)type, null,
                                                      null, null, null);
                fail("expected exception");
            } catch(IllegalArgumentException expected) {
                assertEquals("invalid type: " + i, expected.getMessage());
            }                                                      
        }
    }
	
	public void testBadVersion() throws Exception {
	    //test every invalid version possible.
	    for(int i = 0; i < (0xF8 >> 3); i++) {
	        if(i == 1) // valid
	            continue;
	        int version = i << 3;
    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
    	    out.write( (byte)version ); // mb, me & cf clear.
    	    out.write( 0x00 ); // no type + reserved
    	    out.write( new byte[] { 0, 0 } ); // option length: 0
    	    out.write( new byte[] { 0, 0 } ); // id length: 0
    	    out.write( new byte[] { 0, 0 } ); // type length: 0
    	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
	        try {
    	        DIMERecord record = DIMERecord.createFromStream(in);
	            fail("expected exception");
            } catch(IOException expected) {
                assertEquals("invalid version: " + i, expected.getMessage());
            }
        }
    }
    
    public void testBadReserved() throws Exception {
	    //test every invalid reserved value possible.
	    for(int i = 0; i < 0xF; i++) {
	        if(i == 0) // valid
	            continue;
    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
    	    out.write( 0x08 ); // mb, me & cf clear.
    	    out.write( (byte)i ); // no type + [invalid] reserved
    	    out.write( new byte[] { 0, 0 } ); // option length: 0
    	    out.write( new byte[] { 0, 0 } ); // id length: 0
    	    out.write( new byte[] { 0, 0 } ); // type length: 0
    	    out.write( new byte[] { 0, 0, 0, 0 } ); // data length: 0
	        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
	        try {
    	        DIMERecord record = DIMERecord.createFromStream(in);
	            fail("expected exception");
            } catch(IOException expected) {
                assertEquals("invalid reserved: " + i, expected.getMessage());
            }
        }
    }
}