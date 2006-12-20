package com.limegroup.gnutella.gui;

import java.io.IOException;
import java.util.Locale;

import junit.framework.Test;

import com.limegroup.gnutella.util.GUIBaseTestCase;

public class GUIUtilsTest extends GUIBaseTestCase {

	public GUIUtilsTest(String name) {
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(GUIUtilsTest.class);
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
