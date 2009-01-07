package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;


/**
 * Settings to deal with UI.
 */ 
public final class SwingUiSettings extends LimeProps {

    private SwingUiSettings() {}
    
    /** If the 'offline contacts' in the nav are collapsed. */
    public static final BooleanSetting OFFLINE_COLLAPSED = 
        FACTORY.createBooleanSetting("OFFLINE_CONTACTS_COLLAPSED", true);
    
    /** If the 'online contacts' in the nav are collapsed. */
    public static final BooleanSetting ONLINE_COLLAPSED =
        FACTORY.createBooleanSetting("ONLINE_CONTACTS_COLLAPSED", false);

}
