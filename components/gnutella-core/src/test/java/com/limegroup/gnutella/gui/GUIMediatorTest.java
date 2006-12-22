package com.limegroup.gnutella.gui;

import java.util.Locale;

import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.util.LimeTestCase;

public class GUIMediatorTest extends LimeTestCase {

	public GUIMediatorTest(String name) {
		super(name);
	}

	/**
	 * Sets the locale.
	 * @param locale
	 */
	static void setLocaleSettings(Locale locale) {
		ApplicationSettings.LANGUAGE.setValue(locale.getLanguage());
        ApplicationSettings.COUNTRY.setValue(locale.getCountry());
        ApplicationSettings.LOCALE_VARIANT.setValue(locale.getVariant());
        GUIMediator.resetLocale();
	}
	
	public void testResetLocale() {
		setLocaleSettings(Locale.US);
		assertEquals(Locale.US,GUIMediator.getLocale());
		setLocaleSettings(Locale.GERMAN);
		assertEquals(Locale.GERMAN, GUIMediator.getLocale());
		setLocaleSettings(Locale.GERMANY);
		assertEquals(Locale.GERMANY, GUIMediator.getLocale());
	}
	
}
