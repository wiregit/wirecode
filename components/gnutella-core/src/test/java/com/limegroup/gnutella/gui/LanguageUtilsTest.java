package com.limegroup.gnutella.gui;

import java.util.List;
import java.util.Locale;

import junit.framework.TestCase;

public class LanguageUtilsTest extends TestCase {

    public void testGetLocalesFromJar() {
        List<Locale> locales = LanguageUtils.getLocalesFromJar("../lib/jars/messages.jar", LanguageUtils.BUNDLE_PREFIX, LanguageUtils.BUNDLE_POSTFIX);
        assertTrue(locales.indexOf(new Locale("sr", "", "Latn")) != -1);
        assertTrue(locales.indexOf(Locale.GERMAN) != -1);
        assertTrue(locales.indexOf(Locale.ENGLISH) == -1);
    }

    public void testGetLocalesFromDisk() {
        List<Locale> locales = LanguageUtils.getLocalesFromDisk();
        assertTrue(locales.indexOf(Locale.GERMAN) != -1);
        assertTrue(locales.indexOf(Locale.ENGLISH) == -1);
    }

}
