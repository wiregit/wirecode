package com.limegroup.gnutella.util;

/**
 * class that handles the removal of accents, etc.
 * Non 118 JVM's will use the icu4j package for 
 * conversion.
 */
public class I18NConvert {

    /** instance */
    private final static I18NConvert _instance = new I18NConvert();
    
    /** the class that handles the conversion */
    private static final AbstractI18NConverter _convertDelegator;
    static {
        //instantiates a implementation of abstract class AbstractI18NConverter
        //depeneding on JVM (118 or not)
        if(CommonUtils.isJava118()) {
            _convertDelegator = new I18NConvert118();
        } else {
            _convertDelegator = new I18NConvertICU();
        }
    }
    
    /**
     * Empty constructor so nothing else can instantiate it.
     */
    private I18NConvert() {}

    /** accessor */
    public static I18NConvert instance() {
        return _instance;
    }

    /** delegate to AbstractI18NConverter instance */
    public String getNorm(String s) {
        return _convertDelegator.getNorm(s);
    }

    /** delegate to AbstractI18NConverter instance */
    public String[] getKeywords(String s) {
        return _convertDelegator.getKeywords(s);
    }
}



