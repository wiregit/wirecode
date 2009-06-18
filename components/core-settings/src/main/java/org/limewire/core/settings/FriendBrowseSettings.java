package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;

/** Settings related to browsing friends*/
public class FriendBrowseSettings extends LimeProps {
    
    private FriendBrowseSettings(){}

    /**
     * Whether or not the user has ever successfully browsed all friends in the past
     */
    public static final BooleanSetting HAS_BROWSED_ALL_FRIENDS = FACTORY.createBooleanSetting("HAS_BROWSED_ALL_FRIENDS", false);
    
}
