
import java.text.*;
import java.util.*;
import java.io.*;

public class CountPercent {
    static final String PRE_LINK = "http://www.limewire.org/fisheye/viewrep/~raw,r=MAIN/limecvs/lib/messagebundles/";
    private static final String BUNDLE_NAME = "MessagesBundle";
    private static final String PROPS_EXT = ".properties";
    private static final String UTF8_EXT = ".UTF-8.txt";
    private static final String DEFAULT_LINK = PRE_LINK + BUNDLE_NAME + PROPS_EXT;
    
    /** minimum completion levels for the status HTML page */
    private static final double MIN_PERCENTAGE_COMPLETED     = 0.75;
    private static final double MIN_PERCENTAGE_NEED_REVISION = 0.66;
    private static final double MIN_PERCENTAGE_MIDWAY        = 0.50;
    private static final int    MIN_COUNT_STARTED            = 20;
    
    /** constant link to the translate mailing list. */
    private static final String HTML_TRANSLATE_EMAIL_ADDRESS =
"<b><script type=\"text/javascript\" language=\"JavaScript\"><!--\n" +
"// Protected email script by Joe Maller JavaScripts available at http://www.joemaller.com\n" +
"// This script is free to use and distribute but please credit me and/or link to my site.\n" + 
"e_mA_iL_E = ('limewire' + \"&#46;\" + 'org'); e_mA_iL_E = ('translate' + \"&#64;\" + e_mA_iL_E);\n" +
"document.write('<a href=\"mai' + \"lto:\" + e_mA_iL_E + '\">' + e_mA_iL_E + '</a>');\n" +
"//--></script><noscript><a href=\"#\">[Email address protected by JavaScript:\n" +
"please enable JavaScript to contact me]</a></noscript></b>";
    
    private static final int ACTION_STATISTICS = 0;
    private static final int ACTION_HTML = 1;
    private static final int ACTION_CHECK = 2;
    
    public static void main(String[] args) throws java.io.IOException {
        final int action;
        if (args != null && args.length > 0) {
            if (args[0].equals("html"))
                action = ACTION_HTML;
            else if (args[0].equals("check"))
                action = ACTION_CHECK;
            else {
                System.err.println("Usage: java CountPercent [html|check]");
                return;
            }
        } else
            action = ACTION_STATISTICS;
        new CountPercent(action);
    }
    
    private final DateFormat df;
    private final NumberFormat rc;
    private final NumberFormat pc;
    private final Map/*<String code, LanguageInfo li>*/ langs;
    private final Set/*<String key>*/ basicKeys, advancedKeys;
    private final int basicTotal;

    CountPercent(int action) throws java.io.IOException {
        df = DateFormat.getDateInstance(DateFormat.LONG, Locale.US);

        rc = NumberFormat.getNumberInstance(Locale.US);
        rc.setMinimumIntegerDigits(4);
        rc.setMaximumIntegerDigits(4);
        rc.setGroupingUsed(false);
        
        pc = NumberFormat.getPercentInstance(Locale.US);
        pc.setMinimumFractionDigits(2);
        pc.setMaximumFractionDigits(2);
        pc.setMaximumIntegerDigits(3);
        
        langs = new TreeMap();
        
        final Properties defaultProps = getDefaultProperties();
        advancedKeys = getAdvancedKeys();
        defaultProps.keySet().removeAll(advancedKeys);
        basicKeys = defaultProps.keySet();
        basicTotal = basicKeys.size();

        switch (action) {
        case ACTION_CHECK:
            loadLanguages();
            checkBadKeys();
            break;
        case ACTION_STATISTICS:
            loadLanguages();
            extendVariantLanguages();
            retainBasicKeys(basicKeys);
            pc.setMinimumIntegerDigits(3);
            printStatistics();
            break;
        case ACTION_HTML:
            loadLanguages();
            extendVariantLanguages();
            retainBasicKeys(basicKeys);
            rc.setMinimumIntegerDigits(1);
            printHTML();
            break;
        }
    }
    
    private Properties getDefaultProperties() throws java.io.IOException {
        Properties p = new Properties();
        InputStream in = new FileInputStream(new File(BUNDLE_NAME + PROPS_EXT));
        p.load(in);
        in.close();
        return p;
    }
    
