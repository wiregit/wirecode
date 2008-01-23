package org.limewire.net;

/** Settings & notifications related to binding outgoing sockets to a custom address. */
public interface SocketBindingSettings {

    /** Returns true if outgoing sockets should be bound. */
    boolean isSocketBindingRequired();

    /** Returns the address outgoing sockets should be bound to. */
    String getAddressToBindTo();
    
    /** Returns the port outgoing sockets should be bound to. */
    int getPortToBindTo();

    /** Notification that a binding failed. */
    void bindingFailed();

}
