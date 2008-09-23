package org.limewire.ui.swing.nav;

import java.awt.Component;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.ui.swing.nav.Navigator.NavCategory;

class SimpleNavList implements NavList {
    
    private final NavCategory category;    
    private final CopyOnWriteArrayList<SelectionListener> listeners = new CopyOnWriteArrayList<SelectionListener>();
    private final CopyOnWriteArrayList<NavItem> items = new CopyOnWriteArrayList<NavItem>();
    private int selectedIdx;
    

    public SimpleNavList(NavCategory category) {
        this.category = category;
    }

    @Override
    public void addNavItem(NavItem navItem) {
        items.add(navItem);
    }

    @Override
    public void addSelectionListener(SelectionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void clearSelection() {
        int oldIdx = selectedIdx;
        selectedIdx = -1;
        if(oldIdx != selectedIdx) {
            fireChanged();
        }
    }

    @Override
    public NavCategory getCategory() {
        return category;
    }

    @Override
    public Component getComponent() {
        throw new UnsupportedOperationException("No component, should not be calling.");
    }

    @Override
    public NavItem getNavItem(String name) {
        for(NavItem item : items) {
            if(item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public NavItem getSelectedNavItem() {
        if(selectedIdx != -1) {
            return items.get(selectedIdx);
        } else {
            return null;
        }
    }

    @Override
    public boolean hasNavItem(String name) {
        return getNavItem(name) != null;
    }

    @Override
    public boolean isNavItemSelected() {
        return selectedIdx != -1;
    }

    @Override
    public void removeNavItem(NavItem navItem) {
        int row = items.indexOf(navItem);
        if(row != -1) {
            if(row == selectedIdx) {
                selectedIdx = -1;
                items.remove(row);
                fireChanged();
            }
        }
    }

    @Override
    public void selectItem(NavItem navItem) {
        int row = items.indexOf(navItem);
        if(selectedIdx != row) {
            selectedIdx = row;
            fireChanged();
        }
    }

    @Override
    public void selectItemByName(String name) {
        NavItem item = getNavItem(name);
        if(item != null) {
            selectItem(item);
        }
    }

    
    private void fireChanged() {
        for(SelectionListener listener : listeners) {
            listener.selectionChanged(this);
        }
    }
    
    
}
