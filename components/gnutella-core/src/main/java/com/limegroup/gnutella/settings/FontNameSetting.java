package com.limegroup.gnutella.settings;

import java.util.Properties;

/**
 * Class for a font name setting
 * this setting also has the functionality to not change the value
 * depending on locale.  see loadValue().
 * TODO: look into creating a true 'FontSetting' that keeps a Font
 * object rather than just the name.  This will require changes
 * to the themes.txt format since right now it has three properties
 * (name, style, size) that define a single font.
 */
public final class FontNameSetting extends Setting {

    String _fontName;

    public static final String DUMMY_FONT_SETTING = "";

    FontNameSetting(Properties defaultProps, Properties props, String key,
                                                           String defaultStr) {
        super(defaultProps, props, key, defaultStr, null, null, null);
        _fontName = defaultStr;
    }


    FontNameSetting(Properties defaultProps, Properties props, String key,
                  String defaultStr, String simppKey, String max, String min) {
        super(defaultProps, props, key, defaultStr, simppKey, max, min);
        if(max != DUMMY_FONT_SETTING || min != DUMMY_FONT_SETTING)
            throw new IllegalArgumentException("illegal max or min in setting");
        _fontName = defaultStr;
    }

    public void setValue(String fontName) {
        super.setValue(fontName);
    }

    public String getValue() {
        return _fontName;
    }
    
    /**
     * Most of the theme files have a font (like Verdana)
     * specified that can not display languages other than
     * those using roman alphabets. Therefore, if the locale 
     * is determined not to be one that uses a roman alphabet 
     * then do not set _fontName.  The varaible _fontName
     * is set to the default (dialog) in the constructor.
     */
    protected void loadValue(String sValue) {
        if(isRoman()) 
            _fontName = sValue;
    }

    
    private boolean isRoman() {
        String lang = ApplicationSettings.LANGUAGE.getValue();
        
        //for now just english to be on the safe side
        if(lang.equals("en") ) {
           /*
             || lang.equals("fr")
             || lang.equals("ca")
           */
            return true;
        }
        else
            return false;
    }
    
    protected boolean isInRange(String value) {
        //No illegal ranges for file arrays. Just return true
        return true;
    }    

}
