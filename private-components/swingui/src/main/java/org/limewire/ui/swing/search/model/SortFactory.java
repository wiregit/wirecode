package org.limewire.ui.swing.search.model;

import static org.limewire.util.Objects.compareToNull;
import static org.limewire.util.Objects.compareToNullIgnoreCase;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.store.StoreResult.SortPriority;

/**
 * Factory class for creating sort comparators.
 */
public class SortFactory {

    /**
     * Returns a search result Comparator for the specified sort option.
     */
    @SuppressWarnings("unchecked")
    public static Comparator<VisualSearchResult> getSortComparator(SortOption sortOption) {
        switch (sortOption) {
        case ALBUM:
            return getStringPropertyPlusNameComparator(FilePropertyKey.ALBUM, true);

        case ARTIST:
            return getStringPropertyPlusNameComparator(FilePropertyKey.AUTHOR, true);

        case COMPANY:
            return getStringPropertyPlusNameComparator(FilePropertyKey.COMPANY, true);

        case DATE_CREATED:
            return new SimilarResultsGroupingDelegateComparator(
                    getPriorityComparator(false), getDateComparator(FilePropertyKey.DATE_CREATED, false), getNameComparator(true)); 

        case FILE_EXTENSION:
        case TYPE:
            return new SimilarResultsGroupingDelegateComparator(
                    getPriorityComparator(false), getExtensionComparator(), getNameComparator(true));

        case CATEGORY:
            return new SimilarResultsGroupingDelegateComparator(
                    getPriorityComparator(false), getCategoryComparator(), getNameComparator(true));

        case LENGTH:
            return new SimilarResultsGroupingDelegateComparator(
                    getPriorityComparator(false), getLongComparator(FilePropertyKey.LENGTH, false), getNameComparator(true));

        case NAME:
        case TITLE:
            return new SimilarResultsGroupingDelegateComparator(
                    getPriorityComparator(false), getNameComparator(true));

        case PLATFORM:
            return getStringPropertyPlusNameComparator(FilePropertyKey.COMPANY, true);

        case QUALITY:
            return new SimilarResultsGroupingDelegateComparator(
                    getPriorityComparator(false), getLongComparator(FilePropertyKey.QUALITY, false), getNameComparator(true));

        case RELEVANCE_ITEM:
            return getRelevanceComparator();

        case SIZE_HIGH_TO_LOW:
            return new SimilarResultsGroupingDelegateComparator(
                    getPriorityComparator(false), getSizeComparator(false), getNameComparator(true));

        case SIZE_LOW_TO_HIGH:
            return new SimilarResultsGroupingDelegateComparator(
                    getPriorityComparator(false), getSizeComparator(true), getNameComparator(true));

        case YEAR:
            return new SimilarResultsGroupingDelegateComparator(
                    getPriorityComparator(false), getLongComparator(FilePropertyKey.YEAR, true), getNameComparator(true));
        
        default:
            throw new IllegalArgumentException("unknown item " +  sortOption);
        }
    }

