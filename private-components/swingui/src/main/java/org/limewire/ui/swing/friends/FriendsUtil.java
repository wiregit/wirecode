package org.limewire.ui.swing.friends;

import javax.swing.Icon;

class FriendsUtil {

    public static Icon getIcon(Friend friend, IconLibrary icons) {
        switch(friend.getMode()) {
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
