package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;

public class CloseChatEvent extends AbstractEDTEvent {
    private final ChatFriend chatFriend;
    
    public CloseChatEvent(ChatFriend chatFriend) {
        this.chatFriend = chatFriend;
    }
    
    public ChatFriend getFriend() {
        return chatFriend;
    }
}
