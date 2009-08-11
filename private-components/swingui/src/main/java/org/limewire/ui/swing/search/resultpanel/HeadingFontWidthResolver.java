package org.limewire.ui.swing.search.resultpanel;

import java.awt.Font;
import java.awt.FontMetrics;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.limewire.ui.swing.search.resultpanel.SearchResultTruncator.FontWidthResolver;

/**
 * Implementation of FontWidthResolver to determine pixel width of text within
 * a JEditorPane component.
 */
public class HeadingFontWidthResolver implements FontWidthResolver {

    private static final String EMPTY_STRING = "";
    
    //finds <b>foo</b> or <a href="#foo"> or {1} patterns
    //**NOTE** This does not account for all HTML sanitizing, just for HTML
    //**NOTE** that would have been added by search result display code
    private final Pattern findHTMLTagsOrReplacementTokens = Pattern.compile("([<][/]?[\\w =\"#]*[>])|([{][\\d]*[}])");
    private final Matcher matcher = findHTMLTagsOrReplacementTokens.matcher("");
    private final JEditorPane editorPane;
    private final Font headingFont;
    
    /**
     * Constructs a HeadingFontWidthResolver.
     */
    public HeadingFontWidthResolver(JEditorPane editorPane, Font headingFont) {
        this.editorPane = editorPane;
        this.headingFont = headingFont;
    }
    
    @Override
    public int getPixelWidth(String text) {
        HTMLEditorKit editorKit = (HTMLEditorKit) editorPane.getEditorKit();
        StyleSheet css = editorKit.getStyleSheet();
        FontMetrics fontMetrics = css.getFontMetrics(headingFont);
        matcher.reset(text);
        text = matcher.replaceAll(EMPTY_STRING);
        return fontMetrics.stringWidth(text);
    }
}
