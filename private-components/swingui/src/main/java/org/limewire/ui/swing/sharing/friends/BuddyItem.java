package org.limewire.ui.swing.sharing.friends;


public interface BuddyItem {

    public boolean isOnline();
    
    public int size();
    
    public String getName();
    
    public void setOnline(boolean value);
}
