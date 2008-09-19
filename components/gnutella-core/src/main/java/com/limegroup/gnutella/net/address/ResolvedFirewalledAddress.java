package com.limegroup.gnutella.net.address;

import java.util.Set;

import org.limewire.io.Connectable;

import com.limegroup.gnutella.GUID;

/**
 * Subclass of Firewalled address to mark addresses as resolved. Used by
 * {@link SameNATAddressResolver}.
 */
public class ResolvedFirewalledAddress extends FirewalledAddress {

    public ResolvedFirewalledAddress(Connectable publicAddress, Connectable privateAddress,
            GUID clientGuid, Set<Connectable> pushProxies, int fwtVersion) {
        super(publicAddress, privateAddress, clientGuid, pushProxies, fwtVersion);
        
    }

}
