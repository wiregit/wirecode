package org.limewire.ui.swing.nav;

import java.awt.Component;

import org.limewire.ui.swing.nav.Navigator.NavCategory;

interface NavList {

    NavCategory getCategory();

    void addNavItem(NavItem navItem);

    NavItem getNavItem(String name);

    boolean hasNavItem(String name);

    void selectItem(NavItem navItem);

    void selectItemByName(String name);

    void removeNavItem(NavItem navItem);

    boolean isNavItemSelected();

    void clearSelection();

    NavItem getSelectedNavItem();

    Component getComponent();

    void addSelectionListener(SelectionListener listener);
    
    interface SelectionListener {
        void selectionChanged(NavList source);
    }
}