package com.limegroup.gnutella.settings;

import org.limewire.promotion.PromotionBinder;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for The LimeWire Store&#8482; song search results. This is used by
 * {@link RemoteStringBasicSpecialResultsDatabaseImpl} to store the search
 * index.
 */
public final class ThirdPartySearchResultsSettings extends LimeProps {
    private ThirdPartySearchResultsSettings() {}

    /**
     * The total time this user has been connected to the network (in seconds).
     */    
    public static final StringSetting SEARCH_DATABASE =
        FACTORY.createRemoteStringSetting("SEARCH_DATABASE", "", "ThirdPartySearchResultsSettings.searchDatabase");
 
    /**
     * Enables the promotion system when <code>true</code>.
     */    
    public static final BooleanSetting PROMOTION_SYSTEM_IS_ENABLED =
        FACTORY.createRemoteBooleanSetting("PROMOTION_SYSTEM_IS_ENABLED", true, "ThirdPartySearchResultsSettings.promotionSystemIsEnabled");    
    
    /**
     * The url we use to search for {@link PromotionBinder}s.
     */    
    public static final StringSetting SEARCH_URL =
        FACTORY.createRemoteStringSetting("SEARCH_URL", "http://jeffpalm.com/lwp/getBuckets.php", "ThirdPartySearchResultsSettings.searchUrl");
    
    /**
     * The url we use to redirect for {@link PromotionBinder}s.
     */    
    public static final StringSetting REDIRECT_URL =
        FACTORY.createRemoteStringSetting("REDIRECT_URL", "http://jeffpalm.com/lwp/redirect.php", "ThirdPartySearchResultsSettings.redirectUrl");    
    
    /**
     * The mod we take with the bucket ID. 
     */    
    public static final IntSetting BUCKET_ID_MODULOUS =
        FACTORY.createRemoteIntSetting("BUCKET_ID_MODULOUS", 200, "ThirdPartySearchResultsSettings.bucketIdModulous", 0, Integer.MAX_VALUE); 
    
    /**
     * The max number of search results.
     */    
    public static final IntSetting MAX_NUMBER_OF_SEARCH_RESULTS =
        FACTORY.createRemoteIntSetting("MAX_NUMBER_OF_SEARCH_RESULTS", 5, "ThirdPartySearchResultsSettings.maxNumberOfSearchResults", 0, Integer.MAX_VALUE);
    
    /**
     * The timeout to use when contacting the network for binders.  If this is <code>-1</code> then we ignore it.
     */
    public static final IntSetting NETWORK_TIMEOUT_MILLIS_FOR_REQUESTING_BUCKETS = FACTORY
            .createRemoteIntSetting("NETWORK_TIMEOUT_MILLIS_FOR_REQUESTING_BUCKETS", 5000,
                    "ThirdPartySearchResultsSettings.networkTimeoutMillisForRequestingBuckets", -1,
                    8000);
}
