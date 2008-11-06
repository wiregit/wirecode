package org.limewire.ui.swing.mainframe;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.friend.FriendAutoCompleters;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.SearchBar;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.home.HomePanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.painter.SearchTabSelectionPainter;
import org.limewire.ui.swing.painter.TopPanelPainter;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchNavItem;
import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class TopPanel extends JXPanel implements SearchNavigator {
    
    private final SearchBar searchBar;
    
    private final FancyTabList searchList;
    private final Navigator navigator;
        
    private final NavItem homeNav;
    
    @Inject
    public TopPanel(final SearchHandler searchHandler,
                    Navigator navigator,
                    final FriendAutoCompleters friendLibraries,
                    HomePanel homePanel,
                    StorePanel storePanel,
                    SearchBar searchBar) {
        
        GuiUtils.assignResources(this);
        
        this.searchBar = searchBar;        
        this.searchBar.setSearchHandler(searchHandler);
        
        this.navigator = navigator;
        
        setName("TopPanel");
        
        setBackgroundPainter(new TopPanelPainter());
        
        homeNav = navigator.createNavItem(NavCategory.LIMEWIRE, HomePanel.NAME, homePanel);
        NavItem storeNav = navigator.createNavItem(NavCategory.LIMEWIRE, StorePanel.NAME, storePanel);
        JButton homeButton = new IconButton(NavigatorUtils.getNavAction(homeNav));
        homeButton.setName("TopPanel.homeButton");
        homeButton.setText(I18n.tr("Home"));
        homeButton.setIconTextGap(1);
        JButton storeButton = new IconButton(NavigatorUtils.getNavAction(storeNav));
        storeButton.setName("TopPanel.storeButton");
        storeButton.setText(I18n.tr("Store"));
        storeButton.setIconTextGap(1);
        
        searchList = new FancyTabList();
        searchList.setName("TopPanel.SearchList");
        searchList.setMaxVisibleTabs(3);
        searchList.setMaxTotalTabs(10);
        searchList.setCloseAllText(I18n.tr("Close all searches"));
        searchList.setCloseOneText(I18n.tr("Close search"));
        searchList.setCloseOtherText(I18n.tr("Close other searches"));
        searchList.setRemovable(true);
        searchList.setPreferredSize(
                new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        searchList.setSelectionPainter(new SearchTabSelectionPainter());
        searchList.setHighlightPainter(null);
        searchList.setTabInsets(new Insets(0,20,0,20));
                
        setLayout(new MigLayout("gap 0, insets 0, fill", "", "[center]"));        
        add(homeButton);
        add(storeButton);
        add(searchBar, "gapleft 65, gapright 30");
        add(searchList, "gapleft 4, gaptop 6, grow");
    };

    @Override
    public boolean requestFocusInWindow() {
        return searchBar.requestFocusInWindow();
    }

    @Override
    public SearchNavItem addSearch(
        String title, JComponent searchPanel, final Search search) {
        
        final NavItem item = navigator.createNavItem(
            NavCategory.SEARCH, title, searchPanel);
        final SearchAction action = new SearchAction(item);
        search.addSearchListener(action);

        final Action moreTextAction = new NoOpAction();
        final Action repeat = new AbstractAction(I18n.tr("Repeat search")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                search.repeat();
            }
        };
        
        final TabActionMap actionMap = new TabActionMap(
            action, action, moreTextAction, Collections.singletonList(repeat));
        
        searchList.addTabActionMapAt(actionMap, 0);
        
        item.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                searchList.removeTabActionMap(actionMap);
                search.stop();
            }

            @Override
            public void itemSelected(boolean selected) {
                searchBar.setText(item.getId());
                action.putValue(Action.SELECTED_KEY, selected);
            }
        });
        
        return new SearchNavItem() {
            @Override
            public void sourceCountUpdated(int newSourceCount) {
                if(!item.isSelected()) {
                    action.putValue(TabActionMap.NEW_HINT, true);
                }

                if (newSourceCount >= 50) {
                    action.killBusy();
                }
            }
            
            @Override
            public String getId() {
                return item.getId();
            }
            
            @Override
            public void remove() {
                item.remove();
            }
            
            @Override
            public void select() {
                select(null);
            }
            
            @Override
            public void select(NavSelectable selectable) {
                item.select();
            }
            
            @Override
            public void addNavItemListener(NavItemListener listener) {
                item.addNavItemListener(listener);
            }
            
            @Override
            public void removeNavItemListener(NavItemListener listener) {
                item.removeNavItemListener(listener);
            }
            
            @Override
            public boolean isSelected() {
                return item.isSelected();
            }
        };
    }
    
    private class SearchAction extends AbstractAction implements SearchListener {
        private final NavItem item;
        private Timer busyTimer;
        
        public SearchAction(NavItem item) {
            super(item.getId());
            this.item = item;

            // Make sure this syncs up with any changes in selection.
            addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if (evt.getNewValue().equals(Boolean.TRUE)) {
                            SearchAction.this.item.select();
                            putValue(TabActionMap.NEW_HINT, null);
                        }
                    }
                }
            });
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals(TabActionMap.SELECT_COMMAND)) {
                item.select();
            } else if (e.getActionCommand().equals(TabActionMap.REMOVE_COMMAND)) {
                item.remove();
            }
        }

        @Override
        public void handleSearchResult(SearchResult searchResult) {
        }
        
        @Override
        public void handleSponsoredResults(List<SponsoredResult> sponsoredResults) {
            // do nothing
        }
        
        void killBusy() {
            busyTimer.stop();
            putValue(TabActionMap.BUSY_KEY, Boolean.FALSE);
        }
        
        @Override
        public void searchStarted() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    busyTimer = new Timer(15000, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            putValue(TabActionMap.BUSY_KEY, Boolean.FALSE);
                        }
                    });
                    busyTimer.setRepeats(false);
                    busyTimer.start();
                    putValue(TabActionMap.BUSY_KEY, Boolean.TRUE);
                }
            });
        }
        
        @Override
        public void searchStopped() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    killBusy();
                }
            });
        }
    }

    public void goHome() {
        homeNav.select();
    }
}