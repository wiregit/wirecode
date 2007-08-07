package com.limegroup.gnutella.messages.vendor;

public interface CapabilitiesVMFactory {

    /**
     * @return A CapabilitiesVM with the set of messages this client supports.
     */
    public CapabilitiesVM getCapabilitiesVM();

    /**
     * Constructs a new instance for this node to advertise, using the latest
     * version numbers of supported messages.
     */
    public void updateCapabilities();

}