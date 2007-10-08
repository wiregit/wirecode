package org.limewire.rudp.messages;

/**
 * Defines an interface to begin a reliable UDP connection. 
 */
public interface SynMessage extends RUDPMessage {

    public byte getSenderConnectionID();

    public int getProtocolVersionNumber();

}