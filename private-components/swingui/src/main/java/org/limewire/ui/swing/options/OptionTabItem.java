package org.limewire.ui.swing.options;


public interface OptionTabItem {

    /** Selects the option item. */
    void select();
    
    /** Returns true if this OptionTabItem is currently selected. */
    boolean isSelected();
    
    /** Returns the id of nav item. */
    String getId();
    
    /** Adds a NavItemListener. */
    void addTabItemListener(TabItemListener listener);
    
    /** Removes a NavItemListener. */
    void removeTabItemListener(TabItemListener listener);
}
