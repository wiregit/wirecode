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
"     internationalize LimeWire.&nbsp; If you are an avid English-speaking\n" +
"     user fluent in another language, we need your help!&nbsp; Helping\n" +
"     requires no programming knowledge and little computer savviness\n" +
"     beyond using a word processor.<br />\n" +
"     <br />\n"+
"     <b><big>How LimeWire Supports Multiple Languages</big></b><br />\n" + 
"     <br />\n" +
"     First, view this <a\n" +
"     href=\"http://www.limewire.com/img/screenshots/search.jpg\"\n" +
"     target=\"_blank\">LimeWire screen-shot</a>.&nbsp; Notice how the tabs\n" +
"     (<b>Search</b>, <b>Monitor</b>, <b>Library</b>, etc.) and the\n" +
"     buttons (<b>Download</b>, <b>Kill Download</b>, etc.) have text\n" +
"     on them.&nbsp; All elements of the LimeWire interface can be\n" +
"     translated to any language very easily.<br />\n" +
"     <br />\n" +
"     This translation is accomplished by packaging all the words of\n" +
"     the program into a &quot;message bundle&quot;.&nbsp; A message bundle is more or\n" +
"     less a list, with phrases corresponding to certain parts of the\n" +
"     software.&nbsp; There are message bundles for different languages, so\n" +
"     there is an English message bundle, a French message bundle, a\n" +
"     Japanese message bundle, etc.&nbsp; In English, the text for the\n" +
"     download button is &quot;Download&quot;, whereas in French the text is\n" +
"     &quot;Charger&quot; (which is French for &quot;download&quot;).<br />\n" +
"     <br />\n" +
"     When you start LimeWire, the program loads the appropriate\n" +
"     message bundle and uses its contents for any interface element\n" +
"     that has text on it.&nbsp; For instance, this is the <a\n" +
"     href=\"http://www.limewire.org/fisheye/viewrep/~raw,r=MAIN/limecvs/lib/messagebundles/MessagesBundle.properties\">English\n" +
"     message bundle</a>.&nbsp; Note the line:<br />\n" +
"     <blockquote>\n" +
"      <table border=\"0\" cellspacing=\"1\" cellpadding=\"4\" bgcolor=\"#b1b1b1\">\n" +
"       <tr bgcolor=\"#EFEFEF\">\n" +
"        <td><code>\n" +
"SEARCH_DOWNLOAD_BUTTON_LABEL=Download</code></td>\n" +
"       </tr>\n" +
"      </table>\n" +
"     </blockquote>\n" +
"     This line indicates that the label used on the download button on\n" +
"     the search tab should read &quot;Download&quot;.&nbsp; Contrast this with the\n" +
"     same line in the <a\n" +
"     href=\"http://www.limewire.org/fisheye/viewrep/~raw,r=MAIN/limecvs/lib/messagebundles/MessagesBundle_fr.properties\">French\n" +
"     message bundle</a>:<br />\n" +
"     <blockquote>\n" +
"      <table border=\"0\" cellspacing=\"1\" cellpadding=\"4\" bgcolor=\"#b1b1b1\">\n" +
"       <tr bgcolor=\"#EFEFEF\">\n" +
"        <td><code>\n" +
"#### SEARCH_DOWNLOAD_BUTTON_LABEL=Download<br />\n" +
"SEARCH_DOWNLOAD_BUTTON_LABEL=Charger</code></td>\n" +
"       </tr>\n" +
"      </table>\n" +
"     </blockquote>\n" +
"     Note that the line starting with a &quot;#&quot; is a comment line,\n" +
"     meaning it is not used by LimeWire.&nbsp; The English translation will always\n"+
"     be present as a reference.&nbsp; A label that is not yet translated in a bundle \n" +
"     will look like the following:<br />\n" +
"     <blockquote>\n" +
"      <table border=\"0\" cellspacing=\"1\" cellpadding=\"4\" bgcolor=\"#b1b1b1\">\n" +
"       <tr bgcolor=\"#EFEFEF\">\n" +
"        <td><code>\n" +
"#### SOME_NEW_LABEL=New!<br />\n" +
"#? SOME_NEW_LABEL=</code></td>\n" +
"       </tr>\n" +
"      </table>\n" +
"     </blockquote>\n" +
"     To provide a translation, one just needs to append the translated text after\n" +
"     the equal sign, and remove the leading comment mark and space.<br />\n" +
"     <br />\n");
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
"       may be difficult for native language users.&nbsp; Providing a more complete\n" +
"       translation would be very much appreciated.");
        buildStatus(newpage, langsEmbryonic,
"       are only embryonic and need a complete translation.&nbsp; \n" +
"       The current files are largely untranslated.");
        newpage.append(
"     </ol>\n");
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
"     For <b>Western European Latin-based languages</b>, which can use the US-ASCII\n" +
"     or ISO-8859-1 character set, any text editor (such as Notepad on Windows) can\n" +
"     be used on Windows and Linux.&nbsp; Once a file is completed, it can be sent as a\n" +
"     simple text file to\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br />\n" +
"     <br />\n" +
"     For <b>Central European languages</b>, the preferred format is a simple text\n" +
"     file encoded with the ISO-8859-2 character set, or a UTF-8 encoded simple\n" +
"     text file (which can be edited with Notepad on Windows 2000/XP), or a\n" +
"     correctly marked-up HTML document such as HTML email, or a Word document.<br />\n" +
"     <br />\n" +
"     For <b>other European languages</b>, the preferred format is a plain-text\n" +
"     file, encoded preferably with UTF-8 or a ISO-8859-* character set that\n" +
"     you must explicitly specify to us, or a correctly marked-up HTML document,\n" +
"     or a Word document.&nbsp; Please specify your working operating system and editor\n" +
"     you used to create plain-text files (we may support Windows codepages or Mac\n" +
"     charsets, but we will convert them to Unicode UTF-8 in our repository).<br />\n" +
"     <br />\n" +
"     For <b>Semitic languages</b> (Arabic, Hebrew...), the preferred format is a\n" +
"     plain-text file edited with a editor that supports the right-to-left layout,\n" +
"     encoded preferably with UTF-8 or a ISO-8859-* character set, in logical order.&nbsp; \n" +
"     Be careful with the relative order of keys and values, and with the\n" +
"     appearance of ASCII punctuations around right-to-left words: make sure that\n" +
"     your editor uses the RTL layout with the edited text aligned on the right;\n" +
"     please do not insert BiDi control overrides; but you may need to place LRE/PDF\n" +
"     marks (U+202B/U+202C) locally around non-Semitic words inserted within Semitic\n" +
"     sentences.&nbsp; Also the &quot;<code>\\n</code>&quot; sequences that encode a newline\n" +
"     will be displayed within semitic text as &quot;<code>n\\</code>&quot;: do not use\n" +
"     BiDi overrides for such special sequence whose appearance in your editor is not\n" +
"     important, but that MUST be entered with a leading backslash before the &quot;n&quot;\n" +
"     character.<br />\n" +
"     <br />\n" +
"     For <b>Asian Languages</b>, the preferred submission format is a Unicode text\n" +
"     file encoded with UTF-8.&nbsp; Users of Windows 2000/XP can use Notepad but you\n" +
"     must explicitly select the UTF-8 encoding when saving your file.&nbsp; Users of\n" +
"     localized versions of Windows 95/98/ME can only save their file in the native\n" +
"     local &quot;ANSI&quot; encoding, and should then send us their translation by\n" +
"     copying and pasting it in the content body of the email.<br />\n" +
"     <br />\n" +
"     <b>Mac users</b> should use a word processor and send us their\n" +
"     translations in an unambiguous format.&nbsp; On Mac OSX,\n" +
"     the best tool is &quot;TextEdit&quot;, from the Jaguar accessories, with\n" +
"     which you can directly edit and save plain-text files encoded with UTF-8.<br />\n" +
"     <br />\n" +
"     <b>Linux users</b> can also participate if they have a correct\n" +
"     environment for their locale.&nbsp; Files can be edited with &quot;vi&quot;,\n" +
"     &quot;emacs&quot;, or other editors.<br />\n" +
"     <br />\n" +
"     For further information about internationalization standards, language and\n" +
"     country codes, character sets and encodings, the following web pages may be helpful:<br />\n" +
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
"     ISO-8859 family, the Windows &quot;ANSI code pages&quot;, Macintosh character codes,\n" +
"     and Unicode (or its ISO/IEC 10646 repertoire) can be found at\n" +
"     <a href=\"http://www.cs.tut.fi/~jkorpela/chars.html\"\n" +
"     target=\"_blank\">http://www.cs.tut.fi/~jkorpela/chars.html</a>.<br />\n" +
"     <br />\n" +
"     <b><big>General Considerations for Translators</big></b><br />\n" +
"     <br />\n" +
"     During the translation process, you may <a\n" +
"     href=\"http://www.limewire.org/mailinglist.shtml\">subscribe to the translate mailing list</a>\n" +
"     where you may benefit from other translators' questions and knowledge.<br />\n" + 
"     <br />\n" + 
"     Do not start with the existing message bundle installed with your current\n" +
"     version of LimeWire.&nbsp; Make sure you<b>work on the latest version of a message bundle</b>.&nbsp; \n" +
"     You can get the latest bundle by clicking on a language in the list on the right side of this page.<br />\n" +
"     <br />\n" +
"     When translating, adopt the common terminology used in your localized\n" +
"     operating system.&nbsp; In some cases, some terms were imported from English,\n" +
"     despite other terms already existing in your own language.&nbsp; If a local term can\n" +
"     be used unambiguously, please use it in preference to the English term, even\n" +
"     if you have seen many uses of this English term on web sites.&nbsp; A good\n" +
"     translation must be understood by people who are not entirely savvy with\n" +
"     Internet and computer jargon.&nbsp; Pay particularly attention to the\n" +
"     non-technical translation of common terms: download, upload, host, byte,\n" +
"     firewall, address, file, directory, # (number of), leaf (terminal node), etc.<br />\n" +
"     <br />\n" +
"     Avoid translating word for word and do not use blindly automatic translators.&nbsp; \n" +
"     Be imaginative but make a clear and concise translation.&nbsp; For button\n" +
"     labels and column names, do not translate them with long sentences, as they\n" +
"     need to be short.&nbsp; Suppress some articles, or use abbreviations if necessary.&nbsp; \n" +
"     If there are problems translating some difficult terms, write to the translate\n" +
"     list (in English or French) for assistance.<br />\n" +
"     <br />\n" +
"     <b><big>How to Submit Corrections or Enhancements for Your Language</big></b><br />\n" +
"     <br />\n" +
"     Users that do not have the correct tools to edit a message bundle can send us\n" +
"     an email in English or in French that explains their needs.&nbsp; \n" +
"     For any discussion with the contributing translators you may write to\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br />\n" +
"     <br />\n" +
"     <b>If your corrections are significant</b>, you may send us your complete\n" +
"     message bundle.&nbsp; Please be sure to include all resource strings defined in the\n" +
"     latest version of the existing message bundle before sending us your\n" +
"     revision.<br />\n" +
"     <br />\n" +
"     <b>For simple few corrections or additions</b>, just send the corrected lines\n" +
"     in the content body of an email (making sure to select the correct character encoding\n" +
"     in your email tool before sending it so that non-ASCII characters are not lost or\n" +
"     replaced), with your comments.<br />\n" +
"     <br />\n" +
"     <i>We will review submitted translations and integrate\n" +
"     valuable contributions as quickly as possible.</i><br />\n" +
"     <br />\n" +
"     <b><big>How to Test a New Translation</big></b><br />\n" +
"     <br />\n" +
"     Only Windows and Unix simple text editors can create a plain-text file which\n" +
"     will work in LimeWire, and only for languages using the Western European\n" +
"     Latin character set.&nbsp; Do not use &quot;SimpleText&quot; on Mac OS to edit\n" +
"     properties files as SimpleText does not create plain-text files.&nbsp; Other\n" +
"     translations need to be converted into regular properties files, encoded\n" +
"     using the ISO-8859-1 Latin character set and Unicode escape sequences, with a\n" +
"     tool &quot;native2ascii&quot; found in the Java Development Kit.<br />\n" +
"     <br />\n" +
"     You do not need to rename your translated and converted bundle, which can\n" +
"     co-exist with the English version.&nbsp; LimeWire will load the appropriate\n" +
"     resource file according to the &quot;<code>LANGUAGE=</code>&quot;, and\n" +
"     &quot;<code>COUNTRY=</code>&quot; settings stored in your\n" +
"     &quot;<code>limewire.props</code>&quot; preferences file.&nbsp; The list on the right\n" +
"     can help you to find the correct language code to use.<br />\n" +
"     <br />\n" +
"     Bundles are stored in a single compressed archive\n" +
"     named &quot;MessagesBundles.jar&quot; installed with LimeWire.&nbsp; All bundles\n" +
"     are named &quot;MessagesBundle_xx.properties&quot;, where &quot;xx&quot; is replaced by\n" +
"     the language code.&nbsp; \n" +
"     Note that bundles for languages using non-Western European Latin characters\n" +
"     will be converted from UTF-8 to ASCII using a special format with hexadecimal\n" +
"     Unicode escape sequences, prior to their inclusion in this archive.&nbsp; This can be\n" +
"     performed using the <code>native2ascii</code> tool from the Java Development Kit.&nbsp; \n" +
"     If you do not know how to proceed to test the translation yourself,\n" +
"     ask us for assistance at\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br />\n" +
"     <br />\n" +
"     <b><big>How to Create a New Translation</big></b><br />\n" +
"     <br />\n" +
"     Users that wish to contribute with a new translation must be fluent in the\n" +
"     target language, preferably native of a country where this language is\n" +
"     official.&nbsp; Before starting\n" +
"     your work, please contact us at\n" +
      HTML_TRANSLATE_EMAIL_ADDRESS + ".<br />\n" +
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
            String charset = (String)entry.getKey();
            List l = (List)entry.getValue();
            newpage.append(
"       <tr>\n" +
"        <td colspan=\"3\" valign=\"top\">\n" +
"         <hr noshade size=\"1\">\n" +
"         Languages written with " + charset + " characters:</td>\n" +
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
"  </div>\n"); /* (div id="bod1") */
    }
}
