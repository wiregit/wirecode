package org.limewire.net;

/** A no-op implementation of SocketBindingSettings. */
public class EmptySocketBindingSettings implements SocketBindingSettings {

    public void bindingFailed() {
    }

    public String getAddressToBindTo() {
        return null;
    }

    public int getPortToBindTo() {
        return 0;
    }

    public boolean isSocketBindingRequired() {
        return false;
    }

}
