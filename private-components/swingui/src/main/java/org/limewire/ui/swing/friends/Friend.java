package org.limewire.ui.swing.friends;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface Friend {
    enum State { Available, Away, Busy, Offline };
    
    String getName();
    
    String getStatus();
    
    State getState();
}
