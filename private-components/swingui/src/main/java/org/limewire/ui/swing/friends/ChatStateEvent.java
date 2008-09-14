package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;
import org.limewire.xmpp.api.client.ChatState;

public class ChatStateEvent extends AbstractEDTEvent {
    private static final String TOPIC_PREFIX = "chatstate-";
    private final Friend friend;
    private final ChatState state;

    public ChatStateEvent(Friend friend, ChatState state) {
        this.friend = friend;
        this.state = state;
    }
    
    public Friend getFriend() {
        return friend;
    }
    
    public ChatState getState() {
        return state;
    }
    
    public static String buildTopic(String conversationName) {
        return TOPIC_PREFIX + conversationName;
    }
    
    @Override
    public void publish() {
        super.publish(buildTopic(friend.getID()));
    }
}
