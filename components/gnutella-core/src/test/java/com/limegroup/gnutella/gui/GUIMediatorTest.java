package com.limegroup.gnutella.gui;

import java.util.Locale;

import junit.framework.TestSuite;

import org.limewire.util.BaseTestCase;

public class GUIMediatorTest extends BaseTestCase {

    public GUIMediatorTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(GUIMediatorTest.class);
    }
    
    public void testIsEnglishLocale() {
        assertTrue(GUIMediator.isEnglishLocale(Locale.ENGLISH));
        assertTrue(GUIMediator.isEnglishLocale(Locale.CANADA));
        assertTrue(GUIMediator.isEnglishLocale(Locale.US));
        assertTrue(GUIMediator.isEnglishLocale(Locale.UK));
        assertFalse(GUIMediator.isEnglishLocale(Locale.GERMAN));
    }
    
}
