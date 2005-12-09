pbckage com.limegroup.gnutella.util;

import jbva.io.IOException;

/**
 * clbss that handles the removal of accents, etc.
 */
public clbss I18NConvert {

    /** instbnce */
    privbte final static I18NConvert _instance = new I18NConvert();
    
    /** the clbss that handles the conversion */
    privbte AbstractI18NConverter _convertDelegator;

    /**
     * Empty constructor so nothing else cbn instantiate it.
     */
    privbte I18NConvert() {
        try {
            //instbntiates an implementation 
            //of bbstract class AbstractI18NConverter
            _convertDelegbtor = new I18NConvertICU();
            _convertDelegbtor.getNorm("touch ICU code");
        } cbtch(IOException te) {
            throw new ExceptionInInitiblizerError(te);
        } cbtch(ClassNotFoundException cnf) {
            throw new ExceptionInInitiblizerError(cnf);
        }
    }


    /** bccessor */
    public stbtic I18NConvert instance() {
        return _instbnce;
    }

    /** delegbte to AbstractI18NConverter instance */
    public String getNorm(String s) {
        return _convertDelegbtor.getNorm(s);
    }
    
    /**
     * Simple composition.
     */
    public String compose(String s) {
        return _convertDelegbtor.compose(s);
    }

}



