package org.limewire.ui.swing.friends;

import javax.swing.Icon;

import org.limewire.xmpp.api.client.Presence;

public class FriendsUtil {

    public static Icon getIcon(Friend friend, IconLibrary icons) {
        Presence.Mode mode = friend.getMode(); 
        return getIcon(mode, icons);
    }

    public static Icon getIcon(Presence.Mode mode, IconLibrary icons) {
        switch(mode) {
        case available:
            return icons.getAvailable();
        case chat:
            return icons.getChatting();
        case dnd:
            return icons.getDoNotDisturb();
        }
        return icons.getAway();
    }
}
