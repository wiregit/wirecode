package com.limegroup.gnutella.i18n;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Writes language info out in HTML format.
 */
class HTMLOutput {

    /** @see LanguageInfo#getLink() */
    static final String PRE_LINK = "http://www.limewire.org/fisheye/viewrep/~raw,r=MAIN/limecvs/lib/messagebundles/";
    private static final String DEFAULT_LINK = PRE_LINK + LanguageLoader.BUNDLE_NAME + LanguageLoader.PROPS_EXT;

    /** constant link to the translate mailing list. */
    private static final String HTML_TRANSLATE_EMAIL_ADDRESS =
"<script type=\"text/javascript\" language=\"JavaScript\"><!--\n" +
"// Protected email script by Joe Maller JavaScripts available at http://www.joemaller.com\n" +
"// This script is free to use and distribute but please credit me and/or link to my site.\n" + 
"e_mA_iL_E = ('limewire' + \"&#46;\" + 'org'); e_mA_iL_E = ('translate' + \"&#64;\" + e_mA_iL_E);\n" +
"document.write('<a href=\"mai' + \"lto:\" + e_mA_iL_E + '\">' + e_mA_iL_E + '</a>');\n" +
"//--></script><noscript><a href=\"#\">[Email address protected by JavaScript:\n" +
"please enable JavaScript to contact me]</a></noscript>";

    /** minimum completion levels for the status HTML page */
    private static final double MIN_PERCENTAGE_COMPLETED     = 0.75;
    private static final double MIN_PERCENTAGE_NEED_REVISION = 0.65;
    private static final double MIN_PERCENTAGE_MIDWAY        = 0.45;
    private static final int    MIN_COUNT_STARTED            = 20;
    
    private final StringBuffer page;

    private final DateFormat df;
    private final NumberFormat pc;
    private final Map/*<String code, LanguageInfo li>*/ langs;
    private final int basicTotal;

    /**
     * Constructs a new HTML output.
     * @param df
     * @param pc
     * @param langs
     * @param basicTotal
     */
    HTMLOutput(DateFormat df, NumberFormat pc, Map langs, int basicTotal) {
        this.df = df;
        this.pc = pc;
        this.langs = langs;
        this.basicTotal = basicTotal;
        this.page = buildHTML();
    }
    
    /**
     * Creates the HTML.
     * @return the HTML page in a StringBuffer.
     */
    StringBuffer buildHTML() {
        List langsCompleted = new LinkedList();
        List langsNeedRevision = new LinkedList();
        List langsMidway = new LinkedList();
        List langsStarted = new LinkedList();
        List langsEmbryonic = new LinkedList();
        Map charsets = new TreeMap();
        
        for (Iterator i = langs.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)i.next();
            //final String code = (String)entry.getKey();
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
        
        StringBuffer newpage = new StringBuffer();
        buildStartOfPage(newpage);
        buildStatus(newpage, langsCompleted,
                          langsNeedRevision,
                          langsMidway,
                          langsStarted,
                          langsEmbryonic);
        buildAfterStatus(newpage);
        buildProgress(newpage, charsets);
        buildEndOfPage(newpage);
        return newpage;
    }
        
    /**
     * Prints the HTML to 'out'.
     * @param out
     */
    void printHTML(PrintStream out) {
        /* Make sure printed page contains only ASCII, converting all
         * other code points to decimal NCRs. This will work whatever
         * charset will be selected by the user's browser.
         */
        int pageLength = page.length();
        for (int index = 0; index < pageLength; ) {
            int c = page.charAt(index++); // char's are always positive
            if (c < 160) { /* C0 or Basic Latin or C1 */
                if (c >= 32 && c < 127 || c == '\t') /* Basic Latin or TAB */
                    out.print((char)c);
                else if (c == '\n') /* LF */
                    out.println(); /* platform's newline sequence */
                /* ignore all other C0 and C1 controls */
            } else { /* Use NCRs */
                /* special check for surrogate pairs */
                if (c >= 0xD800 && c <= 0xDBFF && index < pageLength) {
                    char c2 = page.charAt(index);
                    if (c2 >= 0xDC00 && c2 <= 0xDFFF) {
                        c = 0x10000 + ((c - 0xD800) << 10) + (c2 - 0xDC00);
                        index++;
                    }
                }
                out.print("&#");
                out.print(c);//decimal NCR notation
                out.print(';');
            }
        }
    }
    
