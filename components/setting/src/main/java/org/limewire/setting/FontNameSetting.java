package org.limewire.setting;

import java.util.Properties;


/**
 * Contains a font name value key-value pair.
 * <p>
 * Create a <code>FontNameSetting</code> object with a {@link SettingsFactory}.
 * For example, with a file font.txt without the key ARIAL included:
<pre>
        File f = new File("font.txt");
        SettingsFactory sf = new SettingsFactory(f);
        FontNameSetting fn = sf.createRemoteFontNameSetting("key", 
                                                    "defaultValue", 
                                                        "remoteKey");

        FontNameSetting font = sf.createRemoteFontNameSetting("ARIAL", 
                                                       "defaultValue", 
                                                       "ARIAL_REMOTE");
        System.out.println(font.getValue());
        font.setValue("Arial");
        System.out.println(font.getValue());

    Output:
        defaultValue
        Arial

With the change of the value from defaultValue to Arial, font.txt now includes:
        ARIAL=Arial
 </pre>
 * 
 */

 /* TODO: look into creating a true 'FontSetting' that keeps a Font
 * object rather than just the name.  This will require changes
 * to the themes.txt format since right now it has three properties
 * (name, style, size) that define a single font.
 */
public final class FontNameSetting extends Setting {
   
    private String _fontName;

    FontNameSetting(Properties defaultProps, Properties props, String key,
                                                           String defaultStr) {
        super(defaultProps, props, key, defaultStr);
        _fontName = defaultStr;
    }
    /**
      * @param fontName
      */
    public void setValue(String fontName) {
        super.setValue(fontName);
    }

    public String getValue() {
        return _fontName;
    }
    
    /**
     * Most of the theme files have a font (like Verdana)
     * specified that can not display languages other than
     * those using Roman alphabets. Therefore, if the locale 
     * is determined not to be one that uses a Roman alphabet 
     * then do not set _fontName.  The variable _fontName
     * is set to the default (dialog) in the constructor.
     */
    protected void loadValue(String sValue) {
        _fontName = sValue;
    }
}
