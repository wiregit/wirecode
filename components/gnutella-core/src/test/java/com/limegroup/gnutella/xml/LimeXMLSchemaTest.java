package com.limegroup.gnutella.xml;

import java.io.File;
import java.util.Iterator;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.Expand;


/**
 * Unit tests for LimeXMLSchema
 */
public class LimeXMLSchemaTest extends BaseTestCase {
            
	public LimeXMLSchemaTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LimeXMLSchemaTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void setUp() throws Exception {
	    Expand.expandFile(
            CommonUtils.getResourceFile("com/limegroup/gnutella/xml/xml.war"), 
            CommonUtils.getUserSettingsDir()
        );
    }
	
	/**
	 * Tests getCanonicalizedFieldName and SchemaFieldInfo.<p>
	 *
	 * This test is much stricter than necessary, but there's
	 * really no other way to test it.  If the xsd file changes 
	 * in any way (order, addition, removal), this test will fail.
	 * Alter the checks to fix it.
	 */
    public static void testGetFieldNames() throws Exception {
        LimeXMLSchema schema = new LimeXMLSchema(new File(
            LimeXMLProperties.instance().getXMLSchemaDir() 
            + File.separator
            + "audio.xsd"));
        
        //get the fields and print those
        Iterator iterator = schema.getCanonicalizedFields().iterator();
        
        assertTrue( iterator.hasNext() );
        
        check( iterator, "title");
        check( iterator, "artist");
        check( iterator, "album");
        check( iterator, "track");
        check( iterator, "genre");
        check( iterator, "type");
        check( iterator, "seconds");
        check( iterator, "year");
        check( iterator, "language");
        check( iterator, "SHA1");
        check( iterator, "bitrate");
        check( iterator, "price");
        check( iterator, "link");
        check( iterator, "comments");
        check( iterator, "action");
        
        assertTrue( !iterator.hasNext() );
    }
    
    private static void check( Iterator i, String name) {
        String fullName = "audios__audio__" + name + "__";
        
        assertTrue(i.hasNext());
        SchemaFieldInfo fieldInfo = (SchemaFieldInfo)i.next();
        assertEquals(fullName, fieldInfo.getCanonicalizedFieldName());
    }
}	
