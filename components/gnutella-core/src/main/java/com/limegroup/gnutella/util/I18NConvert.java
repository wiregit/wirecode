package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ErrorService;
import java.io.IOException;

/**
 * class that handles the removal of accents, etc.
 * Non 118 JVM's will use the icu4j package for 
 * conversion.
 */
public class I18NConvert {

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
            //depeneding on JVM (118 or not)
            if(CommonUtils.isJava118()) {
                _convertDelegator = new I18NConvert118();
            } else {
                _convertDelegator = new I18NConvertICU();
                _convertDelegator.getNorm("touch ICU code");
            }
        }
        catch(IOException te) {
            ErrorService.error(te);
            convertTo118();
        }
        catch(ClassNotFoundException cnf) {
            ErrorService.error(cnf);
            convertTo118();
        }
    }

    private void convertTo118() {
        //if the error was in the ICU code
        //then try to revert to I18NConvert118
        if(!CommonUtils.isJava118()) {
            try {
                _convertDelegator = new I18NConvert118();
            }
            catch(IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }


    /** accessor */
    public static I18NConvert instance() {
        return _instance;
    }

    /** delegate to AbstractI18NConverter instance */
    public String getNorm(String s) {
        return _convertDelegator.getNorm(s);
    }

}



