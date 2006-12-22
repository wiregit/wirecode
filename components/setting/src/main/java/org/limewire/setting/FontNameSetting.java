package org.limewire.setting;

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

    FontNameSetting(Properties defaultProps, Properties props, String key,
                                                           String defaultStr) {
        super(defaultProps, props, key, defaultStr);
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
        _fontName = sValue;
    }
}
