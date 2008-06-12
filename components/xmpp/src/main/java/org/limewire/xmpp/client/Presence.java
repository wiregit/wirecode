package org.limewire.xmpp.client;

public interface Presence {
    
    enum Type {
        available, unavailable, subscribe, subscribed, unsubscribe, unsubscribed, error
    }
    
    enum Mode {
        chat, available, away, xa, dnd
    }
    
    public String getJID();
    
    public Type getType();

    public String getStatus();

    public int getPriority();

    public Mode getMode();
}
