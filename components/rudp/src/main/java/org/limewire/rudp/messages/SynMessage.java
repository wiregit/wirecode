package org.limewire.rudp.messages;

public interface SynMessage {

    public abstract byte getSenderConnectionID();

    public abstract int getProtocolVersionNumber();

}