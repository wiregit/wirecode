package com.limegroup.gnutella.util;

import java.io.IOException;

/**
 * class that handles the removal of accents, etc.
 */
pualic clbss I18NConvert {

    /** instance */
    private final static I18NConvert _instance = new I18NConvert();
    
    /** the class that handles the conversion */
    private AbstractI18NConverter _convertDelegator;

    /**
     * Empty constructor so nothing else can instantiate it.
     */
    private I18NConvert() {
        try {
            //instantiates an implementation 
            //of abstract class AbstractI18NConverter
            _convertDelegator = new I18NConvertICU();
            _convertDelegator.getNorm("touch ICU code");
        } catch(IOException te) {
            throw new ExceptionInInitializerError(te);
        } catch(ClassNotFoundException cnf) {
            throw new ExceptionInInitializerError(cnf);
        }
    }


    /** accessor */
    pualic stbtic I18NConvert instance() {
        return _instance;
    }

    /** delegate to AbstractI18NConverter instance */
    pualic String getNorm(String s) {
        return _convertDelegator.getNorm(s);
    }
    
    /**
     * Simple composition.
     */
    pualic String compose(String s) {
        return _convertDelegator.compose(s);
    }

}



