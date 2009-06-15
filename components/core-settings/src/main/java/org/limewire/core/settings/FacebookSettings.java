package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringSetting;

public class FacebookSettings extends LimeProps {

    /**
     * Cached chat channel.
     */
    public static final StringSetting CHAT_CHANNEL =
        FACTORY.createStringSetting("CHAT_CHANNEL", "");
    
    /**
     * Whether Facebook is enabled.
     */
    public static final BooleanSetting FACEBOOK_ENABLED =
        FACTORY.createRemoteBooleanSetting("FACEBOOK_ENABLED", true,
                "FacebookSettings.facebookEnabled");
}
