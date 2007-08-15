package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Unit tests for MediaType
 */
public class MediaTypeTest extends LimeTestCase {
    
    
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
    
    public void testEquals() {
        for (MediaType type : MediaType.getDefaultMediaTypes()) {
            assertTrue(type.equals(type));
        }
        assertFalse(MediaType.getAnyTypeMediaType().equals(MediaType.getAudioMediaType()));
        assertFalse(MediaType.getAudioMediaType().equals(MediaType.getAnyTypeMediaType()));
        
        assertFalse(MediaType.getDocumentMediaType().equals(MediaType.getImageMediaType()));
        
        // self constructed types:
        
        MediaType type = new MediaType("self");
        
        assertTrue(type.equals(type));
        for (MediaType defaultType : MediaType.getDefaultMediaTypes()) {
            assertFalse(defaultType.equals(type));
        }
    }
    
    public void testCanonicalizationOfDefaultTypesWhenDeserializing() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        
        for (MediaType type : MediaType.getDefaultMediaTypes()) {
            out.writeObject(type);
        }
        out.flush();
        
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        
        for (MediaType type : MediaType.getDefaultMediaTypes()) {
            MediaType read = (MediaType)in.readObject();
            assertSame(type, read);
        }
    }
}    