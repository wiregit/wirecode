package org.limewire.ui.swing.search.resultpanel;

import com.google.inject.Singleton;

@Singleton
public class SearchResultTruncatorImpl implements SearchResultTruncator {
    private static final String OPEN_TAG = "<b>";
    private static final String CLOSE_TAG = "</b>";
    private static final String ELLIPSIS = "...";
    private static final int ELLIPSIS_SHRINK_INCREMENT = ELLIPSIS.length() + 1;

    @Override
    public String truncateHeading(String headingText, int visibleWidthPixels, FontWidthResolver resolver) {
        //Strip HTML characters *except* <b>bold-wrapped</b> strings
        headingText = headingText.replaceAll("[<][/]?[\\w&&[^b]]*[>]", "");
        if (resolver.getPixelWidth(headingText) <= visibleWidthPixels) {
            return headingText;
        }
        
        //Strip multiple whitespace characters (spaces, \r, \n, \t) and embedded whitespace characters
        String truncated = headingText.replaceAll("[\\s]++", " ").replaceAll("[\\s&&[^ ]]", "");
        
        do {
            if (getEndEdge(truncated) >= (truncated.length() - (truncated.contains(ELLIPSIS) ? ELLIPSIS.length() : 0))) {
                truncated = ELLIPSIS + truncated.substring(ELLIPSIS_SHRINK_INCREMENT);
            } else if (getLeadEdge(truncated) >= 0) {
                truncated = truncated.substring(0, truncated.length() - ELLIPSIS_SHRINK_INCREMENT) + ELLIPSIS;
            }
        } while (resolver.getPixelWidth(truncated) > visibleWidthPixels);
        
        return truncated;
    }

    private int getLeadEdge(String headingText) {
        int indexOf = headingText.indexOf(OPEN_TAG);
        return indexOf == -1 ? 0 : indexOf;
    }

    private int getEndEdge(String headingText) {
        return headingText.indexOf(CLOSE_TAG) + CLOSE_TAG.length();
    }
}