    private Set getAdvancedKeys() throws java.io.IOException  {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(new File(BUNDLE_NAME + PROPS_EXT)),
                "ISO-8859-1"));
        } catch (java.io.UnsupportedEncodingException uee) {
            System.err.println(uee); //shouldn't occur
            throw uee;
        }
        String read;
        while ((read = reader.readLine()) != null &&
               !read.startsWith("## TRANSLATION OF ALL ADVANCED RESOURCE STRINGS AFTER THIS LIMIT IS OPTIONAL"));
        
        StringBuffer sb = new StringBuffer();
        while ((read = reader.readLine()) != null) {
            if (read.length() == 0 ||
                read.charAt(0) == '#')
            continue;
            sb.append(read).append("\n");
        }
        InputStream in;
        try {
            in = new ByteArrayInputStream(sb.toString().getBytes("ISO-8859-1"));
        } catch (java.io.UnsupportedEncodingException uee) {
            System.err.println(uee); //shouldn't occur
            throw uee;
        }
        Properties p = new Properties();
        p.load(in);
        
        in.close();
        reader.close();
        return p.keySet();
    }
    
    /**
     * List and load all available bundles and map them into the languages map.
     * Note that resources are not expanded here per base language, and not cleaned
     * here from extra keys (needed to support the resources "check" option).
     */
    private void loadLanguages() {
        File lib = new File(".");
        if (!lib.isDirectory())
            return;
        
        String[] files = lib.list();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].startsWith(BUNDLE_NAME + "_") ||
                !files[i].endsWith(PROPS_EXT) ||
                 files[i].startsWith(BUNDLE_NAME + "_en"))
                continue;
            
            /* See if a .UTF-8.txt file exists; if so, use that as the link. */
            String linkFileName = files[i];
            int idxProperties = linkFileName.indexOf(PROPS_EXT);
            File utf8 = new File(lib, linkFileName.substring(0, idxProperties) + UTF8_EXT);
            boolean skipUTF8LeadingBOM;
            if (utf8.exists()) {
                /* properties files are normally read as streams of ISO-8859-1 bytes
                but we want to check the UTF-8 source file. The non ASCII characters
                in key values will be read as sequences of Extended Latin 1 characters
                instead of the actual Unicode character coded as Unicode escapes
                in the .properties file. So they won't have the actual run-time value;
                however it allows easier checking and validation here for messages
                printed on the Console, that will output ISO-8859-1; the result output
                still be interpretable as Unicode UTF-8. */
                linkFileName = utf8.getName();
                skipUTF8LeadingBOM = true;
            } else
                skipUTF8LeadingBOM = false;
            
            try {
                InputStream in =
                    new FileInputStream(new File(lib, linkFileName/*files[i]*/));
                if (skipUTF8LeadingBOM) try { /* skip the three-bytes leading BOM */
                   /* the leading BOM (U+FEFF), if present, is coded in UTF-8 as three
                    * bytes 0xEF, 0xBB, 0xBF; they are not part of a resource key. */
                   in.mark(3);
                   if (in.read() != 0xEF || in.read() != 0xBB || in.read() != 0xBF)
                       in.reset();
                } catch (java.io.IOException ioe) {
                }
                loadFile(langs, in, linkFileName, skipUTF8LeadingBOM);
            } catch (FileNotFoundException fnfe) {
                // oh well.
            }
        }
    }
    
    /**
     * Loads a single file into the languages map.
     */
    private LanguageInfo loadFile(Map langs, InputStream in, String filename, boolean isUTF8) {
        try {
            in = new BufferedInputStream(in);
            final Properties props = new Properties();
            props.load(in);
            /* note that the file is read in ISO-8859-1 only, even if it is encoded
             * with another charset. However, the Properties has its unique legacy parser
             * and we want to use it to make sure we use the same syntax. So we'll need to
             * correct the parsed values after the file is read and interpreted as a set of
             * properties (keys,values).
             */
            if (isUTF8) {
                // actually the file was UTF-8-encoded: convert bytes read incorrectly as
                // ISO-8859-1 characters, into actual Unicode UTF-16 code units.
                for (Iterator i = props.entrySet().iterator(); i.hasNext(); ) {
                    final Map.Entry entry = (Map.Entry)i.next();
                    final String key = (String)entry.getKey();
                    final String value = (String)entry.getValue();
                    byte[] bytes = null;
                    try {
                        bytes = value.getBytes("ISO-8859-1");
                    } catch (java.io.IOException ioe) {
                        //should not occur
                    }
                    try {
                        final String correctedValue = new String(bytes, "UTF-8");
                        if (!correctedValue.equals(value))
                            props.put(key, correctedValue);
                    } catch (java.io.IOException ioe) {
                        System.err.println(ioe); //should not occur
                    }
                }
            }
            String lc = props.getProperty("LOCALE_LANGUAGE_CODE", "");
            String cc = props.getProperty("LOCALE_COUNTRY_CODE", "");
            String vc = props.getProperty("LOCALE_VARIANT_CODE", "");
            String sc = props.getProperty("LOCALE_SCRIPT_CODE", "");
            String ln = props.getProperty("LOCALE_LANGUAGE_NAME", lc);
            String cn = props.getProperty("LOCALE_COUNTRY_NAME", cc);
            String vn = props.getProperty("LOCALE_VARIANT_NAME", vc);
            String sn = props.getProperty("LOCALE_SCRIPT_NAME", sc);
            String dn = props.getProperty("LOCALE_ENGLISH_LANGUAGE_NAME", ln);
            boolean rtl = props.getProperty("LAYOUT_RIGHT_TO_LEFT", "false").equals("true");
            
            LanguageInfo li = new LanguageInfo(lc, cc, vc, sc,
                                               ln, cn, vn, sn,
                                               dn, rtl, filename, props);
            langs.put(li.getCode(), li);
            return li;
        } catch (IOException e) {
            // ignore.
        } finally {
            if (in != null)
                try {
                   in.close();
                } catch (IOException ioe) {}
        }
        return null;
    }
    
    /**
     * Check and list extra or badly names names found in resources.
     * Use the default (English) basic and extended resource keys.
     */
    private void checkBadKeys() {
        System.out.println("List of extra or badly named resource keys:");
        System.out.println("-------------------------------------------");
        
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)i.next();
            final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            props.keySet().removeAll(basicKeys);
            props.keySet().removeAll(advancedKeys);
            if (props.size() != 0) {
                System.out.println("(" + code + ") " + li.getName() + ": " + li.getFileName());
                props.list(System.out);
                System.out.println("-------------------------------------------");
            }
        }
    }
    
    /**
     * Extend variant resources from *already loaded* base languages.
     */
    private void extendVariantLanguages() {
        /* Extends missing resources with those from the base language */
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)i.next();
            final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            if (li.isVariant()) {
                final LanguageInfo liBase = (LanguageInfo)langs.get(li.getBaseCode());
                if (liBase != null) {
                    /* Get a copy of base properties */
                    final Properties propsBase = new Properties();
                    propsBase.putAll(liBase.getProperties());
                    /* Remove properties already defined in the current locale */
                    propsBase.keySet().removeAll(props.keySet());
                    /* Add the remaining base properties to the current locale */
                    props.putAll(propsBase);
                }
            }
        }
    }
    
    /**
     * Keep only resources with a basic set of valid keys.
     */
    private void retainBasicKeys(Set basicKeys) {
        /* Extends missing resources with those from the base language */
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)i.next();
            final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            props.keySet().retainAll(basicKeys);
        }
    }
    
    private void printStatistics() {
        System.out.println("Total Number of Resources: " + basicTotal);
        System.out.println("---------------------------------");
        System.out.println();
        
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)i.next();
            final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            final int count = props.size();
            final double percentage = (double)count / (double)basicTotal;
            System.out.print(
                "(" + code + ") " +
                pc.format(percentage) +
                ", count: " + rc.format(count) +
                " (" + li.getName() + ": ");
            try {
                final byte[] langName = li.toString().getBytes("UTF-8");
                System.out.write(langName, 0, langName.length);
            } catch (java.io.UnsupportedEncodingException uee) {
                //shouldn't occur
            }
            System.out.println(")");
        }
    }
    
    private void printHTML() {
        List langsCompleted = new LinkedList();
        List langsNeedRevision = new LinkedList();
        List langsMidway = new LinkedList();
        List langsStarted = new LinkedList();
        List langsEmbryonic = new LinkedList();
        Map charsets = new HashMap();
        
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)i.next();
            final String code = (String)entry.getKey();
            final LanguageInfo li = (LanguageInfo)entry.getValue();
            final Properties props = li.getProperties();
            final int count = props.size();
            final double percentage = (double)count / (double)basicTotal;
            li.setPercentage(percentage);
            if (percentage >= MIN_PERCENTAGE_COMPLETED)
                langsCompleted.add(li);
            else if (percentage >= MIN_PERCENTAGE_NEED_REVISION)
                langsNeedRevision.add(li);
            else if (percentage >= MIN_PERCENTAGE_MIDWAY)
                langsMidway.add(li);
            else if (count >= MIN_COUNT_STARTED)
                langsStarted.add(li);
            else
                langsEmbryonic.add(li);
            
            String script = li.getScript();
            List inScript = (List)charsets.get(script);
            if (inScript == null) {
                inScript = new LinkedList();
                charsets.put(script, inScript);
            }
            inScript.add(li);
        }
        
        StringBuffer page = new StringBuffer();
        buildStartOfPage(page);
        buildStatus(page, langsCompleted,
                          langsNeedRevision,
                          langsMidway,
                          langsStarted,
                          langsEmbryonic);
        buildAfterStatus(page);
        buildProgress(page, charsets);
        buildEndOfPage(page);

        /* Make sure printed page contains only ASCII, converting all
         * other code points to decimal NCRs. This will work whatever
         * charset will be selected by the user's browser.
         */
        int pageLength = page.length();
        for (int index = 0; index < pageLength; ) {
            int c = (int)page.charAt(index++); // char's are always positive
            if (c < 160) { /* C0 or Basic Latin or C1 */
                if (c >= 32 && c < 127 || c == '\t') /* Basic Latin or TAB */
                    System.out.print((char)c);
                else if (c == '\n') /* LF */
                    System.out.println(); /* platform's newline sequence */
                /* ignore all other C0 and C1 controls */
            } else { /* Use NCRs */
                /* special check for surrogate pairs */
                if (c >= 0xD800 && c <= 0xDBFF && index < pageLength) {
                    int c2 = (int)page.charAt(index);
                    if (c2 >= 0xDC00 && c2 <= 0xDFFF) {
                        c = 0x10000 + ((c - 0xD800) << 10) + (c2 - 0xDC00);
                        index++;
                    }
                }
                System.out.print("&#");
                System.out.print((int)c);//decimal NCR notation
                System.out.print(';');
            }
        }
    }
    
    private void buildStartOfPage(StringBuffer page) {
        page.append(
"<html>\n" +
"<head>\n" +
"<!--#include virtual=\"/includes/top.html\" -->\n" +
/* (already in top.html)
"</head>\n" +
"<body>\n" +
 */
"<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
"<tr>\n" +
" <td valign=\"top\">\n" +
"  <div id=\"bod1\">\n" +
"   <h1>Help Internationalize LimeWire</h1>\n" +
"   <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n" + /* Three columns */
"   <tr>\n" +
"    <td valign=\"top\" style=\"line-height: 16px;\">\n" + /* Start column 1 (main content) */
"     The LimeWire Open Source project has embarked on an effort to\n" +
"     internationalize LimeWire. This involves efforts not just by programmers and\n" +
"     developers, but also avid LimeWire users who are bilingual. If you are an\n" +
"     English speaking user fluent in another language, we need your help.<br />\n" +
"     <br />\n" +
"     Before we begin, you should read the following to better acquaint you with\n" +
"     the open source development environment and this website, LimeWire.org. We\n" +
"     will try to make this process as computer science-free as possible.<br />\n" +
"     <br />\n" +
"     <b><big>How LimeWire Renders Code to a Spoken Language</big></b><br />\n" + 
"     <br />\n" +
"     Open up this <a href=\"http://www.limewire.com/img/screenshots/search.jpg\"\n" +
"     target=\"_blank\">LimeWire Screenshot</a>.<br />\n" +
"     Take a look at the tabs, <b>Search</b>, <b>Monitor</b>, <b>Library</b>, etc.\n" +
"     Now take a look at the buttons, <b>Download</b>, <b>Kill Download</b>,\n" +
"     etc...All of these elements of LimeWire (the buttons, the tabs, the Genre\n" +
"     type, the Options window) can be translated to any language very easily. You\n" +
"     can change the word \"Download\" seen on the frequently used \"Download\"\n" +
"     button to \"TÚlÚcharger\" (French for \"download\"), via a text file found\n"+
"     in your LimeWire application directory.<br />\n" +
"     <br />\n" +
"     This file is called the \"<b>Messages Bundle</b>\" file. This file\n" +
"     acts as a dictionary, translating Java code to English or French, etc. When\n" +
"     you start LimeWire, the program reads this file and plugs in the\n" +
"     translations.<br />\n" +
"     <br />\n" + 
"     <a href=\"" + PRE_LINK + "MessagesBundle_es.properties\"><b>Click\n" +
"     here to see what a Messages Bundle looks like</b></a>: (In this example, the\n" +
"     Messages Bundle file has been partly translated into Spanish.)<br />\n" +
"     <br />\n");
    }
    
    private void buildStatus(StringBuffer page,
                             List langsCompleted,
                             List langsNeedRevision,
                             List langsMidway,
                             List langsStarted,
                             List langsEmbryonic) {
        page.append(
"     <b><big>Translations Status:</big></b>\n" +
"     <br />\n" +
"     <ol>\n");
        buildStatus(page, langsCompleted,
"       are complete and will require only small revisions during the project\n" +
"       evolution.");
        buildStatus(page, langsNeedRevision,
"       are mostly complete and can still be used reliably, but may need some\n" +
"       revisions and a few missing translations to work best with newest versions.");
        buildStatus(page, langsMidway,
"       have been completed for an old version, but may now require some work, tests\n" +
"       and revisions plus additional missing translations to reach a reliable status.");
        buildStatus(page, langsStarted,
"       are partly translated but still unfinished, and their use in LimeWire\n" +
"       may be difficult for native language users. Providing a more complete\n" +
"       translation would be very much appreciated.");
        buildStatus(page, langsEmbryonic,
"       are only embryonic and actually need a complete translation.\n" +
"       The current files are only there for demonstration purpose.");
        page.append(
"     </ol>\n");
    }
    
    private void buildStatus(StringBuffer page,
                             List langs, String status) {
        boolean first = true;
        for (Iterator i = langs.iterator(); i.hasNext(); ) {
            LanguageInfo l = (LanguageInfo)i.next();
            if (first)
                page.append(
"      <li>\n");
            else if (!i.hasNext())
                page.append(" and\n");
            else
                page.append(",\n");
            page.append(
"       <b>" + l.getLink() + "</b>");
            first = false;
        }
        if (!first)
            page.append("\n" + status + "</li>\n");
    }

    private void buildAfterStatus(StringBuffer page) {
        page.append(
"     <br />\n" +
"     <b><big>Which tool or editor is needed to work on a translation:</big></b><br />\n" + 
"     <br />\n" + 
"     For <b>Western European Latin-based languages</b>, which can use the US-ASCII \n" +
"     or ISO-8859-1 character set, any text editor (such as NotePad on Windows) can\n" +
"     be used on Windows and Linux. Once a file is completed, it can be sent as a\n" +
"     simple text file to\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br />\n" +
"     <br />\n" +
"     For <b>Central European languages</b>, the prefered format is a simple text\n" +
"     file encoded with the ISO-8859-2 character set, or an UTF-8 encoded simple\n" +
"     text file (which can be edited with NotePad on Windows 2000/XP), or a\n" +
"     correctly marked-up HTML document such as HTML email, or a Word document.<br />\n" +
"     <br />\n" +
"     For <b>other European languages</b>, the preferred format is a plain-text\n" +
"     file, encoded preferably with UTF-8 or a ISO-8859-* character set that\n" +
"     you must explicitly specify to us, or a correctly marked-up HTML document,\n" +
"     or a Word document. Please specify your working operating system and editor\n" +
"     you used to create plain-text files (we may support Windows codepages or Mac\n" +
"     charsets, but we will convert them to Unicode UTF-8 in our repository).<br />\n" +
"     <br />\n" +
"     For <b>Semitic languages</b> (Arabic, Hebrew...), the preferred format is a\n" +
"     plain-text file edited with a editor that supports the right-to-left layout,\n" +
"     encoded preferably with UTF-8 or a ISO-8859-* character set, in logical order.\n" +
"     Be careful with the relative order of keys and values, and with the\n" +
"     appearance of ASCII punctuations around right-to-left words: <i>make sure that\n" +
"     your editor uses the RTL layout with the edited text aligned on the right</i>;\n" +
"     please don't insert BiDi control overrides; but you may need to place LRE/PDF\n" +
"     marks (U+202B/U+202C) locally around non-Semitic words inserted within Semitic\n" +
"     sentences. <i>Also the \"</i><code>\\n</code><i>\" sequences that encode a newline\n" +
"     will be displayed within semitic text as \"</i><code>n\\</code><i>\": don't use\n" +
"     BiDi overrides for such special sequence whose appearance in your editor is not\n" +
"     important, but that MUST be entered with a leading backslash before the 'n'\n" +
"     character.</i><br />\n" +
"     <br />\n" +
"     For <b>Asian Languages</b>, the preferred submission format is a Unicode text\n" +
"     file encoded with UTF-8. Users of Windows 2000/XP can use NotePad but you\n" +
"     must explicitly select the UTF-8 encoding when saving your file. Users of\n" +
"     localized versions of Windows 95/98/ME can only save their file in the native\n" +
"     local \"ANSI\" encoding, and should then send us their translation by\n" +
"     copy/pasting it in the content body of the email.<br />\n" +
"     <br />\n" +
"     <b>Macintosh users</b> should use a word-processor and send us their\n" +
"     translations in an unambiguous format. Mac OS 8/9 \"SimpleText\"\n" +
"     files use a Mac specific specific encoding for international languages\n" +
"     (additionally, SimpleText is too much limited to edit large files), and a\n" +
"     specific format for text attachments (we may enventually have difficulties to\n" +
"     decipher some Mac encodings used in simple text files attachment). On Mac OSX,\n" +
"     the best tool is \"TextEdit\", from the Jaguar accessories, with\n" +
"     which you can directly edit and save plain-text files encoded with UTF-8.<br />\n" +
"     <br />\n" +
"     <b>Linux users</b> can also participate if they have a correctly setup\n" +
"     environment for their locale. Files can be edited with \"vi\", \"emacs\", or\n" +
"     graphical editors for X11.<br />\n" +
"     <br />\n" +
"     For other information about internationalization standards, language and\n" +
"     country codes, character sets and encodings, you may visit these web pages:<br />\n" +
"     <ul>\n" +
"      <li>Language codes: <a href=\"http://www.loc.gov/standards/iso639-2/englangn.html\"\n" +
"       target=\"_blank\">http://www.loc.gov/standards/iso639-2/englangn.html</a></li>\n" +
"      <li>Country codes: <a href=\"http://www.iso.org/iso/en/prods-services/iso3166ma/index.html\"\n" +
"       target=\"_blank\">http://www.iso.org/iso/en/prods-services/iso3166ma/index.html</a></li>\n" +
"      <li>Character sets: <a href=\"http://www.w3.org/International/O-charset.html\"\n" +
"       target=\"_blank\">http://www.w3.org/International/O-charset.html</a></li>\n" +
"      <li>Letter database (languages and character sets): <a href=\"http://www.eki.ee/letter/\"\n" +
"       target=\"_blank\">http://www.eki.ee/letter/</a></li>\n" +
"      <li>Other internationalization data: <a href=\"http://www.unicode.org/unicode/onlinedat/resources.html\"\n" +
"       target=\"_blank\">http://www.unicode.org/unicode/onlinedat/resources.html</a></li>\n" +
"     </ul>\n" +
"     An excellent tutorial on various character sets, including the ASCII variants, the\n" +
"     ISO-8859 family, the Windows \"ANSI code pages\", Macintosh character codes, and\n" +
"     Unicode (or its ISO/IEC 10646 repertoire) can be found on:\n" +
"     <a href=\"http://www.cs.tut.fi/~jkorpela/chars.html\"\n" +
"     target=\"_blank\">http://www.cs.tut.fi/~jkorpela/chars.html</a></li><br />\n" +
"     <br />\n" +
"     <b><big>How to submit corrections or enhancements for your language:</big></b><br />\n" +
"     <br />\n" +
"     Users that don't have the correct tools to edit a Messages Bundle can send us\n" +
"     an Email in English or in French, that explain their needs.<br />\n" +
"     For any discussion with the contributing translators you may write to\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br />\n" +
"     <br />\n" +
"     <b>If your corrections are significant</b>, you may send us your complete\n" +
"     Message Bundle. Please be sure to include all resource strings defined in the\n" +
"     latest version of the existing Messages Bundle before sending us your\n" +
"     revision.<br />\n" +
"     <br />\n" +
"     <b>For simple few corrections or additions</b>, just send the corrected lines\n" +
"     in the content body of an Email (make sure you select the correct character encoding\n" +
"     in your email tool before sending it so that non-ASCII characters are not lost or\n" +
"     replaced), with your comments. <i>We will review your translations and integrate\n" +
"     them into our existing versions after review.</i><br />\n" +
"     <br />\n" +
"     <b><big>How to test a new translation:</big></b><br />\n" +
"     <br />\n" +
"     Only Windows and Unix simple text editors can create a plain-text file which\n" +
"     will work in LimeWire, and only for languages using the Western European\n" +
"     Latin character set. Don't use \"SimpleText\" on Mac OS to edit\n" +
"     properties files as SimpleText does not create plain-text files. Other\n" +
"     translations need to be converted into regular properties files, encoded\n" +
"     using the ISO-8859-1 Latin character set and Unicode escape sequences, with a\n" +
"     tool \"native2ascii\" found in the Java Development Kit.<br />\n" +
"     <br />\n" +
"     You don't need to rename your translated and converted bundle, which can\n" +
"     coexist with the English version. LimeWire will load the appropriate\n" +
"     resources file according to the \"<code>LANGUAGE=</code>\", and\n" +
"     \"<code>COUNTRY=</code>\" settings stored in your \"limewire.props\"\n" +
"     preferences file. Lookup for the correct language code to use, in the list\n" +
"     beside.<br />\n" +
"     <br />\n" +
"     Until version 2.4 of LimeWire, bundles used by LimeWire were simply stored in\n" +
"     a single \"MessagesBundle.properties\" file of your installation\n" +
"     directory. However these versions support other bundles in the same\n" +
"     directory.<br />\n" +
"     <br />\n" +
"     Since version 2.5, bundles are searched in a single <b>zipped archive</b>\n" +
"     named \"MessagesBundles.jar\" installed with LimeWire. All bundles\n" +
"     are named \"MessagesBundle_xx.properties\", where \"xx\" is replaced by\n" +
"     the language code (as shown on the table beside).\n" +
"     Note that bundles for languages using non-Western European Latin characters\n" +
"     will be converted from UTF-8 to ASCI using a special format with hexadecimal\n" +
"     Unicode escape sequences, prior to their inclusion in this archive. This can be\n" +
"     performed using the <code>native2ascii</code> tool from the Java Development Kit.\n" +
"     If you don't know how to proceed to test the translation yourself,\n" +
"     ask us for assistance at the same email address used for your contributions.<br />\n" +
"     <br />\n" +
"     <b><big>How to create a new translation:</big></b><br />\n" +
"     <br />\n" +
"     Users that wish to contribute with a new translation must be fluent in the\n" +
"     target language, preferably native of a country where this language is\n" +
"     official. The English version is the reference one, but if needed you can\n" +
"     look for suggestions at the other completed translations. Before starting\n" +
"     your work, please contact us at\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br />\n" +
"     During the translation process, you may subscribe to the translate list (see above)\n" +
"     where you will benefit from other contributions sent to this address.<br />\n" + 
"     <br />\n" + 
"     Do not start with the existing Message Bundle installed with your current\n" +
"     servent. <b>Work on the latest version of a complete Messages Bundle</b> from\n" +
"     the list of languages on the right of this page (preferably the English\n" +
"     version or the French version which is constantly maintained in sync and is\n" +
"     fully translated). Look also to other existing translations if you need tips.<br />\n" +
"     <br />\n" +
"     When translating, adopt the <b>common terminology</b> used in your localized\n" +
"     operating system. In some cases, some terms were imported from English,\n" +
"     despite other terms already existed in your own language. If a local term can\n" +
"     be used unambiguously, please use it in preference to the English term, even\n" +
"     if you have seen many uses of this English term on web sites. A good\n" +
"     translation must be understood by most native people that are not addicted to\n" +
"     the Internet and computers \"jargon\". Pay particularly attention to the\n" +
"     non-technical translation of common terms: download, upload, host, byte,\n" +
"     firewall, address, file, directory, # (number of), leaf (terminal node)...<br />\n" +
"     <br />\n" +
"     Avoid translating word for word, don't use blindly automatic translators,\n" +
"     be imaginative but make a <b>clear and concise</b> translation. For button\n" +
"     labels and column names, don't translate them with long sentences, as they\n" +
"     may be truncated; suppress some articles, or use abbreviations if needed.\n" +
"     If there are problems translating some difficult terms, write to the translate\n" +
"     list (in English or French) for assistance, and subscribe to this list to\n" +
"     receive comments from users or other translators.<br />\n" +
"     <br />\n" +
"     After you've plugged in your translations into your language's Messages\n" +
"     Bundle, send your copy as a plain-text file attachement to\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br />\n" +
"     <i>We will review your translations and integrate them into our existing\n" +
"     versions after review.</i><br />\n"+
"     <br />\n" +
"     <br />\n" +
"    </td>\n" + /* End of column 1 (spacing) */
"    <td>&nbsp;&nbsp;&nbsp;</td>\n" + /* Column 2 (spacing) */
"    <td valign=\"top\">\n" + /* Start of column 3 (status) */
      /* Start shaded right rectangle */
"     <table border=\"0\" cellspacing=\"1\" cellpadding=\"4\" bgcolor=\"#b1b1b1\" width=\"270\">\n" +
"     <tr bgcolor=\"#EFEFEF\">\n" + 
"      <td valign=\"top\"><br />\n" +
"       <b>LAST UPDATED: <font color=\"#FF0000\">" + df.format(new Date()) + "</font><br />\n" +
"       <br />\n" +
"       LATEST TRANSLATIONS & COMPLETION STATUS:</b><br />\n" +
"       <br />\n" +
"       To get the most recent version of a Messages Bundle, <b>click on the\n" +
"       corresponding language</b> in the following list.<br />\n" +
"       <br />\n" +
"       <font color=\"#FF0000\">To view all available Messages Bundle projects\n" +
"       (new languages may be added), <a\n" +
"       href=\"" + PRE_LINK + "\">click here</a></font>.\n");
    }
    
    private void buildProgress(StringBuffer page, Map charsets) {
        page.append(
"       <table width=\"250\" border=\"0\" cellpadding=\"0\" cellspacing=\"4\">");
        List latin = (List)charsets.remove("Latin");
        page.append(
"       <tr>\n" +
"        <td colspan=\"3\" valign=\"top\">" +
"         <hr noshade size=\"1\">\n" +
"         Languages written with Latin (Western European) characters:</td>\n" +
"       </tr>\n" +
"       <tr>\n" +
"        <td valign=\"top\"><a href=\"" + DEFAULT_LINK + "\" target=\"_blank\"><b>English</b> (US)</a></td>\n" +
"        <td align=\"right\">(default)</td>\n" +
"        <td>en</td>\n" + 
"       </tr>\n");
        for (Iterator i = latin.iterator(); i.hasNext(); ) {
            LanguageInfo l = (LanguageInfo)i.next();
            page.append(
"       <tr>\n" +
"        <td><b>" + l.getLink() + "</b></td>\n" +
"        <td align=\"right\">(" + pc.format(l.getPercentage()) + ")</td>\n" +
"        <td>" + l.getCode() + "</td>\n" +
"       </tr>\n");
        }
        for (Iterator i = charsets.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            String code = (String)entry.getKey();
            List l = (List)entry.getValue();
            page.append(
"       <tr>\n" +
"        <td colspan=\"3\" valign=\"top\">\n" +
"         <hr noshade size=\"1\">\n" +
"         Languages written with " + code + " characters:</td>\n" +
"       </tr>\n");
            for (Iterator j = l.iterator(); j.hasNext(); ) {
                LanguageInfo li = (LanguageInfo)j.next();
                page.append(
"       <tr>\n" +
"        <td><b>" + li.getLink() + "</b></td>\n" +
"        <td align=\"right\">(" + pc.format(li.getPercentage()) + ")</td>\n" +
"        <td>" + li.getCode() + "</td>\n" +
"       </tr>\n");
            }
        }
        page.append(
"       </table>\n");
    }
    
    private void buildEndOfPage(StringBuffer page) {
        page.append(
"      </td>\n" +
"     </tr>\n" +
"     </table>\n" + /* End of shaded right rectangle */
"    </td>\n" + /* End of column 3 (status) */
"   </tr>\n" +
"   </table>\n" + /* End of the 3 columns table below the title */
"  </div>\n" + /* (div id="bod1") */
" </td>\n" +
"</tr>\n" +
"</table>\n" +
"<!--#include virtual=\"/includes/bottom.html\" -->\n" +
"\n");
    }
    
}

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
     * Returns a description of this language.
     * If the variantName is not 'international' or '', then 
     * the display is:
     *    languageName, variantName (countryName)
     * Otherwise, the display is:
     *    languageName (countryName)
     */
    public String toString() {
        if (variantName != null &&
            !variantName.toLowerCase().equals("international") &&
            !variantName.equals(""))
            return languageName + ", " + variantName + " (" + countryName + ")";
        else
            return languageName + " (" + countryName + ")";
    }
    
    public String getScript() { return scriptName; }
    
    public String getFileName() { return fileName; }
    
    public String getName() { return displayName; }
    
    public String getLink() {
        final String tip;
        if (isRightToLeft)
            tip = "\u202b" /* RLE control: Right-To-Left Embedding */
                + toString()
                + "\u202c"; /* PDF control: Pop Directional Format */
        else
            tip = toString();
        return
           "<a href=\"" + CountPercent.PRE_LINK + fileName +
           "\" title=\"" + tip + "\">" +
           displayName + "</a>";
    }
    
}
