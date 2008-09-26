package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;

public class RemoveFriendEvent extends AbstractEDTEvent {
    private final ChatFriend chatFriend;

    public RemoveFriendEvent(ChatFriend chatFriend) {
        super();
        this.chatFriend = chatFriend;
    }

    public ChatFriend getFriend() {
        return chatFriend;
    }
}
