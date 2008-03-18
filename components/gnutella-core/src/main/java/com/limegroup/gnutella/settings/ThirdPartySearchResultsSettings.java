package com.limegroup.gnutella.settings;

import org.limewire.promotion.PromotionBinder;
import org.limewire.setting.StringSetting;

/**
 * Settings for The LimeWire Store&#8482; song search results. This is used by
 * {@link RemoteStringBasicThirdPartyResultsDatabaseImpl} to store the search
 * index.
 */
public final class ThirdPartySearchResultsSettings extends LimeProps {
    private ThirdPartySearchResultsSettings() {}

    /**
     * The search database.
     */    
    public static final StringSetting SEARCH_DATABASE =
        FACTORY.createRemoteStringSetting("SEARCH_DATABASE", "", "ThirdPartySearchResultsSettings.searchDatabase");
    
    /**
     * The url we use to search for {@link PromotionBinder}s.
     */    
    public static final StringSetting SEARCH_URL =
        FACTORY.createRemoteStringSetting("SEARCH_URL", "http://jeffpalm.com/lwp/getBuckets.php", "ThirdPartySearchResultsSettings.searchUrl");    
 
}
