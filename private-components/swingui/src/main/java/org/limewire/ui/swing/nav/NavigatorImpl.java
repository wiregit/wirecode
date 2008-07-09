/**
 * 
 */
package org.limewire.ui.swing.nav;

import javax.swing.JPanel;

import org.limewire.ui.swing.home.HomePanel;
import org.limewire.ui.swing.library.DocumentPanel;
import org.limewire.ui.swing.library.ImagePanel;
import org.limewire.ui.swing.library.MusicPanel;
import org.limewire.ui.swing.library.VideoPanel;
import org.limewire.ui.swing.mainframe.StorePanel;
import org.limewire.ui.swing.search.SearchHandler;

public class NavigatorImpl implements Navigator {

    private final NavigableTarget navTarget;
    private final NavigableTree navTree;

    public NavigatorImpl(NavigableTarget navTarget, NavigableTree navTree) {
        this.navTarget = navTarget;
        this.navTree = navTree;        
        
        navTree.addNavSelectionListener(new NavSelectionListener() {
            @Override
            public void navItemSelected(Navigator.NavCategory category, NavItem navItem) {
                showNavigablePanel(category, navItem);
            }
        });
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.Navigator#addNavigablePanel(org.limewire.ui.swing.nav.NavigatorImpl.NavItem, java.lang.String, javax.swing.JPanel)
     */
    public NavItem addNavigablePanel(final NavCategory category, final String name, JPanel panel, boolean userRemovable) {
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
        navTree.addNavigableItem(category, item, userRemovable);
        navTarget.addNavigablePanel(item, panel);
        return item;
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.Navigator#removeNavigablePanel(org.limewire.ui.swing.nav.NavigatorImpl.NavItem, java.lang.String)
     */
    public void removeNavigablePanel(NavCategory category, NavItem navItem) {
        navTree.removeNavigableItem(category, navItem);
        navTarget.removeNavigablePanel(navItem);
    }
    
    @Override
    public void selectNavigablePanel(NavCategory category, NavItem navItem) {
        navTree.selectNavigableItem(category, navItem);
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.Navigator#showNavigablePanel(org.limewire.ui.swing.nav.NavigatorImpl.NavItem, java.lang.String)
     */
    private void showNavigablePanel(NavCategory category, NavItem navItem) {
        navTarget.showNavigablePanel(navItem);
    }

    public void addDefaultNavigableItems(SearchHandler searchHandler) {
        addNavigablePanel(NavCategory.LIMEWIRE, HomePanel.NAME, new HomePanel(searchHandler), false);
        addNavigablePanel(NavCategory.LIMEWIRE, StorePanel.NAME, new StorePanel(), false);

        addNavigablePanel(NavCategory.LIBRARY, MusicPanel.NAME, new MusicPanel(), false);
        addNavigablePanel(NavCategory.LIBRARY, VideoPanel.NAME, new VideoPanel(), false);
        addNavigablePanel(NavCategory.LIBRARY, ImagePanel.NAME, new ImagePanel(), false);
        addNavigablePanel(NavCategory.LIBRARY, DocumentPanel.NAME, new DocumentPanel(), false);
    }
}
