package org.limewire.ui.swing.nav;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.limewire.ui.swing.home.HomePanel;
import org.limewire.ui.swing.nav.Navigator.NavItem;

public class NavTree extends JPanel implements NavigableTree {
    
    private final NavList limewire;
    private final NavList library;
    
    private final List<NavSelectionListener> navSelectionListeners = new ArrayList<NavSelectionListener>();
    private final List<NavList> navigableLists;

    public NavTree() {
        this.navigableLists = new ArrayList<NavList>();
        this.limewire = new NavList("LimeWire", Navigator.NavItem.LIMEWIRE);
        this.library = new NavList("Library", Navigator.NavItem.LIBRARY);
        
        navigableLists.add(limewire);
        navigableLists.add(library);
        
        setOpaque(false);
        
        Listener listener = new Listener();
        limewire.addListSelectionListener(listener);
        library.addListSelectionListener(listener);
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        add(limewire, gbc);
        add(library, gbc);
        
        gbc.weighty = 1;
        add(Box.createGlue(), gbc);
    }
    
    @Override
    public void addNavigableItem(Navigator.NavItem navItem, String name) {
        switch(navItem) {
        case LIBRARY:
            library.addNavItem(name);
            break;
        case LIMEWIRE:
            limewire.addNavItem(name);
            break;
        }
    }
    
    @Override
    public void selectNavigableItem(NavItem navItem, String name) {
        switch(navItem) {
        case LIBRARY:
            library.selectItem(name);
            break;
        case LIMEWIRE:
            limewire.selectItem(name);
            break;
        }
    }
    
    @Override
    public void removeNavigableItem(Navigator.NavItem navItem, String name) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void addNavSelectionListener(NavSelectionListener listener) {
        navSelectionListeners.add(listener);
    }
    
    public void goHome() {
        limewire.selectItem(HomePanel.NAME);
    }
    
    private class Listener implements ListSelectionListener {
        
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if(e.getValueIsAdjusting()) // Ignore.
                return; 
            
            JList list = (JList)e.getSource();
            if(list.getSelectedIndex() != -1) { // Something is selected!
                for(NavList navList : navigableLists) {
                    if(!navList.isListSourceFrom(list)) {
                        navList.clearSelection();
                    } else {
                        for(NavSelectionListener listener : navSelectionListeners) {
                            listener.navItemSelected(navList.getTarget(), list.getSelectedValue().toString());
                        }
                    }
                }
            }
        }
    }
    
}
