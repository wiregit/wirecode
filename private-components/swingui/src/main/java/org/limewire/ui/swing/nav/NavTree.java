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
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.home.HomePanel;
import org.limewire.ui.swing.mainframe.StorePanel;
import org.limewire.ui.swing.nav.Navigator.NavCategory;

import com.google.inject.Singleton;

@Singleton
public class NavTree extends JXPanel implements NavigableTree {
        
    private final List<NavSelectionListener> navSelectionListeners = new ArrayList<NavSelectionListener>();
    private final List<NavList> navigableLists;

    public NavTree() {
        this.navigableLists = new ArrayList<NavList>();
        setLayout(new GridBagLayout());
        
        addNavList(new NavList("LimeWire", Navigator.NavCategory.LIMEWIRE));
        addNavList(new NavList("Library", Navigator.NavCategory.LIBRARY));
        
        NavList hiddenList = new NavList("Download", Navigator.NavCategory.DOWNLOAD);
        hiddenList.setVisible(false);
        addNavList(hiddenList);
        
        hiddenList = new NavList("Search", Navigator.NavCategory.SEARCH);
        hiddenList.setVisible(false);
        addNavList(hiddenList);
        
        hiddenList = new NavList("Sharing", Navigator.NavCategory.SHARING);
        hiddenList.setVisible(false);
        addNavList(hiddenList);
        
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
    public void addNavigableItem(NavCategory category, NavItem navItem, boolean userRemovable) {
        for(NavList list : navigableLists) {
            if(list.getCategory() == category) {
                list.addNavItem(navItem, userRemovable);
            }
        }
    }
    
    @Override
    public NavItem getNavigableItemByName(NavCategory category, String name) {
        for(NavList list : navigableLists) {
            if(list.getCategory() == category) {
                return list.getNavItem(name);
            }
        }
        return null;
    }

    @Override
    public boolean hasNavigableItem(NavCategory category, String name) {
        for(NavList list : navigableLists) {
            if(list.getCategory() == category) {
                return list.hasNavItem(name);
            }
        }
        return false;
    }

    @Override
    public void selectNavigableItem(NavCategory category, NavItem navItem) {
        for(NavList list : navigableLists) {
            if(list.getCategory() == category) {
                list.selectItem(navItem);
            }
        }
    }
    
    public void selectNavigableItemByName(NavCategory category, String name) {
        for(NavList list : navigableLists) {
            if(list.getCategory() == category) {
                list.selectItemByName(name);
            }
        }
    }
    
    @Override
    public void removeNavigableItem(NavCategory category, NavItem navItem) {
        for(NavList list : navigableLists) {
            if(list.getCategory() == category) {
                list.removeNavItem(navItem);
            }
        }
    }
    
    @Override
    public void addNavSelectionListener(NavSelectionListener listener) {
        navSelectionListeners.add(listener);
    }
    
    public void goHome() {
        selectNavigableItemByName(NavCategory.LIMEWIRE, HomePanel.NAME);
    }
    
    public void showDownloads(){
        selectNavigableItemByName(NavCategory.DOWNLOAD, MainDownloadPanel.NAME);
    }
    
    public void showStore(){
        selectNavigableItemByName(NavCategory.LIMEWIRE, StorePanel.NAME);
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
                            listener.navItemSelected(navList.getCategory(), list.getNavItem());
                        }
                    }
                }
            }
        }
    }
    
}
