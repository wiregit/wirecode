package org.limewire.core.impl;

import org.limewire.core.api.URN;

/**
 * Implementation of URN for the mock core.
 */
public class MockURN implements URN {

    private final String urn;
    
    /**
     * Constructs a MockURN with the specified urn string.
     */
    public MockURN(String urn) {
        this.urn = urn;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MockURN) {
            MockURN mockURN = (MockURN) obj;
            return urn.equals(mockURN.urn);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }

    @Override
    public String toString() {
        return urn;
    }
    
    @Override
    public int compareTo(URN o) {
        if (o instanceof MockURN) {
            MockURN urnObj = (MockURN) o;
            return urn.compareTo(urnObj.urn);
        }
        return -1;
    }
}
