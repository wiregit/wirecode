package com.limegroup.gnutella.i18n;

import java.util.Properties;

/**
 * Struct-like container for language information.
 */
class LanguageInfo implements Comparable {
    
    private final String languageCode;
    private final String countryCode;
    private final String variantCode;
    private final String scriptCode;
    private final String languageName;
    private final String countryName;
    private final String variantName;
    private final String scriptName;
    private boolean isRightToLeft;
    private final String displayName;
    private final String fileName;
    private final Properties properties;
    private double percentage;
    
    /**
     * Constructs a new LanguageInfo object with the given
     * languageCode, countryCode, variantCode,
     * languageName, countryName, and variantName.
     */
    public LanguageInfo(String lc, String cc, String vc, String sc,
                        String ln, String cn, String vn, String sn,
                        String dn, boolean rtl,
                        String fn, Properties props) {
        languageCode = lc.trim();
        countryCode  = cc.trim();
        variantCode  = vc.trim();
        scriptCode   = sc.trim();
        languageName = ln.trim();
        countryName  = cn.trim();
        variantName  = vn.trim();
        scriptName   = sn.trim();
        isRightToLeft = rtl;
        displayName  = dn.trim();
        fileName     = fn.trim();
        properties   = props;
    }
    
    /**
     * Used to map the list of locales codes to their LanguageInfo data and props.
     * Must be unique per loaded localized properties file.
     */
    public int compareTo(Object other) {
        final LanguageInfo o = (LanguageInfo)other;
        int comp = languageCode.compareTo(o.languageCode);
        if (comp != 0)
            return comp;
        comp = countryCode.compareTo(o.countryCode);
        if (comp != 0)
            return comp;
        return variantCode.compareTo(o.variantCode);
    }
    
    public boolean isVariant() {
        return !"".equals(variantCode) || !"".equals(countryCode);
    }
    
    public String getBaseCode() {
        return languageCode;
    }
    
    public String getCode() {
        if (!variantCode.equals(""))
            return languageCode + "_" + countryCode + "_" + variantCode;
        if (!countryCode.equals(""))
            return languageCode + "_" + countryCode;
        return languageCode;
    }
    
    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
    
    public double getPercentage() {
        return percentage;
    }
    
    public Properties getProperties() {
       return properties;
    }

    /**
     * Returns a native description of this language.
     * If the variantName is not 'international' or '', then 
     * the display is:
     *    languageName, variantName (countryName)
     * Otherwise, the display is:
     *    languageName (countryName)
     * If the language is Right-To-Left, the whole string is returned
     * surrounded by BiDi embedding controls.
     */
    public String toString() {
        final String bidi1, bidi2;
        if (isRightToLeft) {
            bidi1 = "\u202b"; /* RLE control: Right-To-Left Embedding */
            bidi2 = "\u202c"; /* PDF control: Pop Directional Format */
        } else {
            bidi1 = "";
            bidi2 = "";
        }
        if (variantName != null &&
            !variantName.toLowerCase().equals("international") &&
            !variantName.equals(""))
            return bidi1 + languageName + ", " + variantName +
                   " (" + countryName + ")" + bidi2;
        else
            return bidi1 + languageName +
                   " (" + countryName + ")" + bidi2;
    }
    
    public String getScript() { return scriptName; }
    
    public String getFileName() { return fileName; }
    
    public String getName() { return displayName; }
    
    public String getLink() {
        return
           "<a href=\"" + HTMLOutput.PRE_LINK + fileName +
           "\" title=\"" + toString() + "\">" +
           displayName + "</a>";
    }
    
}