package com.limegroup.gnutella;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * Unit tests for MediaType
 */
public class MediaTypeTest extends BaseTestCase {
    
	public MediaTypeTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(MediaTypeTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public void testLegacy() throws Exception {
        MediaType[] types = MediaType.getDefaultMediaTypes();
        MediaType mt;
    
        mt = types[0]; /* SCHEMA_ANY_TYPE */
        assertTrue(mt.matches("foo.jpg"));
        assertTrue(mt.matches("foo"));
        assertTrue(mt.matches(""));
    
        mt = types[1]; /* SCHEMA_DOCUMENTS */
        assertEquals(getSchemaDocuments(), mt.toString());
        assertTrue(mt.matches("foo.html"));
        assertTrue(mt.matches("foo.HTML"));
        assertTrue(mt.matches("foo.ps"));
        assertTrue(mt.matches("foo.PS"));
        assertFalse(mt.matches("foo.jpg"));
        assertFalse(mt.matches("foo"));
        assertFalse(mt.matches("foo."));
    }
    
    private static String getSchemaDocuments() throws Exception {
        return (String)PrivilegedAccessor.getValue(MediaType.class,
                "SCHEMA_DOCUMENTS");
    }
}    