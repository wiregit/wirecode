package org.limewire.facebook.service;

import org.limewire.core.settings.LimeProps;
import org.limewire.setting.StringSetting;

public class FacebookSettings extends LimeProps {

    /**
     * Cached chat channel.
     */
    public static final StringSetting CHAT_CHANNEL = FACTORY.createStringSetting("CHAT_CHANNEL", ""); 
}
