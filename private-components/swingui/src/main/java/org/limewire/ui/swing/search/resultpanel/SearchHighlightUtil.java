package org.limewire.ui.swing.search.resultpanel;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

class SearchHighlightUtil {
    private static final Log LOG = LogFactory.getLog(SearchHighlightUtil.class);

    private SearchHighlightUtil() {
    }

    public static String highlight(String search, String content) {
        if (content == null) {
            return "";
        }
        
        if (search == null) {
            return content;
        }
        
        StringBuilder bldr = new StringBuilder();
        int index = 0;
        Matcher matcher = Pattern.compile("\\b(" + Matcher.quoteReplacement(search.replaceAll(" ", "|")) + ")", Pattern.CASE_INSENSITIVE).matcher(content);
        index = 0;
        while (matcher.find()) {
            MatchResult result = matcher.toMatchResult();

            int startIndex = result.start();
            bldr.append(content.substring(index, startIndex));
            String word = result.group();
            bldr.append("<b>").append(word).append("</b>");
            index = matcher.end();

            LOG.debugf("Start: {0} url: {1} end: {2}", startIndex, word, matcher.end());
        }

        if (bldr.length() > 0) {
            bldr.append(content.substring(index));
            return "<html>" + bldr.toString() + "</html>";
        }
        return content;
    }
}