    /**
     * Builds the start of the page.
     */
    private void buildStartOfPage(StringBuffer newpage) {
        newpage.append(
"  <div id=\"bod1\">\n" +
"   <h1>Help Internationalize LimeWire!</h1>\n" +
"   <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n" + /* Three columns */
"   <tr>\n" +
"    <td valign=\"top\" style=\"line-height: 16px;\">\n" + /* Start column 1 (main content) */
"     The LimeWire Open Source Project has embarked on an effort to\n" +
"     internationalize LimeWire.  If you are an avid English-speaking\n" +
"     user fluent in another language, we need your help!  Helping\n" +
"     requires no programming knowledge and little computer savviness\n" +
"     beyond using a word processor.<br /><br />\n" +
"     \n"+
"     <b><big>How LimeWire Supports Multiple Languages</big></b><br /><br />\n" + 
"     \n" +
"     First, view this <a\n" +
"     href=\"http://www.limewire.com/img/screenshots/search.jpg\"\n" +
"     target=\"_blank\">LimeWire screen-shot</a>.  Notice how the tabs\n" +
"     (<b>Search</b>, <b>Monitor</b>, <b>Library</b>, etc.) and the\n" +
"     buttons (<b>Download</b>, <b>Kill Download</b>, etc.) have text\n" +
"     on them.  All elements of the LimeWire interface can be\n" +
"     translated to any language very easily.<br /><br />\n" +
"     \n" +
"     This translation is accomplished by packaging all the words of\n" +
"     the program into a &quot;message bundle&quot;.  A message bundle is more or\n" +
"     less a list, with phrases corresponding to certain parts of the\n" +
"     software.  There are message bundles for different languages, so\n" +
"     there is an English message bundle, a French message bundle, a\n" +
"     Japanese message bundle, etc.  In English, the text for the\n" +
"     download button is &quot;Download&quot;, whereas in French the text is\n" +
"     &quot;Charger&quot; (which is French for &quot;download&quot;).<br /><br />\n" +
"     \n" +
"     When you start LimeWire, the program loads the appropriate\n" +
"     message bundle and uses its contents for any interface element\n" +
"     that has text on it.  For instance, this is the <a href=\"http://www.limewire.org/fisheye/viewrep/~raw,r=MAIN/limecvs/lib/messagebundles/MessagesBundle.properties\">English message bundle</a>.  Note the line:\n" +
"     \n" +
"     <pre>\n" +
"       SEARCH_DOWNLOAD_BUTTON_LABEL=Download</pre>\n" +
"     This line indicates that the label used on the download button on\n" +
"     the search tab should read &quot;Download&quot;.  Contrast this with the\n" +
"     same line in the <a\n" +
"     href=\"http://www.limewire.org/fisheye/viewrep/~raw,r=MAIN/limecvs/lib/messagebundles/MessagesBundle_fr.properties\">French\n" +
"     message bundle</a>:\n" +
"     \n" +
"     <pre>\n" +
"       #### SEARCH_DOWNLOAD_BUTTON_LABEL=Download\n" +
"       SEARCH_DOWNLOAD_BUTTON_LABEL=Charger</pre>\n" +
"     Note that the line starting with a &quot;#&quot; is a comment line,\n" +
"     meaning it is not used by LimeWire.  The English translation will always\n"+
"     be present as a reference.<br /><br />\n" +
"     \n" +
"     \n");
    }
    
    /**
     * Builds the status portion of the page.
     */
    private void buildStatus(StringBuffer newpage,
                             List langsCompleted,
                             List langsNeedRevision,
                             List langsMidway,
                             List langsStarted,
                             List langsEmbryonic) {
        newpage.append(
"     <b><big>Translation Status</big></b>\n" +
"     <br />\n" +
"     <ol>\n");

	//  ### Need subject-verb agreement here; need to check if
	//  ### size of list == 1 or singular versus plural
        buildStatus(newpage, langsCompleted,
"       are complete and will require only small revisions during the project\n" +
"       evolution.");
        buildStatus(newpage, langsNeedRevision,
"       are mostly complete and can still be used reliably, but may need some\n" +
"       revisions and a few missing translations to work best with newest versions.");
        buildStatus(newpage, langsMidway,
"       have been completed for an old version, but may now require some work, tests\n" +
"       and revisions plus additional missing translations to reach a reliable status.");
        buildStatus(newpage, langsStarted,
"       are partly translated but still unfinished, and their use in LimeWire\n" +
"       may be difficult for native language users. Providing a more complete\n" +
"       translation would be very much appreciated.");
        buildStatus(newpage, langsEmbryonic,
"       are only embryonic and actually need a complete translation.\n" +
"       The current files are only there for demonstration purpose.");
        newpage.append(
"     </ol>\n" +
"     \n" +
"     \n");
    }
    
