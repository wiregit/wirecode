package org.limewire.ui.swing.search.model;

import org.limewire.io.URN;

/**
 * Mock URN for unit tests.
 */
public class MockURN implements URN {
    private final String urn;

    public MockURN(String urn) {
        this.urn = urn;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MockURN) {
            MockURN urnObj = (MockURN) obj;
            return urn.equals(urnObj.urn);
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
            MockURN urnObj = (MockURN) o;
            return urn.compareTo(urnObj.urn);
        }
        return -1;
    }

    @Override
    public String toString() {
        return urn;
    }
}
