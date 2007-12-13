package com.limegroup.gnutella.privategroups;

import org.limewire.listener.Event;

/**
 * A message event is used to notify that there is a new message received by the ChatManager ReaderThread
 */
public class MessageEvent<Message, E extends Enum> implements Event<Message, E>{

    private final Message msg;
    private final E enumerator;
    
    public MessageEvent(Message msg, E enumerator) {
        this.msg = msg;
        this.enumerator = enumerator;
    }

    public Message getSource() {
        return msg;
    }

    public E getType() {
        return enumerator;
    }


}