    /**
     * Builds an individual bullet point in the status portion of the page.
     */
    private void buildStatus(StringBuffer newpage,
                             List langsList, String status) {
        boolean first = true;
        for (Iterator i = langsList.iterator(); i.hasNext(); ) {
            LanguageInfo l = (LanguageInfo)i.next();
            if (first)
                newpage.append(
"      <li>\n");
            else if (!i.hasNext())
                newpage.append(" and\n");
            else
                newpage.append(",\n");
            newpage.append(
"       " + l.getLink());
            first = false;
        }
        if (!first)
            newpage.append("\n" + status + "</li>\n");
    }

    /**
     * Builds the info after the status portion.
     */
    private void buildAfterStatus(StringBuffer newpage) {
        newpage.append(
"     <br />\n" +
"     <b><big>Which Tool or Editor Is Needed to Work on Translations?</big></b><br />\n" + 
"     <br />\n" + 
"     For <b>Western European Latin-based languages</b>, which can use the US-ASCII \n" +
"     or ISO-8859-1 character set, any text editor (such as NotePad on Windows) can\n" +
"     be used on Windows and Linux. Once a file is completed, it can be sent as a\n" +
"     simple text file to\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br />\n" +
"     <br />\n" +
"     For <b>Central European languages</b>, the preferred format is a simple text\n" +
"     file encoded with the ISO-8859-2 character set, or an UTF-8 encoded simple\n" +
"     text file (which can be edited with NotePad on Windows 2000/XP), or a\n" +
"     correctly marked-up HTML document such as HTML email, or a Word document.<br /><br />\n" +
"     \n" +
"     For <b>other European languages</b>, the preferred format is a plain-text\n" +
"     file, encoded preferably with UTF-8 or a ISO-8859-* character set that\n" +
"     you must explicitly specify to us, or a correctly marked-up HTML document,\n" +
"     or a Word document. Please specify your working operating system and editor\n" +
"     you used to create plain-text files (we may support Windows codepages or Mac\n" +
"     charsets, but we will convert them to Unicode UTF-8 in our repository).<br /><br />\n" +
"     \n" +
"     For <b>Semitic languages</b> (Arabic, Hebrew...), the preferred format is a\n" +
"     plain-text file edited with a editor that supports the right-to-left layout,\n" +
"     encoded preferably with UTF-8 or a ISO-8859-* character set, in logical order.\n" +
"     Be careful with the relative order of keys and values, and with the\n" +
"     appearance of ASCII punctuations around right-to-left words: make sure that\n" +
"     your editor uses the RTL layout with the edited text aligned on the right;\n" +
"     please don't insert BiDi control overrides; but you may need to place LRE/PDF\n" +
"     marks (U+202B/U+202C) locally around non-Semitic words inserted within Semitic\n" +
"     sentences. Also the &quot;<code>\\n</code>&quot; sequences that encode a newline\n" +
"     will be displayed within semitic text as &quot;<code>n\\</code>&quot;: don't use\n" +
"     BiDi overrides for such special sequence whose appearance in your editor is not\n" +
"     important, but that MUST be entered with a leading backslash before the &quot;n&quot;\n" +
"     character.<br /><br />\n" +
"     \n" +
"     For <b>Asian Languages</b>, the preferred submission format is a Unicode text\n" +
"     file encoded with UTF-8. Users of Windows 2000/XP can use NotePad but you\n" +
"     must explicitly select the UTF-8 encoding when saving your file. Users of\n" +
"     localized versions of Windows 95/98/ME can only save their file in the native\n" +
"     local &quot;ANSI&quot; encoding, and should then send us their translation by\n" +
"     copy/pasting it in the content body of the email.<br /><br />\n" +
"     \n" +
"     <b>Macintosh users</b> should use a word-processor and send us their\n" +
"     translations in an unambiguous format. Mac OS 8/9 &quot;SimpleText&quot;\n" +
"     files use a Mac-specific encoding for international languages\n" +
"     (additionally, SimpleText is too much limited to edit large files), and a\n" +
"     specific format for text attachments (we may enventually have difficulties to\n" +
"     decipher some Mac encodings used in simple text files attachment). On Mac OSX,\n" +
"     the best tool is &quot;TextEdit&quot;, from the Jaguar accessories, with\n" +
"     which you can directly edit and save plain-text files encoded with UTF-8.<br /><br />\n" +
"     \n" +
"     <b>Linux users</b> can also participate if they have a correctly setup\n" +
"     environment for their locale. Files can be edited with &quot;vi&quot;, &quot;emacs&quot;, or\n" +
"     graphical editors for X11.<br /><br />\n" +
"     \n" +
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
"     ISO-8859 family, the Windows &quot;ANSI code pages&quot;, Macintosh character codes, and\n" +
"     Unicode (or its ISO/IEC 10646 repertoire) can be found on:\n" +
"     <a href=\"http://www.cs.tut.fi/~jkorpela/chars.html\"\n" +
"     target=\"_blank\">http://www.cs.tut.fi/~jkorpela/chars.html</a></li><br /><br />\n" +
"     \n" +
"     \n" +
"     \n" +
"     <b><big>How to Submit Corrections or Enhancements for Your Language</big></b><br />\n" +
"     <br />\n" +
"     Users that do not have the correct tools to edit a message bundle can send us\n" +
"     an Email in English or in French that explains their needs. \n" +
"     For any discussion with the contributing translators you may write to\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br /><br />\n" +
"     \n" +
"     <b>If your corrections are significant</b>, you may send us your complete\n" +
"     message bundle. Please be sure to include all resource strings defined in the\n" +
"     latest version of the existing message bundle before sending us your\n" +
"     revision.<br /><br />\n" +
"     \n" +
"     <b>For simple few corrections or additions</b>, just send the corrected lines\n" +
"     in the content body of an email (making sure to select the correct character encoding\n" +
"     in your email tool before sending it so that non-ASCII characters are not lost or\n" +
"     replaced), with your comments. <i>We will review your translations and integrate\n" +
"     them into our existing versions after review.</i><br /><br />\n" +
"     \n" +
"     <b><big>How to Test a New Translation:</big></b><br /><br />\n" +
"     \n" +
"     Only Windows and Unix simple text editors can create a plain-text file which\n" +
"     will work in LimeWire, and only for languages using the Western European\n" +
"     Latin character set. Don't use &quot;SimpleText&quot; on Mac OS to edit\n" +
"     properties files as SimpleText does not create plain-text files. Other\n" +
"     translations need to be converted into regular properties files, encoded\n" +
"     using the ISO-8859-1 Latin character set and Unicode escape sequences, with a\n" +
"     tool &quot;native2ascii&quot; found in the Java Development Kit.<br /><br />\n" +
"     \n" +
"     You do not need to rename your translated and converted bundle, which can\n" +
"     coexist with the English version. LimeWire will load the appropriate\n" +
"     resources file according to the &quot;<code>LANGUAGE=</code>&quot;, and\n" +
"     &quot;<code>COUNTRY=</code>&quot; settings stored in your &quot;<code>limewire.props</code>&quot;\n" +
"     preferences file. Lookup for the correct language code to use, in the list\n" +
"     beside.<br /><br />\n" +
"     \n" +
"     Bundles are stored in a single <b>zipped archive</b>\n" +
"     named &quot;MessagesBundles.jar&quot; installed with LimeWire. All bundles\n" +
"     are named &quot;MessagesBundle_xx.properties&quot;, where &quot;xx&quot; is replaced by\n" +
"     the language code (as shown on the table beside).\n" +
"     Note that bundles for languages using non-Western European Latin characters\n" +
"     will be converted from UTF-8 to ASCI using a special format with hexadecimal\n" +
"     Unicode escape sequences, prior to their inclusion in this archive. This can be\n" +
"     performed using the <code>native2ascii</code> tool from the Java Development Kit.\n" +
"     If you don't know how to proceed to test the translation yourself,\n" +
"     ask us for assistance at the same email address used for your contributions.<br /><br />\n" +
"     \n" +
"     <b><big>How to Create a New Translation</big></b><br /><br />\n" +
"     \n" +
"     Users that wish to contribute with a new translation must be fluent in the\n" +
"     target language, preferably native of a country where this language is\n" +
"     official. Before starting\n" +
"     your work, please contact us at\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br /><br />\n" +
"     \n" +
"     During the translation process, you may subscribe to the translate list (see above)\n" +
"     where you will benefit from other contributions sent to this address.<br /><br />\n" + 
"     \n" + 
"     Do not start with the existing message bundle installed with your current\n" +
"     servent. <b>Work on the latest version of a message bundle</b> from\n" +
"     the list of languages on the top right of this page.<br /><br />\n" +
"     \n" +
"     When translating, adopt the <b>common terminology</b> used in your localized\n" +
"     operating system. In some cases, some terms were imported from English,\n" +
"     despite other terms already existed in your own language. If a local term can\n" +
"     be used unambiguously, please use it in preference to the English term, even\n" +
"     if you have seen many uses of this English term on web sites. A good\n" +
"     translation must be understood by most native people that are not addicted to\n" +
"     the Internet and computers &quot;jargon&quot;.  Pay particularly attention to the\n" +
"     non-technical translation of common terms: download, upload, host, byte,\n" +
"     firewall, address, file, directory, # (number of), leaf (terminal node)...<br /><br />\n" +
"     \n" +
"     Avoid translating word for word, do not use blindly automatic translators,\n" +
"     be imaginative but make a <b>clear and concise</b> translation. For button\n" +
"     labels and column names, don't translate them with long sentences, as they\n" +
"     may be truncated; suppress some articles, or use abbreviations if needed.\n" +
"     If there are problems translating some difficult terms, write to the translate\n" +
"     list (in English or French) for assistance, and subscribe to this list to\n" +
"     receive comments from users or other translators.<br /><br />\n" +
"     \n" +
"     After you've plugged in your translations into your language's message\n" +
"     bundle, send your copy as a plain-text file attachment to\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br /><br />\n" +
"     \n" +
"     <i>We will review your translations and integrate them into our existing\n" +
"     versions after review.</i><br /><br /><br />\n"+
"     \n" +
"     \n" +
"    </td>\n" + /* End of column 1 (spacing) */
"    <td>&nbsp;&nbsp;&nbsp;</td>\n" + /* Column 2 (spacing) */
"    <td valign=\"top\">\n" + /* Start of column 3 (status) */
      /* Start shaded right rectangle */
"     <table border=\"0\" cellspacing=\"1\" cellpadding=\"4\" bgcolor=\"#b1b1b1\" width=\"270\">\n" +
"     <tr bgcolor=\"#EFEFEF\">\n" + 
"      <td valign=\"top\"><br />\n" +
"       <b>LAST UPDATED: <font color=\"#FF0000\">" + df.format(new Date()) + "</font><br />\n" +
"       <br />\n" +
"       To get the most recent version of a message bundle, <b>click on the\n" +
"       corresponding language</b> in the following list.<br />\n" +
"       <br />\n" +
"       LATEST TRANSLATIONS STATUS:</b><br />\n");
    }
    
