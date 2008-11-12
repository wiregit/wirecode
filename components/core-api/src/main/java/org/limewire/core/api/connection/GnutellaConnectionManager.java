package org.limewire.core.api.connection;

import java.beans.PropertyChangeListener;

/**
 * A hook into the gnutella connection management.
 */
public interface GnutellaConnectionManager {

    public void addPropertyChangeListener(PropertyChangeListener listener);
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /** Returns true if the node is currently an ultrapeer. */
    public boolean isUltrapeer();

    /** Disconnects & reconnects to Gnutella. */
    public void restart();   
    
    /** Returns the current strength of the Gnutella connections. */
    public ConnectionStrength getConnectionStrength();

}
