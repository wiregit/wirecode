package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ErrorService;

/**
 * class that handles the removal of accents, etc.
 * Non 118 JVM's will use the icu4j package for 
 * conversion.
 */
public class I18NConvert {

    /** instance */
    private final static I18NConvert _instance = new I18NConvert();
    
    /** the class that handles the conversion */
    private static AbstractI18NConverter _convertDelegator;
    static {
        //instantiates a implementation of abstract class AbstractI18NConverter
        //depeneding on JVM (118 or not)
        if(CommonUtils.isJava118()) {
            _convertDelegator = new I18NConvert118();
        } else {
            _convertDelegator = new I18NConvertICU();
        }
        
        initializeDel();
    }

    /**
     * calls initialize on _convertDelegator.  If the initialization
     * failes, the exception will be 'ErrorServic'ed and _convertDelegator
     * will be changed.
     */
    private static void initializeDel() {
        try {
            _convertDelegator.initialize();
            //try a simple getNorm to make sure it works
            _convertDelegator.getNorm("touch ICU code");
        }
        catch(Throwable e) { 
            ErrorService.error(e);
            changeDel();
        }
    }

    /**
     * changes the _convertDelegator.  If the _convertDelegator 
     * which failed (due to failure in initialization or internally
     * in ICU) is I18NConvertICU then we try to load in I18NConvert118. 
     */
    private static void changeDel() {
        if(_convertDelegator.getClass() == I18NConvertICU.class) {
            _convertDelegator = new I18NConvert118();
            initializeDel();
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

}



