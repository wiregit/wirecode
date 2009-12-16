package com.limegroup.gnutella.settings;

import java.io.File;

import junit.framework.Test;

import org.limewire.core.api.Category;
import org.limewire.core.settings.SharingSettings;
import org.limewire.gnutella.tests.LimeTestCase;


public class SharingSettingsTest extends LimeTestCase {

	public SharingSettingsTest(String name) {
		super(name);
	}
	
	public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(SharingSettingsTest.class);
    }

    /**
     * Tests if all settings are set to the default save directory.
     */
	public void testUnsetMediaTypeDirectories() {
		Category[] categories = Category.values();
		for (int i = 0; i < categories.length; i++) {
			assertEquals("Should be the save directory", 
					SharingSettings.getSaveDirectory( null),
					SharingSettings.getFileSettingForCategory(categories[i]).get());
							
		}
	}
	
	public void testSetMediaTypeDirectories() throws Exception {
		
		File tmpFile = File.createTempFile("prefix", "postfix");
		tmpFile.deleteOnExit();
		File dir = new File(tmpFile.getParentFile(), "subdir");
		dir.mkdir();
		dir.deleteOnExit();
		
		// set all mediatype directories
		Category[] types = Category.values();
		for (int i = 0; i < types.length; i++) {
			SharingSettings.getFileSettingForCategory(types[i]).set(dir);
		}
		
		// test if they are all set
		for (int i = 0; i < types.length; i++) {
			assertEquals("Should be the set directory", 
					dir,
					SharingSettings.getFileSettingForCategory(types[i]).get());
		}
		
		// revert them
		for (int i = 0; i < types.length; i++) {
			SharingSettings.getFileSettingForCategory(types[i]).revertToDefault();
		}
		
		// check if they are reverted
		for (int i = 0; i < types.length; i++) {
			assertTrue("Should be default", 
					SharingSettings.getFileSettingForCategory(
							types[i]).isDefault());
			assertEquals("Should be the save directory", 
					SharingSettings.getSaveDirectory(null),
					SharingSettings.getFileSettingForCategory(types[i]).get());
		}
	}
}
