package com.limegroup.gnutella.dime;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import com.sun.java.util.collections.HashMap;

import junit.framework.Test;

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