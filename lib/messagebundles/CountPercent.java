

import java.text.*;
import java.util.*;
import java.io.*;


public class CountPercent {
    
    public static void main(String[] args) throws Exception {
        new CountPercent();
    }
    
    CountPercent() throws Exception {
        NumberFormat pc = NumberFormat.getNumberInstance();
        pc.setMaximumFractionDigits(2);
        pc.setMinimumFractionDigits(2);
        pc.setMinimumIntegerDigits(3);
        pc.setMaximumIntegerDigits(3);
        
        NumberFormat rc = NumberFormat.getNumberInstance();
        rc.setMinimumIntegerDigits(4);
        rc.setMaximumIntegerDigits(4);
        
        Map /* String -> Properties */ langs = new HashMap();
        loadLanguages(langs);
        
        Properties english = getEnglish();
        int total = english.size();
        
        System.out.println("Total Number of Resources: " + total);
        System.out.println("---------------------------------");
        System.out.println();
        
        for(Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            LanguageInfo key = (LanguageInfo)entry.getKey();
            Properties value = (Properties)entry.getValue();
            int size = value.size();
            double percentage = (double)size / (double)total * 100.0;
            System.out.print("(" + key.getCode() + ") " + pc.format(percentage) + "%, size: " + rc.format(size));
            System.out.print(" (");
            byte[] lang = key.toString().getBytes("UTF-8");
            System.out.write(lang, 0, lang.length);
            System.out.println(")");
        }
    }
    
    private Properties getEnglish() throws Exception {
        Properties p = new Properties();
        InputStream in = new FileInputStream(new File("MessagesBundle.properties"));
        p.load(in);
        in.close();
        return p;
    }
    
	private void loadLanguages(Map langs) {
	    File lib = new File(".");
	    if(!lib.isDirectory())
	        return;

	    String[] files = lib.list();
	    for(int i = 0; i < files.length; i++) {
	        if(!files[i].startsWith("MessagesBundle_") ||
	           !files[i].endsWith(".properties") ||
	           files[i].startsWith("MessagesBundle_en"))
	            continue;
	        
	        try {
                InputStream in =
                    new FileInputStream(new File(lib, files[i]));
	            loadFile(langs, in);
            } catch(FileNotFoundException fnfe) {
                // oh well.
            }
        }
    }
    
	/**
	 * Loads a single file into a List.
	 */
	private void loadFile(Map langs, InputStream in) {
	    Properties p = new Properties();
        try {
            in = new BufferedInputStream(in);
            p.load(in);

            String lc = p.getProperty("LOCALE_LANGUAGE_CODE");
            String cc = p.getProperty("LOCALE_COUNTRY_CODE");
            String vc = p.getProperty("LOCALE_VARIANT_CODE");
            String ln = p.getProperty("LOCALE_LANGUAGE_NAME");
            String cn = p.getProperty("LOCALE_COUNTRY_NAME");
            String vn = p.getProperty("LOCALE_VARIANT_NAME");
            
            langs.put(new LanguageInfo(lc, cc, vc, ln, cn, vn), p);
        } catch(IOException e) {
            // ignore.
        } finally {
            if( in != null )
                try { in.close(); } catch(IOException ioe) {}
        }
    }
}
    
    
class LanguageInfo {
    private final String languageCode;
    private final String countryCode;
    private final String variantCode;
    private final String display;
    private final String countryName;
    private final String variantName;
    
    /**
     * Constructs a new LanguageInfo object with the given
     * languageCode, countryCode, variantCode,
     * languageName, countryName, and variantName.
     */
    public LanguageInfo(String lc, String cc, String vc,
                     String ln, String cn, String vn) {
        languageCode = lc.trim();
        countryCode = cc.trim();
        variantCode = vc.trim();
        display = ln.trim();
        countryName = cn.trim();
        variantName = vn.trim();
    }
    
    /**
     * Returns a description of this language.
     * If the variantName is not 'international' or '', then 
     * the display is:
     *    languageName, variantName (countryName)
     * Otherwise, the display is:
     *    languageName (countryName)
     */
    public String toString() {
        if( variantName != null &&
            !variantName.toLowerCase().equals("international") &&
            !variantName.equals("") )
            return display + ", " + variantName + " (" + countryName + ")";
        else
            return display + " (" + countryName + ")";
    }
    
    public String getCode() { return languageCode; }
}    