    /**
     * Returns a search result Comparator for category types.
     */
    static Comparator<VisualSearchResult> getCategoryComparator() {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                return compareToNull(vsr1.getCategory(), vsr2.getCategory());
            }
        };
    }
    
    /**
     * Returns a search result Comparator for date values.  The specified key
     * must reference property values stored as Long objects.
     */
    static Comparator<VisualSearchResult> getDateComparator(
            final FilePropertyKey key, final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Long v1 = (Long) vsr1.getProperty(key);
                Long v2 = (Long) vsr2.getProperty(key);
                return compareNullCheck(v1, v2, ascending, true);
            }
        };
    }

    /**
     * Returns a search result Comparator for file extensions.
     */
    static Comparator<VisualSearchResult> getExtensionComparator() {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                return compareToNull(vsr1.getFileExtension(), vsr2.getFileExtension());
            }
        };
    }

    /**
     * Returns a search result Comparator for Long values.  The specified key
     * must reference property values that can be converted to Long objects.
     */
    static Comparator<VisualSearchResult> getLongComparator(
            final FilePropertyKey key, final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                Long l1 = (Long)vsr1.getProperty(key);
                Long l2 = (Long)vsr2.getProperty(key);
                return compareNullCheck(l1, l2, ascending, true);
            }
        };
    }

    /**
     * Returns a search result Comparator that compares the heading field.
     */
    static Comparator<VisualSearchResult> getNameComparator(
            final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = vsr1.getHeading();
                String v2 = vsr2.getHeading();
                return ascending ? compareToNullIgnoreCase(v1, v2, false)
                        : compareToNullIgnoreCase(v2, v1, false);
            }
        };
    }

    /**
     * Returns a search result Comparator that compares the sort priority
     * field for store results.
     */
    static Comparator<VisualSearchResult> getPriorityComparator(
            final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                // Define sort order.
                int result = ascending ? 1 : -1;
                
                if (vsr1 instanceof VisualStoreResult) {
                    SortPriority sp1 = ((VisualStoreResult) vsr1).getSortPriority();
                    if (vsr2 instanceof VisualStoreResult) {
                        SortPriority sp2 = ((VisualStoreResult) vsr2).getSortPriority();
                        // Compare two store results.
                        if (sp1 == sp2) {
                            return 0;
                        } else if (sp1 == SortPriority.TOP) {
                            return result;
                        } else if (sp2 == SortPriority.TOP) {
                            return -result;
                        } else {
                            return (sp1 == SortPriority.MIXED) ? result : -result;
                        }
                        
                    } else {
                        // Compare store vs. non-store results.
                        return (sp1 == SortPriority.TOP) ? result : 
                            ((sp1 == SortPriority.BOTTOM) ? -result : 0);
                    }
                    
                } else if (vsr2 instanceof VisualStoreResult) {
                    SortPriority sp2 = ((VisualStoreResult) vsr2).getSortPriority();
                    // Compare non-store vs. store results.
                    return (sp2 == SortPriority.TOP) ? -result : 
                        ((sp2 == SortPriority.BOTTOM) ? result : 0);
                    
                } else {
                    // Two non-store results always equal.
                    return 0;
                }
            }
        };
    }
    
    /**
     * Returns a search result Comparator that compares the relevance and name
     * values.
     */
    @SuppressWarnings("unchecked")
    static Comparator<VisualSearchResult> getRelevanceComparator() {
        return new SimilarResultsGroupingDelegateComparator(
                getPriorityComparator(false), 
                getRelevanceComparator(false), getNameComparator(true));
    }

    /**
     * Returns a search result Comparator for relevance values with the 
     * specified sort order.
     */
    static Comparator<VisualSearchResult> getRelevanceComparator(
            final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                float r1 = vsr1.getRelevance();
                float r2 = vsr2.getRelevance();
                return ascending ? compareToNull(r1, r2, false) 
                        : compareToNull(r2, r1, false);
            }
        };
    }

    /**
     * Returns a search result Comparator for size values.
     */
    static Comparator<VisualSearchResult> getSizeComparator(
            final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                return ascending ? compareToNull(vsr1.getSize(), vsr2.getSize(), false)
                        : compareToNull(vsr2.getSize(), vsr1.getSize(), false);
            }
        };
    }

    /**
     * Returns a search result Comparator for string values.  The specified key
     * must reference property values stored as String objects.
     */
    static Comparator<VisualSearchResult> getStringComparator(
            final FilePropertyKey key, final boolean ascending) {
        return new Comparator<VisualSearchResult>() {
            @Override
            public int compare(VisualSearchResult vsr1, VisualSearchResult vsr2) {
                String v1 = (String) vsr1.getProperty(key);
                String v2 = (String) vsr2.getProperty(key);
                return ascending ? compareToNullIgnoreCase(v1, v2, false)
                        : compareToNullIgnoreCase(v2, v1, false);
            }
        };
    }

    /**
     * Returns a search result Comparator that compares the specified string
     * property and name values.
     */
    @SuppressWarnings("unchecked")
    static Comparator<VisualSearchResult> getStringPropertyPlusNameComparator(
            final FilePropertyKey filePropertyKey, final boolean ascending) {
        return new SimilarResultsGroupingDelegateComparator(
                getPriorityComparator(false),
                getStringComparator(filePropertyKey, ascending), getNameComparator(ascending));
    }
    
    /**
     * Compare the two specified Comparable objects, and returns a negative,
     * zero, or positive value if the first object is less than, equal to, or
     * greater than the second.  If <code>ascending</code> is false, then the
     * sign of the return value is reversed.  If <code>nullsFirst</code> is
     * false, then null values are treated as larger than non-null values.
     */
    private static int compareNullCheck(Comparable c1, Comparable c2, 
            boolean ascending, boolean nullsFirst) {
        return ascending ? compareToNull(c1, c2, nullsFirst) 
                : compareToNull(c2, c1, nullsFirst);
    }
}
