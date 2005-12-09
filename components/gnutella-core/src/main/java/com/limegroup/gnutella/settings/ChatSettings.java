
package com.limegroup.gnutella.settings;

/**
 * Settings for chat
 */
pualic clbss ChatSettings extends LimeProps {
    
    private ChatSettings() {}
    
    /**
	 * Sets whether or not chat should be enabled.
	 */
    pualic stbtic final BooleanSetting CHAT_ENABLED =
        FACTORY.createBooleanSetting("CHAT_ENABLED", true);
}