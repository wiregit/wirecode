package com.limegroup.gnutella.gui;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

public class GUIUtilsTest extends BaseTestCase {

	public GUIUtilsTest(String name) {
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(GUIUtilsTest.class);
    }

	public static void globalSetUp() throws Exception {
		File guiDir = getGUIDir();
		System.setProperty("user.dir", guiDir.getAbsolutePath());
	}
	
	/**
	 * Can only be run for one locale, since GUIUtils caches the
	 * NumberFormat instance which are initialized when the class
	 * is loaded.
	 * @throws IOException 
	 */
	public void testToUnitBytesGerman() throws IOException {
		ResourceManagerTest.setLocaleSettings(Locale.GERMAN);
		assertEquals("1,5 KB", GUIUtils.toUnitbytes(1536));
	}

}
