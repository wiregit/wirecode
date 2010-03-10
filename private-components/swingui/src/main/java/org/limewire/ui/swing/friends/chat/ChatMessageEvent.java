package org.limewire.ui.swing.friends.chat;

import org.limewire.listener.DefaultDataEvent;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.DataCategory;

/**
 * event for new chat message.
 */
public class ChatMessageEvent extends DefaultDataEvent<Message> {
    
    @SuppressWarnings("unused")
    @InspectablePrimitive(value = "has chatted", category = DataCategory.USAGE)
    private static volatile boolean hasChatted;
    
    public ChatMessageEvent(Message message) {
        super(message);
        switch (message.getType()) {
            case SENT:
            case RECEIVED:
                hasChatted = true;
        }
    }
}
