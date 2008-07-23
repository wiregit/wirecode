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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class NavigatorImpl implements Navigator {

    private final NavigableTarget navTarget;
    private final NavigableTree navTree;

    @Inject
    public NavigatorImpl(@Named("MainTarget") NavigableTarget navTarget, @Named("MainTree") NavigableTree navTree) {
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

    @Inject
    public void addDefaultNavigableItems(SearchHandler searchHandler, HomePanel homePanel,
            StorePanel storePanel, MusicPanel musicPanel, VideoPanel videoPanel,
            ImagePanel imagePanel, DocumentPanel documentPanel) {
        addNavigablePanel(NavCategory.LIMEWIRE, HomePanel.NAME, homePanel, false);
        addNavigablePanel(NavCategory.LIMEWIRE, StorePanel.NAME, storePanel, false);

        addNavigablePanel(NavCategory.LIBRARY, MusicPanel.NAME, musicPanel, false);
        addNavigablePanel(NavCategory.LIBRARY, VideoPanel.NAME, videoPanel, false);
        addNavigablePanel(NavCategory.LIBRARY, ImagePanel.NAME, imagePanel, false);
        addNavigablePanel(NavCategory.LIBRARY, DocumentPanel.NAME, documentPanel, false);
    }
}
