package com.limegroup.gnutella.gui;

import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

public class LanguageUtilsTest extends BaseTestCase {

    public LanguageUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LanguageUtilsTest.class);
    }
    
    public void testAddLocalesFromJar() {
        List<Locale> locales = new ArrayList<Locale>(); 
        File jar = FileUtils.getJarFromClasspath("org/limewire/i18n/Messages.class");
        LanguageUtils.addLocalesFromJar(locales, jar );
        assertTrue(locales.indexOf(new Locale("sr", "", "Latn")) != -1);
        assertTrue(locales.indexOf(Locale.GERMAN) != -1);
        assertTrue(locales.indexOf(Locale.ENGLISH) == -1);
    }

    public void testGetLocales() {
        List<Locale> locales = Arrays.asList(LanguageUtils.getLocales(new Font("Dialog", Font.PLAIN, 0)));
        assertFalse(locales.isEmpty());
        assertEquals(Locale.ENGLISH, locales.get(0));
        assertTrue(locales.indexOf(new Locale("sr", "", "Latn")) != -1);
        assertTrue(locales.indexOf(Locale.GERMAN) != -1);
    }

}
