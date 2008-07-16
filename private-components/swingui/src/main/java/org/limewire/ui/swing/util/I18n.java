package org.limewire.ui.swing.util;

import java.util.Locale;

public class I18n {

 //   private static final String BASENAME = "org.limewire.i18n.Messages";
    //TODO I18nFactory.getI18n(I18n.class, BASENAME);
   // private static org.xnap.commons.i18n.I18n i18n;// = I18nFactory.getI18n(I18n.class, BASENAME);
    
    public static final void setLocale(Locale locale) {
      //  i18n.setResources(BASENAME, locale, ClassLoader.getSystemClassLoader());
    }    
    
    public static final String tr(String text) {
    	//TODO
    	return text;
        //return i18n.tr(text);
    }

    /**
     * Returns the translation of a text in the given locale if available.
     * 
     * This allows you to look up a translation for a specific locale. Should be 
     * used with care since the whole hierarchy for the message bundle might be loaded.
     * 
     * @param text the text to translate
     * @param locale the locale to look up the translation for
     */
    public static final String trl(String text, Locale locale) {
    	//TODO
    	return text;
//        org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(I18n.class, BASENAME, locale, I18nFactory.NO_CACHE);
//        return i18n.tr(text);
    }

    public static final String tr(String text, Object... args) {
    	//TODO    	
    	for(int i = 0; i< args.length; i++){
    		text = text.replace("{"+i+"}", args[i].toString());
    	}
    	return text;
        //return i18n.tr(text.replace("'", "''"), args);
    }

    public static final String trc(String comment, String text) {
        //TODO
        return text;
      //  return i18n.trc(comment, text);
    }
    
    public static final String trn(String singularText, String pluralText, long number) {
        //TODO
        return tr(number>1 ? pluralText : singularText, number);
        //return i18n.trn(singularText, pluralText, number);
    }

    public static final String trn(String singularText, String pluralText, long number, Object...args) {
        //TODO
        return tr(trn(singularText,pluralText, number), args);
       // return i18n.trn(singularText.replace("'", "''"), pluralText.replace("'", "''"), number, args);
    }


}
