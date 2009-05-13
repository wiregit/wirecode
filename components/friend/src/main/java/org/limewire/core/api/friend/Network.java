package org.limewire.core.api.friend;

public interface Network {
    public enum Type {XMPP, FACEBOOK}
    public String getCanonicalizedLocalID();    
    public String getNetworkName();
    public Type getType();
}