    /**
     * Builds the progress table.
     */
    private void buildProgress(StringBuffer newpage, Map charsets) {
        newpage.append(
"       <table width=\"250\" border=\"0\" cellpadding=\"0\" cellspacing=\"4\">");
        List latin = (List)charsets.remove("Latin");
        newpage.append(
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
            newpage.append(
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
            newpage.append(
"       <tr>\n" +
"        <td colspan=\"3\" valign=\"top\">\n" +
"         <hr noshade size=\"1\">\n" +
"         Languages written with " + code + " characters:</td>\n" +
"       </tr>\n");
            for (Iterator j = l.iterator(); j.hasNext(); ) {
                LanguageInfo li = (LanguageInfo)j.next();
                newpage.append(
"       <tr>\n" +
"        <td><b>" + li.getLink() + "</b></td>\n" +
"        <td align=\"right\">(" + pc.format(li.getPercentage()) + ")</td>\n" +
"        <td>" + li.getCode() + "</td>\n" +
"       </tr>\n");
            }
        }
        newpage.append(
"       </table>\n");
    }
    
    /**
     * Builds the closing footers of the page.
     */
    private void buildEndOfPage(StringBuffer newpage) {
        newpage.append(
"      </td>\n" +
"     </tr>\n" +
"     </table>\n" + /* End of shaded right rectangle */
"    </td>\n" + /* End of column 3 (status) */
"   </tr>\n" +
"   </table>\n" + /* End of the 3 columns table below the title */
"  </div>\n"/* (div id="bod1") */
);
    }
}
