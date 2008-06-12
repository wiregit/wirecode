package org.limewire.xmpp.client;

public class Presence {
    
    enum Type {
        available, unavailable, subscribe, subscribed, unsubscribe, unsubscribed, error
    }
    
    enum Mode {
        chat, available, away, xa, dnd
    }
    
    protected Type type;
    protected java.lang.String status;
    protected int priority;
    protected Mode mode;
    protected java.lang.String language;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
