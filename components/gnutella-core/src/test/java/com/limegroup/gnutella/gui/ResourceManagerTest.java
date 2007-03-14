package com.limegroup.gnutella.gui;

import java.util.Locale;


public class ResourceManagerTest extends GUIBaseTestCase {

	public ResourceManagerTest(String name) {
		super(name);
	}
	
	public void testResetLocaleOptions() {
		setLocaleSettings(Locale.US);
		assertEquals(Locale.US, ResourceManager.getLocale());
		setLocaleSettings(Locale.GERMAN);
		assertEquals(Locale.GERMAN, ResourceManager.getLocale());
		setLocaleSettings(Locale.GERMANY);
		assertEquals(Locale.GERMANY, ResourceManager.getLocale());
	}

}
