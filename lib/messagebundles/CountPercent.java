

import java.text.*;
import java.util.*;
import java.io.*;

public class CountPercent {
    static final String PRE_LINK = "http://www.limewire.org/fisheye/viewrep/~raw,r=MAIN/limecvs/lib/messagebundles/";
    private static final String BUNDLE_NAME = "MessagesBundle";
    private static final String PROPS_EXT = ".properties";
    private static final String UTF8_EXT = ".UTF-8.txt";
    private static final String ENGLISH_LINK = PRE_LINK + BUNDLE_NAME + PROPS_EXT;
    
    public static void main(String[] args) throws Exception {
        new CountPercent(args);
    }
    
    private final NumberFormat pc;
    private final NumberFormat rc;
    private final DateFormat df;
    private final Map langs;
    private final int total;
    
    CountPercent(String[] args) throws Exception {
        pc = NumberFormat.getPercentInstance(Locale.US);
        pc.setMinimumFractionDigits(2);
        pc.setMaximumFractionDigits(2);
        pc.setMaximumIntegerDigits(3);
        
        rc = NumberFormat.getNumberInstance(Locale.US);
        rc.setMinimumIntegerDigits(4);
        rc.setMaximumIntegerDigits(4);
        
        df = DateFormat.getDateInstance(DateFormat.LONG, Locale.US);
        
        Set advanced = getAdvancedKeys();        
        langs = new TreeMap();
        loadLanguages(advanced, langs);
        
        Properties english = getEnglish();
        removeAdvanced(advanced, english);
        total = english.size();
        
        if (args != null && args.length > 0 && args[0].equals("html")) {
            pc.setMinimumIntegerDigits(1);
            printHTML();
        } else {
            pc.setMinimumIntegerDigits(3);
            printStatistics();
        }
    }
    
