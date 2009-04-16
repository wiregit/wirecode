package org.limewire.ui.swing.friends.chat;

import org.limewire.core.api.friend.FriendPresence;

interface MessageTicTacToeOffer extends Message {

    /**
     * @return the {@link FriendPresence} which sent the fiole offer
     */
    FriendPresence getPresence();
    
    public String toString();

}
