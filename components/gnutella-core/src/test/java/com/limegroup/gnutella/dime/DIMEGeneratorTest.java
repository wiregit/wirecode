package com.limegroup.gnutella.dime;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import junit.framework.Test;

/**
 * Tests for DIMEGenerator.
 */
public final class DIMEGeneratorTest extends com.limegroup.gnutella.util.BaseTestCase {

	/**
	 * Constructs a new test instance for responses.
	 */
	public DIMEGeneratorTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DIMEGeneratorTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testWrite() throws Exception {
	    DIMEGenerator gen = new DIMEGenerator();
	    
	    DIMERecord one =
	        DIMERecord.create(DIMERecord.TYPE_UNCHANGED,
                              null, null, null, "one".getBytes());
        DIMERecord two =
            DIMERecord.create(DIMERecord.TYPE_MEDIA_TYPE,
                              null, null, null, "two".getBytes());
        DIMERecord three =
            DIMERecord.create(DIMERecord.TYPE_ABSOLUTE_URI,
                              null, null, null, "three".getBytes());

        gen.add(one);
        gen.add(two);
        gen.add(three);
        
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        gen.write(out);
        
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        DIMERecord readOne = DIMERecord.createFromStream(in);
        DIMERecord readTwo = DIMERecord.createFromStream(in);
        DIMERecord readThree = DIMERecord.createFromStream(in);
        assertEquals(-1, in.read());
        
        assertEquals(DIMERecord.TYPE_UNCHANGED, readOne.getTypeId());
        assertEquals(DIMERecord.TYPE_MEDIA_TYPE, readTwo.getTypeId());
        assertEquals(DIMERecord.TYPE_ABSOLUTE_URI, readThree.getTypeId());
        
        assertEquals("one".getBytes(), readOne.getData());
        assertEquals("two".getBytes(), readTwo.getData());
        assertEquals("three".getBytes(), readThree.getData());
        
        assertTrue(readOne.isFirstRecord());
        assertTrue(readThree.isLastRecord());
        
        assertFalse(readOne.isLastRecord());
        assertFalse(readTwo.isFirstRecord());
        assertFalse(readTwo.isLastRecord());
        assertFalse(readThree.isFirstRecord());
    }
    
    public void testWriteWithOne() throws Exception {
        DIMEGenerator gen = new DIMEGenerator();
        DIMERecord one = DIMERecord.create(DIMERecord.TYPE_UNCHANGED,
                                           null, null, null, null);
        gen.add(one);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        gen.write(out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        DIMERecord readOne = DIMERecord.createFromStream(in);
        assertEquals(-1, in.read());
        assertTrue(readOne.isFirstRecord());
        assertTrue(readOne.isLastRecord());
    }
}