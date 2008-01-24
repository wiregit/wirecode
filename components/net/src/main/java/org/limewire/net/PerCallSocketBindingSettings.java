package org.limewire.net;

/**
 * Used when a <code>Socket</code> user wants to specify the local port to bind to
 */
public interface PerCallSocketBindingSettings extends SocketBindingSettings {
    /**
     * 
     * @return the local port outgoing sockets should be bound to.
     */
    int getPortToBindTo();
}
