package com.limegroup.gnutella.util;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.*;

import com.limegroup.gnutella.ErrorService;

import com.ibm.icu.text.Normalizer;


/**
 * class that handles the removal of accents, etc.
 * this class now uses the Normalizer of icu4j
 */
public class I18NConvert {

    /** instance */
    private final static I18NConvert _instance = new I18NConvert();
    
    /** the class that handles the conversion */
    private final AbstractI18NConverter _convertDelegator;

    /**
     * constructor : load in the excluded codepoints and the case map
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

    public String getNorm(String s) {
        return _convertDelegator.getNorm(s);
    }

    public String[] getKeywords(String s) {
        return _convertDelegator.getKeywords(s);
    }
}



