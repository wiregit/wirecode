package org.limewire.ui.swing.nav;

import org.limewire.ui.swing.nav.Navigator.NavCategory;



public interface NavigableTree {
   
    void removeNavigableItem(NavCategory category, NavItem navItem);
    
    void addNavigableItem(NavCategory category, NavItem navItem, boolean userRemovable);
    
    void addNavSelectionListener(NavSelectionListener listener);

    void selectNavigableItem(NavCategory category, NavItem navItem);

    NavItem getNavigableItemByName(NavCategory category, String name);

    boolean hasNavigableItem(NavCategory category, String name);
}
