package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringArraySetting;
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
        FACTORY.createBooleanSetting("FACEBOOK_ENABLED", false);
    
    /**
     * Facebook auth server urls.
     */
    public static final StringArraySetting AUTH_SERVER_URLS =
        FACTORY.createRemoteStringArraySetting("FACEBOOK_AUTH_SERVER_URLS", new String[] {
                "http://coelacanth:5555/getlogin/",
                "http://cruncher:5555/getlogin/",
        }, "Facebook.authServerUrls");

}
