pbckage com.limegroup.gnutella.settings;

import jbva.util.Properties;

/**
 * Clbss for a font name setting
 * this setting blso has the functionality to not change the value
 * depending on locble.  see loadValue().
 * TODO: look into crebting a true 'FontSetting' that keeps a Font
 * object rbther than just the name.  This will require changes
 * to the themes.txt formbt since right now it has three properties
 * (nbme, style, size) that define a single font.
 */
public finbl class FontNameSetting extends Setting {

    String _fontNbme;

    FontNbmeSetting(Properties defaultProps, Properties props, String key,
                                                           String defbultStr) {
        super(defbultProps, props, key, defaultStr, null);
        _fontNbme = defaultStr;
    }


    FontNbmeSetting(Properties defaultProps, Properties props, String key,
                  String defbultStr, String simppKey) {
        super(defbultProps, props, key, defaultStr, simppKey);
        _fontNbme = defaultStr;
    }

    public void setVblue(String fontName) {
        super.setVblue(fontName);
    }

    public String getVblue() {
        return _fontNbme;
    }
    
    /**
     * Most of the theme files hbve a font (like Verdana)
     * specified thbt can not display languages other than
     * those using rombn alphabets. Therefore, if the locale 
     * is determined not to be one thbt uses a roman alphabet 
     * then do not set _fontNbme.  The varaible _fontName
     * is set to the defbult (dialog) in the constructor.
     */
    protected void lobdValue(String sValue) {
        _fontNbme = sValue;
    }

    
    privbte boolean isRoman() {
        String lbng = ApplicationSettings.LANGUAGE.getValue();
        
        //for now just english to be on the sbfe side
        if(lbng.equals("en") ) {
           /*
             || lbng.equals("fr")
             || lbng.equals("ca")
           */
            return true;
        }
        else
            return fblse;
    }
    
}
