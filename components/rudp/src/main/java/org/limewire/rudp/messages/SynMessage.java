package org.limewire.rudp.messages;

public interface SynMessage extends RUDPMessage {

    public byte getSenderConnectionID();

    public int getProtocolVersionNumber();

}