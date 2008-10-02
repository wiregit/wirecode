package org.limewire.ui.swing.nav;

/**
 * A token representing a navigable item.
 */
public interface NavItem {

    /** Selects the nav item. */
    void select();

    /** Removes this nav item. */
    void remove();

    /** Returns the id of nav item. */
    String getId();
    
    /** Adds a NavItemListener. */
    void addNavItemListener(NavItemListener listener);
    
    /** Removes a NavItemListener. */
    void removeNavItemListener(NavItemListener listener);
    
    /** Returns true if this NavItem is currently selected. */
    boolean isSelected();


}
