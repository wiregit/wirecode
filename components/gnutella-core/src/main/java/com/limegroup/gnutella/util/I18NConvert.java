package com.limegroup.gnutella.util;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.*;

import com.limegroup.gnutella.ErrorService;

import com.ibm.icu.text.Normalizer;


/**
 * class that handles the removal of accents, etc.
 * Non 118 JVM's will use the icu4j package for 
 * conversion.
 */
public class I18NConvert {

    /** instance */
    private final static I18NConvert _instance = new I18NConvert();
    
    /** the class that handles the conversion */
    private final AbstractI18NConverter _convertDelegator;

    /**
     * constructor : 
     * instantiates a implementation of abstract class AbstractI18NConverter
     * depeneding on JVM (118 or not)
     */
    private I18NConvert() {
        if(CommonUtils.isJava118()) 
            _convertDelegator = new I18NConvert118();
        else
            _convertDelegator = new I18NConvertICU();
    }

    /** accesor */
    public static I18NConvert instance() {
        return _instance;
    }

    /** delgate to AbstractI18NConverter intance */
    public String getNorm(String s) {
        return _convertDelegator.getNorm(s);
    }

    /** delgate to AbstractI18NConverter intance */
    public String[] getKeywords(String s) {
        return _convertDelegator.getKeywords(s);
    }
}



