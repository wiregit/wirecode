package org.limewire.ui.swing.friends;

import javax.swing.Icon;

class FriendsUtil {

    public static Icon getIcon(Friend friend, IconLibrary icons) {
        //Change to chatting icon because gtalk doesn't actually set mode to 'chat', so icon won't show chat bubble normally
        if (friend.isChatting()) {
            return friend.isReceivingUnviewedMessages() ? icons.getUnviewedMessages() : icons.getChatting();
        }
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
