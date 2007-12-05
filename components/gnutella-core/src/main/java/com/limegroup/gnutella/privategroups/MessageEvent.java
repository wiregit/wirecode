package com.limegroup.gnutella.privategroups;

import org.limewire.listener.Event;

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
