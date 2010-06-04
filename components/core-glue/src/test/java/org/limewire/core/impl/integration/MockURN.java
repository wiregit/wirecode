package org.limewire.core.impl.integration;

import org.limewire.common.URN;

/**
 * Test implementation of URN.
 */
class MockURN implements URN {
    private final String urn;

    public MockURN(String urn) {
        this.urn = urn;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MockURN) {
            MockURN testUrn = (MockURN) obj;
            return urn.equals(testUrn.urn);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }
    
    @Override
    public int compareTo(URN o) {
        if (o instanceof MockURN) {
            MockURN testUrn = (MockURN) o;
            return urn.compareTo(testUrn.urn);
        }
        return -1;
    }
    
    @Override
    public String toString() {
        return urn;
    }
}
