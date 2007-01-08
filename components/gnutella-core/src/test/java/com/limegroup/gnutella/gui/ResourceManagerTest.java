package com.limegroup.gnutella.gui;

import java.util.Locale;

import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.util.LimeTestCase;

public class ResourceManagerTest extends LimeTestCase {

	public ResourceManagerTest(String name) {
		super(name);
	}
	
	static void setLocaleSettings(Locale locale) {
		ApplicationSettings.LANGUAGE.setValue(locale.getLanguage());
        ApplicationSettings.COUNTRY.setValue(locale.getCountry());
        ApplicationSettings.LOCALE_VARIANT.setValue(locale.getVariant());
        ResourceManager.resetLocaleOptions();
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
