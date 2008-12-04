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
    
    public static final StringSetSetting XMPP_SERVERS =
        FACTORY.createStringSetSetting("XMPP_SERVERS",
                "false,true,talk.google.com,5222,gmail.com,Gmail,http://mail.google.com/mail/signup;" +
                "false,false,jabber.hot-chilli.net,5222,jabber.hot-chilli.net,Hot-Chilli,http://jabber.hot-chilli.net/jwchat/;" +
                "false,false,jabberes.org,5222,jabberes.org,JabberES,http://www.jabberes.org/jrt/;" +
                "false,false,jabber.ru,5222,jabber.ru,Jabber.ru,http://www.jabber.ru/xreg/;" +
                "false,false,jabbim.com,5222,jabbim.com,Jabbim,https://secure.jabbim.com/limewire/;" +
                "false,false,xmpp.services.livejournal.com,5222,livejournal.com,LiveJournal,http://www.livejournal.com/create.bml;" +
                "false,false,macjabber.de,5222,macjabber.de,MacJabber.de,https://macjabber.de:444/;" +
        "");

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
}
