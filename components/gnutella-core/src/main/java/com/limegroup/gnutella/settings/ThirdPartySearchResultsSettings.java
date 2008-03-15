package com.limegroup.gnutella.settings;

import org.limewire.setting.StringSetting;

/**
 * Settings for The LimeWire Store&#8482; song search results. This is used by
 * {@link RemoteStringBasicThirdPartyResultsDatabaseImpl} to store the search
 * index.
 */
public final class ThirdPartySearchResultsSettings extends LimeProps {
    private ThirdPartySearchResultsSettings() {}

    /**
     * The total time this user has been connected to the network (in seconds).
     */    
    public static final StringSetting SEARCH_DATABASE =
        FACTORY.createRemoteStringSetting("SEARCH_DATABASE", "", "ThirdPartySearchResultsSettings.searchDatabase");
 
}
