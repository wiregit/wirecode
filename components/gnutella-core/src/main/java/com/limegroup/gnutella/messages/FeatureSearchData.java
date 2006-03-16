
// Commented for the Learning branch

package com.limegroup.gnutella.messages;

/**
 * Enumeration types for the GGEP "WH" extension, which deals with feature search and What's New search.
 * 
 * A query packet's GGEP block may have the extension "WH" What is new.
 * This GGEP extension does 2 things:
 * It tells what features the searching computer supports.
 * It makes the query packet a special kind of search.
 * 
 * So far, there is just 1 feature, the What's New search, represented by the number 1.
 * A query that is a What's New search will have the GGEP "WH" extension, with a byte value of 1.
 */
public final class FeatureSearchData {

    /** Don't let anyone make a FeatureSearchData object, use the static members instead. */
    private FeatureSearchData() {}

    /** 1, the highest currently supported feature search. */
    public static final int FEATURE_SEARCH_MAX_SELECTOR = 1;

    /** 1, the value for a What's New search. */
    public static final int WHAT_IS_NEW = 1;

    /**
     * Determine if a given feature search version supports the What's New search.
     * 
     * @param version The version number
     * @return        True if version is 1 or more
     */
    public static boolean supportsWhatIsNew(int version) {

        // Return true if version is 1, which includes the What's New feature
        return version >= WHAT_IS_NEW;
    }

    /**
     * Determines if we support the feature.
     * Also returns true if feature is 0, indicating no feature requirements.
     * 
     * @param feature The number code of a feature, like 1 for What's New
     * @return        True if feature is 1 or less
     */
    public static boolean supportsFeature(int feature) {

        // Compare feature to 1, the highest feature search we support
        return feature <= FEATURE_SEARCH_MAX_SELECTOR;
    }
}
