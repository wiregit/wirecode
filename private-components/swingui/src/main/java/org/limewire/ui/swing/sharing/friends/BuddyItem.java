package org.limewire.ui.swing.sharing.friends;

import java.beans.PropertyChangeListener;

public interface BuddyItem {
    
    public int size();
    
    public String getName();
    
    public void addPropertyChangeListener(PropertyChangeListener l);
    
    public void removePropertyChangeListener(PropertyChangeListener l);
}
