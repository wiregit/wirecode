package org.limewire.ui.swing.search.resultpanel;

class SearchResultTruncatorImpl implements SearchResultTruncator {
    private static final String OPEN_BOLD = "<b>";
    private static final String CLOSE_BOLD = "</b>";
    private static final String ELLIPSIS = "...";
    private static final int ELLIPSIS_SHRINK_INCREMENT = ELLIPSIS.length() + 1;

    @Override
    public String truncateHeading(String headingText, int visibleWidthPixels, FontWidthResolver resolver) {
        if (resolver.getPixelWidth(headingText) <= visibleWidthPixels) {
            return headingText;
        }
        
        String truncated = headingText;
        
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
        return headingText.indexOf(OPEN_BOLD);
    }

    private int getEndEdge(String headingText) {
        return headingText.indexOf(CLOSE_BOLD) + CLOSE_BOLD.length();
    }
}
