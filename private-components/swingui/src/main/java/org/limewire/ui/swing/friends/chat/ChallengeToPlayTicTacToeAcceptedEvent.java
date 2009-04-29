package org.limewire.ui.swing.friends.chat;

import org.limewire.ui.swing.event.AbstractEDTEvent;

public class ChallengeToPlayTicTacToeAcceptedEvent extends AbstractEDTEvent {

    private ChatFriend chatFriend;
    public ChallengeToPlayTicTacToeAcceptedEvent(ChatFriend chatFriend) {
        this.chatFriend = chatFriend;
    }   
    public ChatFriend getFriend() {
        return chatFriend;
    }
}
