package com.limegroup.gnutella;

import junit.framework.*;
import com.limegroup.gnutella.util.BaseTestCase;
import com.sun.java.util.collections.*;
import java.io.*;

/**
 * Unit tests for ByteReader
 */
public class ByteReaderTest extends BaseTestCase {
    
	public ByteReaderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ByteReaderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() throws Exception {
		InputStream in;
		ByteReader bin;
		String s;       
	
		in=new StringBufferInputStream("abc\r\na\rbc\n");
		bin=new ByteReader(in);
	
		s=bin.readLine(); assertEquals("abc", s);
		s=bin.readLine(); assertEquals("abc", s);
		s=bin.readLine(); assertNull(s);
		s=bin.readLine(); assertNull(s);
	
		in=new StringBufferInputStream("a\ne");
		bin=new ByteReader(in);
	
		s=bin.readLine(); assertEquals("a", s);
		s=bin.readLine(); assertNull(s);  
	}
	
}