    private Set getAdvancedKeys() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(new File(BUNDLE_NAME + PROPS_EXT)));
        String read = reader.readLine();
        while (read != null &&
               !read.startsWith("## TRANSLATION OF ALL ADVANCED RESOURCE STRINGS AFTER THIS LIMIT IS OPTIONAL"))
            read = reader.readLine();
        
        StringBuffer sb = new StringBuffer();
        while (read != null) {
            sb.append("\n").append(read);
            read = reader.readLine();
        }
        InputStream in = new ByteArrayInputStream(sb.toString().getBytes());
        Properties p = new Properties();
        p.load(in);
        
        in.close();
        reader.close();
        return p.keySet();
    }
    
    private void loadLanguages(Set advanced, Map langs) {
        File lib = new File(".");
        if (!lib.isDirectory())
            return;
        
        String[] files = lib.list();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].startsWith(BUNDLE_NAME + "_") ||
                !files[i].endsWith(PROPS_EXT) ||
                 files[i].startsWith(BUNDLE_NAME + "_en"))
                continue;
            
            String linkFileName = files[i];
            // see if a .UTF-8.txt file exists; if so, use that as the link.
            int idxProperties = linkFileName.indexOf(PROPS_EXT);
            File utf8 = new File(lib, linkFileName.substring(0, idxProperties) + UTF8_EXT);
            if (utf8.exists())
                linkFileName = utf8.getName();
            
            try {
                InputStream in =
                    new FileInputStream(new File(lib, files[i]));
                loadFile(langs, in, advanced, linkFileName);
            } catch (FileNotFoundException fnfe) {
                // oh well.
            }
        }
    }
    
    /**
     * Loads a single file into a List.
     */
    private void loadFile(Map langs, InputStream in, Set advanced, String filename) {
        try {
            in = new BufferedInputStream(in);
            final Properties p = new Properties();
            p.load(in);
            removeAdvanced(advanced, p);
            
            String lc = p.getProperty("LOCALE_LANGUAGE_CODE", "");
            String cc = p.getProperty("LOCALE_COUNTRY_CODE", "");
            String vc = p.getProperty("LOCALE_VARIANT_CODE", "");
            String sc = p.getProperty("LOCALE_SCRIPT_CODE", "");
            String ln = p.getProperty("LOCALE_LANGUAGE_NAME", lc);
            String cn = p.getProperty("LOCALE_COUNTRY_NAME", cc);
            String vn = p.getProperty("LOCALE_VARIANT_NAME", vc);
            String sn = p.getProperty("LOCALE_SCRIPT_NAME", sc);
            String dn = p.getProperty("LOCALE_ENGLISH_LANGUAGE_NAME", ln);
            
            langs.put(new LanguageInfo(lc, cc, vc, sc,
                                       ln, cn, vn, sn,
                                       dn, filename), p);
        } catch (IOException e) {
            // ignore.
        } finally {
            if (in != null)
                try { in.close(); } catch (IOException ioe) {}
        }
    }
    
    private Properties getEnglish() throws Exception {
        Properties p = new Properties();
        InputStream in = new FileInputStream(new File(BUNDLE_NAME + PROPS_EXT));
        p.load(in);
        in.close();
        return p;
    }
    
    private void removeAdvanced(Set a, Properties p) {
        for (Iterator i = a.iterator(); i.hasNext(); )
            p.remove(i.next());
    }
    
    private void printStatistics() throws Exception {
        System.out.println("Total Number of Resources: " + total);
        System.out.println("---------------------------------");
        System.out.println();
        
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            LanguageInfo key = (LanguageInfo)entry.getKey();
            Properties value = (Properties)entry.getValue();
            int size = value.size();
            double percentage = (double)size / (double)total;
            System.out.print("(" + key.getCode() + ") " + pc.format(percentage) + ", size: " + rc.format(size));
            System.out.print(" ( " + key.getName() + ": ");
            byte[] lang = key.toString().getBytes("UTF-8");
            System.out.write(lang, 0, lang.length);
            System.out.println(")");
        }
    }
    
    private void printHTML() throws Exception {
        List completed = new LinkedList();
        List midway = new LinkedList();
        List started = new LinkedList();
        Map charsets = new HashMap();
        
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            LanguageInfo key = (LanguageInfo)entry.getKey();
            Properties value = (Properties)entry.getValue();
            int size = value.size();
            double percentage = (double)size / (double)total;
            key.setPercentage(percentage);
            if (percentage >= 0.70)
                completed.add(key);
            else if (percentage >= 0.40)
                midway.add(key);
            else
                started.add(key);
            
            String script = key.getScript();
            List inScript = (List)charsets.get(script);
            if (inScript == null) {
                inScript = new LinkedList();
                charsets.put(script, inScript);
            }
            inScript.add(key);
        }
        
        StringBuffer page = new StringBuffer();
        buildStartOfPage(page);
        buildStatus(page, completed, midway, started);
        buildAfterStatus(page);
        buildProgress(page, charsets);
        buildEndOfPage(page);
        byte[] out = page.toString().getBytes("8859_1");//prefered encoding for the LimeWire.org website, not UTF-8!
        System.out.write(out, 0, out.length);
    }
    
    private void buildStartOfPage(StringBuffer page) {
        page.append(

"<html>\n" + 
"<head>\n" +
"<!--#include virtual=\"/includes/top.html\" -->\n" +
"<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
  "<tr>\n" +
    "<td valign=\"top\" colspan=2>\n" +
	"<div id=\"bod1\">\n" +
	"<h1>Help Internationalize LimeWire</h1>\n" +
"<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n" + 
"<tr>\n" +
"<td valign=\"top\" style=\"line-height : 16px;\">\n" +
  "The LimeWire Open Source project has embarked on an effort to " +
  "internationalize LimeWire. This involves efforts not just by programmers and " +
  "developers, but also avid LimeWire users who are bilingual. If you are an " +
  "English speaking user fluent in another language, we need your help.<br>\n" +
  "<br>\n" +
  "Before we begin, you should read the following to better acquaint you with " +
  "the open source development environment and this website, LimeWire.org. We " +
  "will try to make this process as computer science-free as possible.<br>\n" +
  "<br>\n" +
  "<b>How LimeWire Renders Code to a Spoken Language</b><br>\n" + 
  "<br>\n" +
  "Open up this <a href=\"http://www.limewire.com/img/screenshots/search.jpg\" target=\"_blank\">LimeWire Screenshot</a><br>\n" +
  "Take a look at the tabs, <b>Search</b>, <b>Monitor</b>, <b>Library</b>, etc. " +
  "Now take a look at the buttons, <b>Download</b>, <b>Kill Download</b>, " +
  "etc...All of these elements of LimeWire (the buttons, the tabs, the Genre " +
  "type, the Options window) can be translated to any language very easily. You " +
  "can change the word \"Download\" seen on the frequently used " +
  "\"Download\" button to \"Télécharger\" (French for " +
  "\"download\"), via a text file found in your LimeWire application " +
  "directory.<br>\n" +
  "<br>\n" +
  "This file is called the \"<b>Messages Bundle</b>\" file. This file " +
  "acts as a dictionary, translating Java code to English or French, etc. When " +
  "you start LimeWire, the program reads this file and plugs in the " +
  "translations.<br>\n" +
  "<br>\n" + 
  "<a href=\"" + PRE_LINK + "MessagesBundle_es.properties\"><b>Click " +
  "here to see what a Messages Bundle looks like</b></a>: (In this example, the " +
  "Messages Bundle file has been partly translated into Spanish.)<br>\n" +
  "<br>\n"
  );
    }
    
    private void buildStatus(StringBuffer page, List completed, List midway, List started) {
        boolean first = false;
        page.append("<b>Translations Status:</b> <ol>\n" +
                    "<li><b><a href=\"" + ENGLISH_LINK + ">English</a></b>");
                    
        for (Iterator i = completed.iterator(); i.hasNext(); ) {
            LanguageInfo l = (LanguageInfo)i.next();
            if (!first && !i.hasNext())
                page.append(" and ");
            else if (!first)
                page.append(", ");
            page.append("<b>" + l.getLink() + "</b>");
        }
        page.append(" are complete and will require only small revisions during the project evolution.</li>");
        
        page.append("<li>");
        first = true;
        for (Iterator i = midway.iterator(); i.hasNext(); ) {
            LanguageInfo l = (LanguageInfo)i.next();
            if (!first && !i.hasNext())
                page.append(" and ");
            else if (!first)
                page.append(", ");
            page.append("<b>" + l.getLink() + "</b>");
            first = false;
        }
        page.append(" are mostly complete but still require some work, tests and revisions, plus " +
                    "additional missing translations to reach a reliable status.</li>");
                    
        page.append("<li>");
        first = true;
        for (Iterator i = started.iterator(); i.hasNext(); ) {
            LanguageInfo l = (LanguageInfo)i.next();
            if (!first && !i.hasNext())
                page.append(" and ");
            else if (!first)
                page.append(", ");
            page.append("<b>" + l.getLink() + "</b>");
            first = false;
        }
        page.append(" are only embryonic and actually need a highly wanted complete translation. " +
                    "The current files are only there for demonstration purpose.</li>");
        page.append("</ol>");
        
    }
    
    private void buildAfterStatus(StringBuffer page) {
        page.append(
  "<br><br>\n" +
  "<b>Which tool or editor is needed to work on a translation:</b><br>\n" + 
  "<br>\n" + 
  "For <b>Western European Latin-based languages</b>, which can use the US-ASCII  " +
  "or ISO-8859-1 character set, any text editor (such as NotePad on Windows) can " +
  "be used on Windows and Linux. Once a file is completed, it can be sent as a " +
  "simple text file to <script type=\"text/javascript\" language=\"JavaScript\"><!--\n" +
"// Protected email script by Joe Maller JavaScripts available at http://www.joemaller.com\n" +
"// This script is free to use and distribute but please credit me and/or link to my site.\n" + 
"emailE = ('limewire.org');\n" +
"emailE = ('translate' + '@' + emailE);\n" +
"document.write('<b><a href=\"mailto:' + emailE + '\">' + emailE + '</b></a>');\n" +
"//--></script><noscript><a href=\"#\">(Email address protected by JavaScript:\n" +
  "please enable JavaScript to contact me)</a></noscript>.<br>\n" +
  "<br>\n" +
  "For <b>Central European languages</b>, the prefered format is a simple text " +
  "file encoded with the ISO-8859-2 character set, or an UTF-8 encoded simple " +
  "text file (which can be edited with NotePad on Windows 2000/XP), or a " +
  "correctly marked-up HTML document such as HTML email, or a Word document.<br>\n" +
  "<br>\n" +
  "For <b>other European or Semitic languages</b>, the preferred format is a " +
  "plain text file, encoded with a ISO-8859-* character set which you must " +
  "explicitly specify, or a correctly marked-up HTML document, or a Word " +
  "document.<br>\n" +
  "<br>\n" +
  "For <b>Asian Languages</b>, the preferred submission format is a Unicode text " +
  "file encoded with UTF-8. Users of Windows 2000/XP can use NotePad but you " +
  "must explicitly select the UTF-8 encoding when saving your file. Users of " +
  "localized versions of Windows 95/98/ME can only save their file in the native " +
  "\"ANSI\" encoding, and should then send us their translation by " +
  "copy/pasting it in the content body of the email.<br>\n" +
  "<br>\n" +
  "<b>Macintosh users</b> should use a word-processor and send us their " +
  "translations in an unambiguous format. Mac OS 8/9 \"SimpleText\" " +
  "files use a Mac specific specific encoding for international languages " +
  "(additionally, SimpleText is too much limited to edit large files), and a " +
  "specific format for text attachments (we may enventually have difficulties to " +
  "decipher some Mac encodings used in simple text files attachment). On Mac OSX, " +
  "the best tool is \"TextEdit\", from the Jaguar accessories, with " +
  "which you can directly edit and save plain text files encoded with UTF-8. " +
  "<b>Linux users</b> can also participate if they have a " +
  "correctly setup environment for their locale. Files can be edited with " +
  "\"vi\", \"emacs\", or graphical editors for X11.<br>\n" +
  "<br>\n" +
  "For other information about internationalization standards, country and " +
  "language codes, character sets and encodings, you may visit these web pages:<br>\n" +
  "<a href=\"http://www.w3.org/International/O-charset.html\">http://www.w3.org/International/O-charset.html</a><br>\n" +
  "<a href=\"http://www.unicode.org/unicode/onlinedat/resources.html\">http://www.unicode.org/unicode/onlinedat/resources.html</a><br>\n" +
  "<br>\n" +
  "<b>How to submit corrections or enhancements for your " +
  "language:</b><br>\n" +
  "<br>\n" +
  "Users that don't have the correct tools to edit a Messages Bundle can send us " +
  "an Email in English or in French, that explain their needs.<br>\n" +
  "For any discussion with the contributing translators you may write to " +
  "<script type=\"text/javascript\" language=\"JavaScript\"><!--\n" +
"// Protected email script by Joe Maller JavaScripts available at http://www.joemaller.com\n" +
"// This script is free to use and distribute but please credit me and/or link to my site.\n" +
"emailE = ('limewire.org'); emailE = ('translate' + '@' + emailE);\n" +
"document.write('<b><a href=\"mailto:' + emailE + '\">' + emailE + '</b></a>');\n" +
"//--></script><noscript><a href=\"#\">(Email address protected by JavaScript:\n" +
  "please enable JavaScript to contact me)</a></noscript>.<br>\n" +
  "<br>\n" +
  "<b>If your corrections are significant</b>, you may send us your complete " +
  "Message Bundle. Please be sure to include all resource strings defined in the " +
  "latest version of the existing Messages Bundle before sending us your " +
  "revision.<br>\n" +
  "<br>\n" +
  "<b>For simple few corrections or additions</b>, just send the corrected lines " +
  "in the content body of an Email, with your comments. <i>We will review your " +
  "translations and integrate them into our existing versions after review.</i><br>\n" +
  "<br>\n" +
  "<b>How to test a new translation:</b><br>\n" +
  "<br>\n" +
  "Only Windows and Unix simple text editors can create a plain text file which " +
  "will work in LimeWire, and only for languages using the Western European " +
  "Latin character set. Don't use \"SimpleText\" on Mac OS to edit " +
  "properties files as SimpleText does not create plain text files. Other " +
  "translations need to be converted into regular properties files, encoded " +
  "using the ISO-8859-1 Latin character set and Unicode escape sequences, with a " +
  "tool \"native2ascii\" found in the Java Development Kit.<br>\n" +
  "<br>\n" +
  "You don't need to rename your translated and converted bundle, which can " +
  "coexist with the English version. LimeWire will load the appropriate " +
  "resources file according to the \"LANGUAGE=\", and " +
  "\"COUNTRY=\" settings stored in your \"limewire.props\" " +
  "preferences file. Lookup for the correct language code to use, in the list " +
  "beside.<br>\n" +
  "<br>\n" +
  "Until version 2.4 of LimeWire, bundles used by LimeWire were simply stored in " +
  "a single \"MessagesBundle.properties\" file of your installation " +
  "directory. However these versions support other bundles in the same " +
  "directory.<br>\n" +
  "<br>\n" +
  "Since version 2.5, bundles are searched in a single <b>zipped archive</b> " +
  "named \"MessagesBundles.jar\" installed with LimeWire. All bundles " +
  "are named \"MessagesBundle_xx.properties\", where \"xx\" is " +
  "replaced by the language code. " +
  "If you don't know how to proceed to test the translation " +
  "yourself, ask us for assistance at the same email address used for your " +
  "contributions.<br>\n" +
  "<br>\n" +
  "<b>How to create a new translation:</b><br>\n" +
  "<br>\n" +
  "Users that wish to contribute with a new translation must be fluent in the " +
  "target language, preferably native of a country where this language is " +
  "official. The English version is the reference one, but if needed you can " +
  "look for suggestions at the other completed translations. Before starting " +
  "your work, please contact us at " +
  "<script type=\"text/javascript\" language=\"JavaScript\"><!--\n" +
"// Protected email script by Joe Maller JavaScripts available at http://www.joemaller.com\n" +
"// This script is free to use and distribute but please credit me and/or link to my site.\n" +
"emailE = ('limewire.org'); emailE = ('translate' + '@' + emailE);\n" +
"document.write('<b><a href=\"mailto:' + emailE + '\">' + emailE + '</a></b>');\n" +
"//--></script><noscript><a href=\"#\">(Email address protected by JavaScript:\n" +
  "please enable JavaScript to contact me)</a></noscript>.\n" +
  "During the translation process, you may subscribe to the translate list (see above) " +
  "where you will benefit from other contributions sent to this address.<br>\n " + 
  "<br>\n " + 
  "Do not start with the existing Message Bundle installed with your current " +
  "servent. <b>Work on the latest version of a complete Messages Bundle</b> from " +
  "the list of languages on the right of this page (preferably the English " +
  "version or the French version which is constantly maintained in sync and is " +
  "fully translated). Look also to other existing translations if you need tips.<br>\n" +
  "<br>\n" +
  "When translating, adopt the <b>common terminology</b> used in your localized " +
  "operating system. In some cases, some terms were imported from English, " +
  "despite other terms already existed in your own language. If a local term can " +
  "be used unambiguously, please use it in preference to the English term, even " +
  "if you have seen many uses of this English term on web sites. A good " +
  "translation must be understood by most native people that are not addicted to " +
  "the Internet and computers \"jargon\". Pay particularly attention to " +
  "the translation of: download, upload, host, byte, firewall, port, file, " +
  "directory, # (number of), leaf (terminal node)...<br>\n" +
  "<br>\n" +
  "Avoid translating word for word, don't use blindly automatic translators, " +
  "be imaginative but make a <b>clear and concise</b> translation. For button " +
  "labels and column names, don't translate them with long sentences, as they " +
  "may be truncated; suppress some articles, or use abbreviations if needed. " +
  "If there are problems translating some difficult terms, write to the translate " +
  "list (in English or French) for assistance, and subscribe to this list to " +
  "receive comments from users or other translators.<br>\n" +
  "<br>\n" +
  "After you've plugged in your translations into your language's Messages " +
  "Bundle, send your copy as a plain text file attachement to " +
"<script type=\"text/javascript\" language=\"JavaScript\"><!--\n " +
"// Protected email script by Joe Maller JavaScripts available at http://www.joemaller.com\n" +
"// This script is free to use and distribute but please credit me and/or link to my site.\n" +
"emailE = ('limewire.org'); emailE = ('translate' + '@' + emailE);\n" +
"document.write('<b><a href=\"mailto:' + emailE + '\">' + emailE + '</a></b>');\n" +
"//--></script><noscript><a href=\"#\">(Email address protected by JavaScript:\n" +
  "please enable JavaScript to contact me)</a></noscript>.\n" +
  "<i>We will review " +
  "your translations and integrate them into our existing versions after review.</i><br><br>\n" +
  "<br>\n" +
 "</td>\n" + 
 "<td>&nbsp;&nbsp;&nbsp;</td>\n" +
 "<td valign=\"top\">\n" +
  "<table border=\"0\" cellspacing=\"1\" cellpadding=\"4\" bgcolor=\"#b1b1b1\" width=\"270\">\n" +
  "<tr bgcolor=\"#EFEFEF\">\n" + 
   "<td valign=\"top\"><br>\n" +
    "<b>LAST UPDATED: <font color=\"#FF0000\">" + df.format(new Date()) + "</font><br>\n" +
     "<br>\n" +
     "LATEST TRANSLATIONS & COMPLETION STATUS:</b><br>\n" +
     "<br>\n" +
     "To get the most recent version of a Messages Bundle, <b>click on the " +
     "corresponding language</b> in the following list.<br>\n" +
     "<br>\n" +
     "<font color=\"#FF0000\">To view all available Messages Bundle projects " +
     "(new languages may be added), <a " +
     "href=\"" + PRE_LINK + "\">click here</a></font>.\n"
    ); 
    }
    
    private void buildProgress(StringBuffer page, Map charsets) {
        page.append(
  "<table width=\"250\" border=\"0\" cellpadding=\"0\" cellspacing=\"4\">"
  );
        
        List latin = (List)charsets.remove("Latin");
        page.append(
  "<tr>\n" +
   "<td colspan=\"3\" valign=\"top\"><hr noshade size=\"1\">\n" +
    "Languages written with Latin (Western European) characters:</td>\n" +
  "</tr>\n" +
  "<tr>\n" +
   "<td valign=\"top\">\n" +
    "<a href=\"" + ENGLISH_LINK + "\" target=\"_blank\"><b>English</b> (US)</a>\n" +
   "</td>\n" +
   "<td align=\"right\">(default)</td>\n" +
   "<td>en</td>\n" + 
  "</tr>\n"
  );
        for (Iterator i = latin.iterator(); i.hasNext(); ) {
            LanguageInfo l = (LanguageInfo)i.next();
            page.append(
  "<tr>\n" +
   "<td><b>" + l.getLink() + "</b></td>\n" +
   "<td align=\"right\">(" + pc.format(l.getPercentage()) + ")</td>\n" +
   "<td>" + l.getCode() + "</td>\n" +
  "</tr>"
  );
        }
            
        for (Iterator i = charsets.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            String code = (String)entry.getKey();
            List l = (List)entry.getValue();
            page.append(
  "<tr>\n" +
   "<td colspan=\"3\" valign=\"top\"><hr noshade size=\"1\">\n" +
    "Languages written with " + code + " characters:</td>\n" +
  "</tr>\n"
  );
            for (Iterator j = l.iterator(); j.hasNext(); ) {
                LanguageInfo li = (LanguageInfo)j.next();
                page.append(
  "<tr>\n" +
   "<td><b>" + li.getLink() + "</b></td>\n" +
   "<td align=\"right\">(" + pc.format(li.getPercentage()) + ")</td>\n" +
   "<td>" + li.getCode() + "</td>\n" +
  "</tr>"
  );
            }
        }
        page.append(
  "</table>"
  );
    }
    
    private void buildEndOfPage(StringBuffer page) {
        page.append(
 "</td>\n" +
"</tr>\n" +
"</table>\n" +
	"</div>\n" +
    "</td>\n" +
"<!--#include virtual=\"/includes/bottom.html\" -->"
);
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
    private final String displayName;
    private final String fileName;
    private double percentage;
    
    /**
     * Constructs a new LanguageInfo object with the given
     * languageCode, countryCode, variantCode,
     * languageName, countryName, and variantName.
     */
    public LanguageInfo(String lc, String cc, String vc, String sc,
                        String ln, String cn, String vn, String sn,
                        String dn, String fn) {
        languageCode = lc.trim();
        countryCode  = cc.trim();
        variantCode  = vc.trim();
        scriptCode   = sc.trim();
        languageName = ln.trim();
        countryName  = cn.trim();
        variantName  = vn.trim();
        scriptName   = sn.trim();
        displayName  = dn.trim();
        fileName = fn.trim();
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
            !variantName.equals("") )
            return languageName + ", " + variantName + " (" + countryName + ")";
        else
            return languageName + " (" + countryName + ")";
    }
    
    /**
     * Used to map the list of locales to their properties data.
     * Must be unique per loaded properties file.
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
    
    public String getCode() {
        if (!variantCode.equals(""))
            return languageCode + "_" + countryCode + "_" + variantCode;
        if (!countryCode.equals(""))
            return languageCode + "_" + countryCode;
        return languageCode;
    }
    
    public String getScript() { return scriptName; }
    
    public String getFileName() { return fileName; }
    
    public String getName() { return displayName; }
    
    public String getLink() {
        return "<a href=\"" + CountPercent.PRE_LINK + fileName + "\">" + displayName + "</a>";
    }
    
    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
    
    public double getPercentage() {
        return percentage;
    }
}
