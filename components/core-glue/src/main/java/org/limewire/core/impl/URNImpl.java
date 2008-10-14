package org.limewire.core.impl;

import org.limewire.core.api.URN;
import org.limewire.util.Objects;

public class URNImpl implements URN {
    private final com.limegroup.gnutella.URN urn;

    public URNImpl(com.limegroup.gnutella.URN urn) {
        this.urn = Objects.nonNull(urn, "urn");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof com.limegroup.gnutella.URN) {
            return urn.equals(obj);
        } else if (obj instanceof URNImpl) {
            return urn.equals(((URNImpl) obj).getUrn());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }

    @Override
    public String toString() {
        return urn.toString();
    }

    public com.limegroup.gnutella.URN getUrn() {
        return urn;
    }

    @Override
    public int compareTo(URN o) {
        return urn.toString().compareTo(o.toString());
    }

}
