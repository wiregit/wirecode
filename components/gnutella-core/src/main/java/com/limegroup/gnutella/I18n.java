package com.limegroup.gnutella;

import java.util.Locale;
import java.util.ResourceBundle;

import org.xnap.commons.i18n.I18nFactory;

public class I18n {

    private static final org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(I18n.class, "Messages");
    
    public static final String tr(String text) {
        return i18n.tr(text);
    }
    
    public static final String tr(String text, Object... args) {
        return i18n.tr(text, args);
    }
    
    public static final String trn(String singularText, String pluralText, long number) {
        return i18n.trn(singularText, pluralText, number);
    }
    
    public static final String trn(String singularText, String pluralText, long number, Object...args) {
        return i18n.trn(singularText, pluralText, number, args);
    }

    public static ResourceBundle getResources() {
        return i18n.getResources();
    }

    public static boolean setLocale(Locale arg0) {
        return i18n.setLocale(arg0);
    }

    public static void setResources(ResourceBundle arg0) {
        i18n.setResources(arg0);
    }

    public static void setResources(String arg0, Locale arg1, ClassLoader arg2) {
        i18n.setResources(arg0, arg1, arg2);
    }

    public static void setSourceCodeLocale(Locale arg0) {
        i18n.setSourceCodeLocale(arg0);
    }
    
    public static String marktr(String text) {
        return text;
    }
}
