package org.limewire.net.address;

/**
 * Represents the network address of a limewire instance.  An <code>Address</code>
 * is a collection of information that enables one limewire instance to make a 
 * network connection to another.  It is an address in the abstract sense, and doesn't
 * necessarily mean and ip and port, although it could include that information.
 * It could also include, for example, a client guid and push proxies.
 * It could also be a jabber id, if jabber messages are used as a signaling
 * channel, for example, to execute reverse connections or firewall transfers.
 */
public interface Address {
    enum EventType {
        ADDRESS_CHANGED
    }
}
