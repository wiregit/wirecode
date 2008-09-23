/**
 * 
 */
package org.limewire.ui.swing.nav;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComponent;

import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.home.HomePanel;
import org.limewire.ui.swing.library.MyLibraryPanel;
import org.limewire.ui.swing.mainframe.StorePanel;
import org.limewire.ui.swing.search.SearchHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class NavigatorImpl implements Navigator {

    private final NavigableTarget navTarget;
    private final NavigableTree navTree;
    private final CopyOnWriteArrayList<NavSelectionListener> listeners = new CopyOnWriteArrayList<NavSelectionListener>();

    @Inject
    public NavigatorImpl(NavigableTarget navTarget, NavigableTree navTree) {
        this.navTarget = navTarget;
        this.navTree = navTree;
        
        navTree.addNavSelectionListener(new NavSelectionListener() {
            @Override
            public void navItemSelected(NavCategory category, NavItem navItem) {
                showNavigablePanel(category, navItem);
                for(NavSelectionListener listener : listeners) {
                    listener.navItemSelected(category, navItem);
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.Navigator#addNavigablePanel(org.limewire.ui.swing.nav.NavigatorImpl.NavItem, java.lang.String, javax.swing.JPanel)
     */
    public NavItem addNavigablePanel(final NavCategory category, final String name, JComponent panel, boolean userRemovable) {
        NavItem item = new NavItem() {
            @Override
            public String getName() {
                return name;
            }
            
            @Override
            public void remove() {
                removeNavigablePanel(category, this);
            }
            
            @Override
            public void select() {
                selectNavigablePanel(category, this);
            }
        };
        navTree.addNavigableItem(category, item);
        navTarget.addNavigablePanel(item, panel);
        return item;
    }

    public void removeNavigablePanel(NavCategory category, String name) {
        NavItem navItem = navTree.getNavigableItemByName(category, name);
        if(navItem != null) {
            removeNavigablePanel(category, navItem);
        }
    }

    private void removeNavigablePanel(NavCategory category, NavItem navItem) {
        navTree.removeNavigableItem(category, navItem);
        navTarget.removeNavigablePanel(navItem);
    }

    public boolean hasNavigablePanel(NavCategory category, String name) {
        return navTree.hasNavigableItem(category, name);
    }

    private void selectNavigablePanel(NavCategory category, NavItem navItem) {
        navTree.selectNavigableItem(category, navItem);
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.Navigator#showNavigablePanel(org.limewire.ui.swing.nav.NavigatorImpl.NavItem, java.lang.String)
     */
    private void showNavigablePanel(NavCategory category, NavItem navItem) {
        navTarget.showNavigablePanel(navItem);
    }

    @Inject
    public void addDefaultNavigableItems(SearchHandler searchHandler, HomePanel homePanel,
            StorePanel storePanel, MyLibraryPanel musicPanel, MainDownloadPanel mainDownloadPanel) {
        addNavigablePanel(NavCategory.LIMEWIRE, HomePanel.NAME, homePanel, false);
        addNavigablePanel(NavCategory.LIMEWIRE, StorePanel.NAME, storePanel, false);

        addNavigablePanel(NavCategory.LIBRARY, MyLibraryPanel.NAME, musicPanel, false);
        
        addNavigablePanel(NavCategory.DOWNLOAD, MainDownloadPanel.NAME, mainDownloadPanel, false);
    }

    @Override
    public void addNavListener(NavSelectionListener itemListener) {
        listeners.add(itemListener);
    }
    
    @Override
    public void removeNavListener(NavSelectionListener itemListener) {
        listeners.remove(itemListener);
    }
}
