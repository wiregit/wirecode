package org.limewire.core.api.mojito;

import java.beans.PropertyChangeListener;
import java.io.PrintWriter;

/**
 * Defines the manager interface for the Mojito DHT.
 */
public interface MojitoManager {
    
    /**
     * Adds the specified listener to the list that is notified when a 
     * property value changes. 
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes the specified listener from the list that is notified when a 
     * property value changes. 
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);
    
    /**
     * Returns true if the Mojito DHT is running.
     */
    public boolean isRunning();
    
    /**
     * Invokes the specified command on the Mojito DHT, and forwards output
     * to the specified PrintWriter. 
     */
    public boolean handle(String command, PrintWriter out);
    
    /**
     * Returns the name of the DHT.
     */
    public String getName();
    
    /**
     * Returns {@code true} if the DHT is ready.
     */
    public boolean isReady();
    
    /**
     * Returns {@code true} if the DHT is booting.
     */
    public boolean isBooting();
}
