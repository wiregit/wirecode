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
            public void navItemSelected(Navigator.NavItem target, String name) {
                showNavigablePanel(target, name);
            }
        });
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.Navigator#addNavigablePanel(org.limewire.ui.swing.nav.NavigatorImpl.NavItem, java.lang.String, javax.swing.JPanel)
     */
    public void addNavigablePanel(Navigator.NavItem target, String name, JPanel panel, boolean userRemovable) {
        navTree.addNavigableItem(target, name, userRemovable);
        navTarget.addNavigablePanel(target + name, panel);
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.Navigator#removeNavigablePanel(org.limewire.ui.swing.nav.NavigatorImpl.NavItem, java.lang.String)
     */
    public void removeNavigablePanel(Navigator.NavItem target, String name) {
        navTree.removeNavigableItem(target, name);
        navTarget.removeNavigablePanel(target + name);
    }
    
    @Override
    public void selectNavigablePanel(NavItem target, String name) {
        navTree.selectNavigableItem(target, name);
    }

    /* (non-Javadoc)
     * @see org.limewire.ui.swing.nav.Navigator#showNavigablePanel(org.limewire.ui.swing.nav.NavigatorImpl.NavItem, java.lang.String)
     */
    private void showNavigablePanel(Navigator.NavItem target, String name) {
        navTarget.showNavigablePanel(target + name);
    }

    public void addDefaultNavigableItems(SearchHandler searchHandler) {
        addNavigablePanel(Navigator.NavItem.LIMEWIRE, HomePanel.NAME, new HomePanel(searchHandler), false);
        addNavigablePanel(Navigator.NavItem.LIMEWIRE, StorePanel.NAME, new StorePanel(), false);

        addNavigablePanel(Navigator.NavItem.LIBRARY, MusicPanel.NAME, new MusicPanel(), false);
        addNavigablePanel(Navigator.NavItem.LIBRARY, VideoPanel.NAME, new VideoPanel(), false);
        addNavigablePanel(Navigator.NavItem.LIBRARY, ImagePanel.NAME, new ImagePanel(), false);
        addNavigablePanel(Navigator.NavItem.LIBRARY, DocumentPanel.NAME, new DocumentPanel(), false);
    }
}
