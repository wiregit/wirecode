package org.limewire.ui.swing.nav;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.home.HomePanel;
import org.limewire.ui.swing.mainframe.StorePanel;
import org.limewire.ui.swing.nav.NavList.SelectionListener;

import com.google.inject.Singleton;

@Singleton
public class NavTree extends JXPanel implements NavigableTree {
        
    private final List<NavSelectionListener> navSelectionListeners = new ArrayList<NavSelectionListener>();
    private final List<NavList> navigableLists;

    public NavTree() {
        this.navigableLists = new ArrayList<NavList>();
        setLayout(new GridBagLayout());
        
        addNavList(new ListNavList("LimeWire", NavCategory.LIMEWIRE), true);
        addNavList(new ListNavList("Library", NavCategory.LIBRARY), true);        
        addNavList(new SimpleNavList(NavCategory.DOWNLOAD), false);        
        addNavList(new SimpleNavList(NavCategory.SEARCH), false);        
        addNavList(new SimpleNavList(NavCategory.SHARING), false);     
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 20, 0);
        gbc.weighty = 1;
        add(Box.createGlue(), gbc);
    }
    
    private void addNavList(NavList navList, boolean visible) {
        if(visible) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 0, 20, 0);            
            add(navList.getComponent(), gbc);
        }
        
        navigableLists.add(navList);
        navList.addSelectionListener(new Listener());
    }
    
    @Override
    public void addNavigableItem(NavCategory category, NavItem navItem) {
        for(NavList list : navigableLists) {
            if(list.getCategory() == category) {
                list.addNavItem(navItem);
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
    
    private class Listener implements SelectionListener {        
        @Override
        public void selectionChanged(NavList source) {
            if(source.isNavItemSelected()) {
                for(NavList navList : navigableLists) {
                    if(navList != source) {
                        navList.clearSelection();
                    } else {
                        for(NavSelectionListener listener : navSelectionListeners) {
                            listener.navItemSelected(navList.getCategory(), source.getSelectedNavItem());
                        }
                    }
                }
            }
        }
    }
    
}
