package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayConfig.HeadingOnly;
import static org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayConfig.HeadingAndSubheading;
import static org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayConfig.HeadingAndMetadata;
import static org.limewire.ui.swing.search.resultpanel.ListViewRowHeightRule.RowDisplayConfig.HeadingSubHeadingAndMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;

public class ListViewRowHeightRuleImpl implements ListViewRowHeightRule {
    private static final String EMPTY_STRING = "";
    private final Log LOG = LogFactory.getLog(getClass());
    private final PropertyKeyComparator AUDIO_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.GENRE, PropertyKey.BITRATE, PropertyKey.TRACK_NUMBER, PropertyKey.SAMPLE_RATE);
    private final PropertyKeyComparator VIDEO_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.YEAR, PropertyKey.RATING, PropertyKey.COMMENTS, PropertyKey.HEIGHT, 
                                  PropertyKey.WIDTH, PropertyKey.BITRATE);
    private final PropertyKeyComparator DOCUMENTS_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.DATE_CREATED, PropertyKey.AUTHOR);
    private final PropertyKeyComparator PROGRAMS_COMPARATOR = 
        new PropertyKeyComparator(PropertyKey.PLATFORM, PropertyKey.COMPANY);

    @Override
    public RowDisplayResult getDisplayResult(VisualSearchResult vsr, String searchText) {
        if (vsr.isSpam()) {
            return new RowDisplayResultImpl(HeadingOnly, vsr.getHeading(), null, null, vsr.isSpam());
        }
            
        switch(vsr.getDownloadState()) {
        case DOWNLOADING:
        case DOWNLOADED:
        case LIBRARY:
            return new RowDisplayResultImpl(HeadingOnly, vsr.getHeading(), null, null, vsr.isSpam());
        case NOT_STARTED:
            String heading = vsr.getHeading();
            String highlightedHeading = highlightMatches(heading, searchText);
            
            LOG.debugf("Heading: {0} highlightedMatches: {1}", heading, highlightedHeading);
            
            String subheading = vsr.getSubHeading();

            String highlightedSubheading = highlightMatches(subheading, searchText);
            
            LOG.debugf("Subheading: {0} highlightedMatches: {1}", subheading, highlightedSubheading);

            if (!isDifferentLength(heading, highlightedHeading) && !isDifferentLength(subheading, highlightedSubheading)) {
            
                PropertyMatch propertyMatch = getPropertyMatch(vsr, searchText);
                
                return newResult(HeadingSubHeadingAndMetadata, vsr, highlightedHeading, highlightedSubheading, propertyMatch);
            }
            
            return newResult(HeadingAndSubheading, vsr, highlightedHeading, highlightedSubheading, null);
            
        default:
            throw new UnsupportedOperationException("Unhandled download state: " + vsr.getDownloadState());
        }
    }

    private RowDisplayResultImpl newResult(RowDisplayConfig config, VisualSearchResult vsr, String heading,
            String subheading, PropertyMatch propertyMatch) {
        if (emptyOrNull(subheading)) {
            if (propertyMatch == null || (propertyMatch.getKey() == null || EMPTY_STRING.equals(propertyMatch.getKey()))) {
                config = HeadingOnly;
            } else {
                config = HeadingAndMetadata;
            }
        }
        
        return new RowDisplayResultImpl(config, heading, propertyMatch, subheading, vsr.isSpam());
    }

    private boolean emptyOrNull(String val) {
        return val == null || EMPTY_STRING.equals(val.trim());
    }
    
    private PropertyMatch getPropertyMatch(VisualSearchResult vsr, String searchText) {
        if(searchText == null)
            return null;
        
        ArrayList<PropertyKey> properties = new ArrayList<PropertyKey>(vsr.getProperties().keySet());
        Collections.sort(properties, getComparator(vsr));
        for (PropertyKey key : properties) {
            String value = vsr.getPropertyString(key);

            String propertyMatch = highlightMatches(value, searchText);
            if (value != null && isDifferentLength(value, propertyMatch)) {
                String betterKey = key.toString().toLowerCase();
                betterKey = betterKey.replace('_', ' ');
                return new PropertyMatchImpl(betterKey, propertyMatch);
            }
        }

        // No match found.
        return null;
    }
    
    private boolean isDifferentLength(String str1, String str2) {
        return str1 != null && str2 != null && str1.length() != str2.length();
    }
    
    private Comparator<PropertyKey> getComparator(VisualSearchResult vsr) {
        switch (vsr.getCategory()) {
        case AUDIO:
            return AUDIO_COMPARATOR;
        case VIDEO:
            return VIDEO_COMPARATOR;
        case DOCUMENT:
            return DOCUMENTS_COMPARATOR;
        default:
            return PROGRAMS_COMPARATOR;
        }
    }
    
    /**
     * Adds an HTML bold tag around every occurrence of highlightText.
     * Note that comparisons are case insensitive.
     * @param text the text to be modified
     * @return the text containing bold tags
     */
    private String highlightMatches(String sourceText, String searchText) {
        boolean haveSearchText = searchText != null && searchText.length() > 0;

        // If there is no search or filter text then return sourceText as is.
        if (!haveSearchText)
            return sourceText;
        
        return SearchHighlightUtil.highlight(searchText, sourceText);
    }
    
    private static class RowDisplayResultImpl implements RowDisplayResult {
        private final RowDisplayConfig config;
        private final String heading;
        private final String subheading;
        private final PropertyMatch metadata;
        private final boolean spam;
        
        public RowDisplayResultImpl(RowDisplayConfig config, String heading, PropertyMatch metadata,
                String subheading, boolean spam) {
            this.config = config;
            this.heading = heading;
            this.metadata = metadata;
            this.subheading = subheading;
            this.spam = spam;
        }

        @Override
        public RowDisplayConfig getConfig() {
            return config;
        }

        @Override
        public String getHeading() {
            return heading;
        }

        @Override
        public PropertyMatch getMetadata() {
            return metadata;
        }

        @Override
        public String getSubheading() {
            return subheading;
        }

        @Override
        public boolean isSpam() {
            return spam;
        }
    }
    
    private static class PropertyMatchImpl implements PropertyMatch {
        private final String highlightedVal;
        private final String key;
        
        public PropertyMatchImpl(String key, String highlightedVal) {
            this.key = key;
            this.highlightedVal = highlightedVal;
        }

        @Override
        public String getHighlightedValue() {
            return highlightedVal;
        }

        @Override
        public String getKey() {
            return key;
        }
    }
    
    private static class PropertyKeyComparator implements Comparator<PropertyKey> {
        private final PropertyKey[] keyOrder;

        public PropertyKeyComparator(PropertyKey... keys) {
            this.keyOrder = keys;
        }

        @Override
        public int compare(PropertyKey o1, PropertyKey o2) {
            if (o1 == o2) {
                return 0;
            }
            
            for(PropertyKey key : keyOrder) {
                if (o1 == key) {
                    return -1;
                } else if (o2 == key) {
                    return 1;
                }
            }
            return 0;
        }
    }
}
