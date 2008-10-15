package org.limewire.ui.swing.nav;

import javax.swing.JComponent;


/** A listener for navigation.  This is intended to be used to listen to all changes on Navigation. */
public interface NavigationListener {
    
    /** Notification that the selection has changed. */
    public void itemSelected(NavCategory category, NavItem navItem, NavSelectable selectable, JComponent panel);

    /** Notification that an item was removed. */
    public void itemRemoved(NavCategory category, NavItem navItem, JComponent panel);
    
    /** Notification that an item was added. */
    public void itemAdded(NavCategory category, NavItem navItem, JComponent panel);
    
}
