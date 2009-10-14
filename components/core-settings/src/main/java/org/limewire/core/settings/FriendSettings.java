package org.limewire.core.settings;

import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.setting.BooleanSetting;

/**
 * Settings for friends.
 */
public class FriendSettings extends LimeProps {

    private FriendSettings() {}

    /**
     * This setting tracks whether or not the user should be in do not disturb mode. 
     * It should be remembered across sessions.
     */
    public static final BooleanSetting DO_NOT_DISTURB =
        (BooleanSetting)FACTORY.createBooleanSetting("XMPP_DO_NOT_DISTURB", false).setPrivate(true);
    
    /**
     * Has the user ever opened the sign in dialog?
     */
    @InspectablePrimitive(value = "ever opened sign in dialog", category = DataCategory.USAGE)
    public static final BooleanSetting EVER_OPENED_SIGN_IN_DIALOG =
        (BooleanSetting)FACTORY.createBooleanSetting("EVER_OPENED_SIGN_IN_DIALOG", false).setPrivate(true);
    
    /**
     * Has the user ever tried to sign in?
     */
    @InspectablePrimitive(value = "ever tried to sign in", category = DataCategory.USAGE)
    public static final BooleanSetting EVER_TRIED_TO_SIGN_IN =
        (BooleanSetting)FACTORY.createBooleanSetting("EVER_TRIED_TO_SIGN_IN", false).setPrivate(true);
    
    /**
     * Has the user ever successfully signed in?
     */
    @InspectablePrimitive(value = "ever signed in", category = DataCategory.USAGE)
    public static final BooleanSetting EVER_SIGNED_IN =
        (BooleanSetting)FACTORY.createBooleanSetting("EVER_SIGNED_IN", false).setPrivate(true);
}
