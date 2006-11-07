package com.limegroup.gnutella.gui;

import java.text.NumberFormat;
import java.util.Locale;

import com.limegroup.gnutella.util.BaseTestCase;

public class GUIUtilsTest extends BaseTestCase {

	public GUIUtilsTest(String name) {
		super(name);
	}
	
	public void testNumberformatField() {
		GUIMediatorTest.setLocaleSettings(Locale.US);
		NumberFormat format = NumberFormat.getInstance(GUIMediator.getLocale());
		assertEquals("20.1", format.format(20.1));
		assertEquals("1,000", format.format(1000));
		assertEquals("1,000.99", format.format(1000.99));
		GUIMediatorTest.setLocaleSettings(Locale.GERMAN);
		format = NumberFormat.getInstance(GUIMediator.getLocale());
		assertEquals("20,1", format.format(20.1));
		assertEquals("1.000", format.format(1000));
		assertEquals("1.000,99", format.format(1000.99));
		GUIMediatorTest.setLocaleSettings(Locale.GERMANY);
		format = NumberFormat.getInstance(GUIMediator.getLocale());
		assertEquals("20,1", format.format(20.1));
		assertEquals("1.000", format.format(1000));
		assertEquals("1.000,99", format.format(1000.99));
	}
	
	/**
	 * Can only be run for one locale, since GUIUtils caches the
	 * NumberFormat instance which are initialized when the class
	 * is loaded.
	 */
	public void testToUnitBytesGerman() { 
		GUIMediatorTest.setLocaleSettings(Locale.GERMAN);
		assertEquals("1,5 KB", GUIUtils.toUnitbytes(1536));
	}

}
