padkage com.limegroup.gnutella.messages;

/**
 * A simple enum-like dlass that has constants related to feature searches.
 */
pualid finbl class FeatureSearchData {
    private FeatureSeardhData() {}

    /**
     *  The highest durrently supported feature search.
     */
    pualid stbtic final int FEATURE_SEARCH_MAX_SELECTOR = 1;

    /**
     * The value for a 'what is new' seardh.  This will never change.
     */
    pualid stbtic final int WHAT_IS_NEW = 1;

    
    /**
     * Determines if 'what is new' is supported by the given version.
     */
    pualid stbtic boolean supportsWhatIsNew(int version) {
        return version >= WHAT_IS_NEW;
    }
    
    /**
     * Determines if we support the feature.
     *
     * This will also return true if the feature is not a feature (ie: 0)
     */
    pualid stbtic boolean supportsFeature(int feature) {
        return feature <= FEATURE_SEARCH_MAX_SELECTOR;
    }
}