package org.limewire.ui.swing.nav;



public interface NavigableTree {
   
    void removeNavigableItem(Navigator.NavItem navItem, String name);
    
    void addNavigableItem(Navigator.NavItem navItem, String name);
    
    void addNavSelectionListener(NavSelectionListener listener);

    void selectNavigableItem(Navigator.NavItem target, String name);

}
