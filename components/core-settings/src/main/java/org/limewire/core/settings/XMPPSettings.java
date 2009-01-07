package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringSetSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for XMPP: a list of servers and the label and username of the
 * auto-login account, if there is one (the password can be retrieved from
 * PasswordManager).
 */
public class XMPPSettings extends LimeProps {

    private XMPPSettings() {}

    /**
     * Out-of-the-box list of well known jabber servers
     */
    public static final StringSetSetting XMPP_SERVERS =
        FACTORY.createStringSetSetting("XMPP_SERVERS",
                "false,true,gmail.com,Gmail;" +
                "false,false,livejournal.com,LiveJournal;" +
        "");

    /**
     * User-defined custom jabber server
     */
    public static final StringSetting USER_DEFINED_XMPP_SERVER =
        (StringSetting)FACTORY.createStringSetting("XMPP_SERVER", "").setPrivate(true);

    public static final StringSetting XMPP_AUTO_LOGIN =
        (StringSetting)FACTORY.createStringSetting("XMPP_AUTO_LOGIN", "").setPrivate(true);
    
    /**
     * This setting tracks whether or not the user should be in do not disturb mode. 
     * It should be remembered across xmpp sessions.
     */
    public static final BooleanSetting XMPP_DO_NOT_DISTURB =
        (BooleanSetting)FACTORY.createBooleanSetting("XMPP_DO_NOT_DISTURB", false).setPrivate(true);
    
    /**
     * This setting is used to track whether or not the user wants to show offline buddies in the left panel.
     */
    public static final BooleanSetting XMPP_SHOW_OFFLINE =
        (BooleanSetting)FACTORY.createBooleanSetting("XMPP_SHOW_OFFLINE", true).setPrivate(true);
    
    /**
     * Whether or not 'REMEMBER_ME' is checked -- this has nothing to do with
     * auto logging in. It's only the state of the REMEMBER ME checkbox, so if
     * you uncheck it it doesn't stay checked on the next login.
     */
    public static final BooleanSetting REMEMBER_ME_CHECKED =
        FACTORY.createBooleanSetting("REMEMBER_ME_XMPP", true);
}
