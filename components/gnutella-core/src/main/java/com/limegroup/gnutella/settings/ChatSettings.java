
package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.CommonUtils;

/**
 * Settings for chat
 */
public class ChatSettings extends LimeProps {
    
    private ChatSettings() {}
    
    /**
	 * Sets whether or not chat should be enabled.
	 */
    public static final BooleanSetting CHAT_ENABLED =
        FACTORY.createBooleanSetting("CHAT_ENABLED", true);
}