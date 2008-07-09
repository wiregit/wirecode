package org.limewire.ui.swing.nav;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.home.HomePanel;
import org.limewire.ui.swing.nav.Navigator.NavItem;

public class NavTree extends JXPanel implements NavigableTree {
        
    private final List<NavSelectionListener> navSelectionListeners = new ArrayList<NavSelectionListener>();
    private final List<NavList> navigableLists;

    public NavTree() {
        this.navigableLists = new ArrayList<NavList>();
        setLayout(new GridBagLayout());
        
        addNavList(new NavList("LimeWire", Navigator.NavItem.LIMEWIRE));
        addNavList(new NavList("Search", Navigator.NavItem.SEARCH));
        addNavList(new NavList("Library", Navigator.NavItem.LIBRARY));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        gbc.weighty = 1;
        add(Box.createGlue(), gbc);
    }
    
    private void addNavList(NavList navList) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        
        add(navList, gbc);
        navigableLists.add(navList);
        navList.addListSelectionListener(new Listener());
    }
    
    @Override
    public void addNavigableItem(Navigator.NavItem navItem, String name, boolean userRemovable) {
        for(NavList list : navigableLists) {
            if(list.getTarget() == navItem) {
                list.addNavItem(name, userRemovable);
            }
        }
    }
    
    @Override
    public void selectNavigableItem(NavItem navItem, String name) {
        for(NavList list : navigableLists) {
            if(list.getTarget() == navItem) {
                list.selectItem(name);
            }
        }
    }
    
    @Override
    public void removeNavigableItem(Navigator.NavItem navItem, String name) {
        for(NavList list : navigableLists) {
            if(list.getTarget() == navItem) {
                list.removeNavItem(name);
            }
        }
    }
    
    @Override
    public void addNavSelectionListener(NavSelectionListener listener) {
        navSelectionListeners.add(listener);
    }
    
    public void goHome() {
        selectNavigableItem(NavItem.LIMEWIRE, HomePanel.NAME);
    }
    
    private class Listener implements ListSelectionListener {
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if(e.getValueIsAdjusting()) // Ignore.
                return; 
            
            NavList list = (NavList)e.getSource();
            if(list.getSelectedIndex() != -1) { // Something is selected!
                for(NavList navList : navigableLists) {
                    if(navList != list) {
                        navList.clearSelection();
                    } else {
                        for(NavSelectionListener listener : navSelectionListeners) {
                            listener.navItemSelected(navList.getTarget(), list.getSelectionKey());
                        }
                    }
                }
            }
        }
    }
    
}
