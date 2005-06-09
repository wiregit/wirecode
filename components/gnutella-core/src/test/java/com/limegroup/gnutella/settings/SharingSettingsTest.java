package com.limegroup.gnutella.settings;

import java.io.File;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.limegroup.gnutella.gui.search.NamedMediaType;
import com.limegroup.gnutella.util.BaseTestCase;

public class SharingSettingsTest extends BaseTestCase {

	public SharingSettingsTest(String name) {
		super(name);
	}
	
	public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(SharingSettingsTest.class);
        return suite;
    }
    
    public void setUp() throws Exception {
        
    }
        
    
    public void tearDown() {
       
    }


	public void testUnsetMediaTypeDirectories() {
		for (Iterator i = NamedMediaType.getAllNamedMediaTypes().iterator();
			i.hasNext();) {
			assertEquals("Should be the save directory", 
					SharingSettings.getSaveDirectory(),
					SharingSettings.getFileSettingForMediaType(
							((NamedMediaType)i.next()).getMediaType()).getValue());
		}
	}
	
	public void testSetMediaTypeDirectories() throws Exception {
		
		File tmpFile = File.createTempFile("prefix", "postfix");
		tmpFile.deleteOnExit();
		File dir = new File(tmpFile.getParentFile(), "subdir");
		dir.mkdir();
		dir.deleteOnExit();
		
		// set all mediatype directories
		for (Iterator i = NamedMediaType.getAllNamedMediaTypes().iterator();
			i.hasNext();) {
			SharingSettings.getFileSettingForMediaType
				(((NamedMediaType)i.next()).getMediaType()).setValue(dir);
		}
		
		// test if they are all set
		for (Iterator i = NamedMediaType.getAllNamedMediaTypes().iterator();
			i.hasNext();) {
			assertEquals("Should be the set directory", 
					dir,
					SharingSettings.getFileSettingForMediaType(
							((NamedMediaType)i.next()).getMediaType()).getValue());
		}
		
		// revert them
		for (Iterator i = NamedMediaType.getAllNamedMediaTypes().iterator();
			i.hasNext();) {
			SharingSettings.getFileSettingForMediaType
			(((NamedMediaType)i.next()).getMediaType()).revertToDefault();
		}
		
		// check if they are reverted
		for (Iterator i = NamedMediaType.getAllNamedMediaTypes().iterator();
			i.hasNext();) {
			assertTrue("Should be default", 
					SharingSettings.getFileSettingForMediaType(
							((NamedMediaType)i.next()).getMediaType()).isDefault());
			assertEquals("Should be the save directory", 
					SharingSettings.getSaveDirectory(),
					SharingSettings.getFileSettingForMediaType(
							((NamedMediaType)i.next()).getMediaType()).getValue());
		}
	}
	
}
