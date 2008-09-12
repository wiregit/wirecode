package org.limewire.ui.swing.nav;

import org.limewire.ui.swing.nav.Navigator.NavCategory;

public class MockNavigableTree implements NavigableTree {

    @Override
    public void addNavSelectionListener(NavSelectionListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addNavigableItem(NavCategory category, NavItem navItem, boolean userRemovable) {
        // TODO Auto-generated method stub

    }

    @Override
    public NavItem getNavigableItemByName(NavCategory category, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeNavigableItem(NavCategory category, NavItem navItem) {
        // TODO Auto-generated method stub

    }

    @Override
    public void selectNavigableItem(NavCategory category, NavItem navItem) {
        // TODO Auto-generated method stub

    }

    public boolean hasNavigableItem(NavCategory category, String name) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
