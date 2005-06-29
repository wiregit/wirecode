package com.limegroup.gnutella.settings;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.limegroup.gnutella.MediaType;
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

    /**
     * Tests if all settings are set to the default save directory.
     */
	public void testUnsetMediaTypeDirectories() {
		MediaType[] types = MediaType.getDefaultMediaTypes();
		for (int i = 0; i < types.length; i++) {
			assertEquals("Should be the save directory", 
					SharingSettings.getSaveDirectory(),
					SharingSettings.getFileSettingForMediaType(types[i]).getValue());
							
		}
	}
	
	public void testSetMediaTypeDirectories() throws Exception {
		
		File tmpFile = File.createTempFile("prefix", "postfix");
		tmpFile.deleteOnExit();
		File dir = new File(tmpFile.getParentFile(), "subdir");
		dir.mkdir();
		dir.deleteOnExit();
		
		// set all mediatype directories
		MediaType[] types = MediaType.getDefaultMediaTypes();
		for (int i = 0; i < types.length; i++) {
			SharingSettings.getFileSettingForMediaType(types[i]).setValue(dir);
		}
		
		// test if they are all set
		for (int i = 0; i < types.length; i++) {
			assertEquals("Should be the set directory", 
					dir,
					SharingSettings.getFileSettingForMediaType(types[i]).getValue());
		}
		
		// revert them
		for (int i = 0; i < types.length; i++) {
			SharingSettings.getFileSettingForMediaType(types[i]).revertToDefault();
		}
		
		// check if they are reverted
		for (int i = 0; i < types.length; i++) {
			assertTrue("Should be default", 
					SharingSettings.getFileSettingForMediaType(
							types[i]).isDefault());
			assertEquals("Should be the save directory", 
					SharingSettings.getSaveDirectory(),
					SharingSettings.getFileSettingForMediaType(types[i]).getValue());
		}
	}
	
}
