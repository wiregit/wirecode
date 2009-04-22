package org.limewire.ui.swing.mainframe;

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
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.FancyTabList;
import org.limewire.ui.swing.components.FancyTabListFactory;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.home.HomePanel;
import org.limewire.ui.swing.library.MyLibraryIntroPanel;
import org.limewire.ui.swing.library.MyLibraryPanel;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.painter.factories.SearchTabPainterFactory;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchBar;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.SearchNavItem;
import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.search.UiSearchListener;
import org.limewire.ui.swing.search.advanced.AdvancedSearchPanel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.mozilla.browser.MozillaInitialization;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class TopPanel extends JXPanel implements SearchNavigator {
    
    private final SearchBar searchBar;
    
    private final FancyTabList searchList;
    private final Navigator navigator;        
    private final NavItem homeNav;      
    private final NavItem libraryNav;  
    private final NavItem friendNav;
    private JButton myLibraryIntroButton;

        
    @Inject
    public TopPanel(final SearchHandler searchHandler,
                    Navigator navigator,
                    final HomePanel homePanel,
                    final StorePanel storePanel,
                    final LeftPanel leftPanel,
                    SearchBar searchBar,
                    FancyTabListFactory fancyTabListFactory,
                    BarPainterFactory barPainterFactory,
                    SearchTabPainterFactory tabPainterFactory,
                    final LibraryNavigator libraryNavigator,
                    AdvancedSearchPanel advancedSearchPanel,
                    final MyLibraryPanel myLibraryPanel) {        
        GuiUtils.assignResources(this);
        
        this.searchBar = searchBar;
        this.navigator = navigator;
        this.searchBar.addSearchActionListener(new Searcher(searchHandler)); 
        
        setName("WireframeTop");
        
        setBackgroundPainter(barPainterFactory.createTopBarPainter());
        
        // add advanced search into the navigator, for use elsewhere.
        navigator.createNavItem(NavCategory.LIMEWIRE, AdvancedSearchPanel.NAME, advancedSearchPanel);
        advancedSearchPanel.addSearchListener(new UiSearchListener() {
            @Override
            public void searchTriggered(SearchInfo searchInfo) {
                searchHandler.doSearch(searchInfo);
            }
        });
        
        homeNav = navigator.createNavItem(NavCategory.LIMEWIRE, HomePanel.NAME, homePanel);      
        JButton homeButton = new IconButton(NavigatorUtils.getNavAction(homeNav));
        homeButton.setName("WireframeTop.homeButton");
        homeButton.setToolTipText(I18n.tr("Home"));
        homeButton.setText(null);
        homeButton.setIconTextGap(1);
        homeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                homePanel.loadDefaultUrl();
            }
        });
        
        JButton storeButton;
        if(MozillaInitialization.isInitialized()) {
            NavItem storeNav = navigator.createNavItem(NavCategory.LIMEWIRE, StorePanel.NAME, storePanel);
            storeButton = new IconButton(NavigatorUtils.getNavAction(storeNav));
        } else {
            storeButton = new IconButton();
        }
        storeButton.setName("WireframeTop.storeButton");
        storeButton.setToolTipText(I18n.tr("LimeWire Store"));
        storeButton.setText(null);
        storeButton.setIconTextGap(1);
        storeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                storePanel.loadDefaultUrl();
            }
        });
         
        libraryNav = navigator.createNavItem(NavCategory.LIBRARY, MyLibraryPanel.NAME, myLibraryPanel);      
        final JButton myLibraryButton = new IconButton(NavigatorUtils.getNavAction(libraryNav));
        myLibraryButton.setName("WireframeTop.libraryButton");
        myLibraryButton.setToolTipText(I18n.tr("My Library"));
        myLibraryButton.setText(null);
        myLibraryButton.setIconTextGap(1);
        myLibraryButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                myLibraryPanel.showAllFiles();                
            }            
        });
        //changed for advanced filters usability test, don't need warning cause not testing library and sharing
        if (false) {
            myLibraryButton.setVisible(false);
            Action introAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    myLibraryButton.setVisible(true);
                    myLibraryIntroButton.setVisible(false);
                    myLibraryPanel.showAllFiles();
                    libraryNav.select();
                }
            };
            NavItem introNav = navigator.createNavItem(NavCategory.LIBRARY, MyLibraryIntroPanel.NAME, new MyLibraryIntroPanel(introAction));
            myLibraryIntroButton = new IconButton(NavigatorUtils.getNavAction(introNav));
            myLibraryIntroButton.setName("WireframeTop.libraryIntroButton");
            myLibraryIntroButton.setToolTipText(I18n.tr("My Library"));
            myLibraryIntroButton.setText(null);
            myLibraryIntroButton.setIconTextGap(1);
        } else {
            myLibraryIntroButton = null;
        }
        
        friendNav = navigator.createNavItem(NavCategory.LIBRARY, "Friend panel", new JPanel());      
        JButton friendButton = new IconButton(NavigatorUtils.getNavAction(friendNav));
        friendButton.setName("WireframeTop.friendButton");
        friendButton.setToolTipText(I18n.tr("My Friends"));
        friendButton.setText(null);
        friendButton.setIconTextGap(1);
   
     
        searchList = fancyTabListFactory.create();
        searchList.setName("WireframeTop.SearchList");
        searchList.setMaxVisibleTabs(3);
        searchList.setMaxTotalTabs(10);
        searchList.setCloseAllText(I18n.tr("Close All Searches"));
        searchList.setCloseOneText(I18n.tr("Close Search"));
        searchList.setCloseOtherText(I18n.tr("Close Other searches"));
        searchList.setRemovable(true);
        searchList.setSelectionPainter(tabPainterFactory.createSelectionPainter());
        searchList.setHighlightPainter(tabPainterFactory.createHighlightPainter());
        searchList.setTabInsets(new Insets(0,10,2,10));

        setLayout(new MigLayout("gap 0 0 0 0, insets 0 0 0 0, novisualpadding, filly"));        
        add(myLibraryButton, "gapbottom 2, gaptop 0, gapleft 15, hidemode 3");       
        if (myLibraryIntroButton != null) {
            add(myLibraryIntroButton, "gapbottom 2, gaptop 0, gapleft 15, hidemode 3");
        }
        //add(homeButton, "gapbottom 2, gaptop 0");
        add(storeButton, "gapbottom 2, gaptop 0, gapleft 10");
        add(friendButton, "gapbottom 2, gaptop 0, gapleft 10");

        add(searchBar, "gapleft 14, gapbottom 2, gaptop 0");
        add(searchList, "gapleft 10, gaptop 3, gapbottom 1, growy");
        
        // Do not show store if mozilla failed to load.
        if(!MozillaInitialization.isInitialized()) {
            storeButton.setVisible(false);
        }
        
        navigator.addNavigationListener(new NavigationListener() {
            @Override
            public void categoryRemoved(NavCategory category) {
                if(category == NavCategory.SEARCH_RESULTS) {
                    libraryNavigator.selectLibrary();
                }
            }
            
            @Override public void categoryAdded(NavCategory category) {}
            @Override public void itemAdded(NavCategory category, NavItem navItem, JComponent panel) {}
            @Override public void itemRemoved(NavCategory category, NavItem navItem, JComponent panel) {}
            @Override public void itemSelected(NavCategory category, NavItem navItem, NavSelectable selectable, JComponent panel) {}
      ;  });
    };

    @Override
    public boolean requestFocusInWindow() {
        return searchBar.requestFocusInWindow();
    }

    @Override
    public SearchNavItem addSearch(
        String title, final JComponent searchPanel, final Search search) {
        
        final NavItem item = navigator.createNavItem(
            NavCategory.SEARCH_RESULTS, title, searchPanel);
        final SearchAction action = new SearchAction(item);
        search.addSearchListener(action);

        final Action moreTextAction = new NoOpAction();
        final RepeatSearchAction repeat = new RepeatSearchAction(search);
        search.addSearchListener(repeat);

        final TabActionMap actionMap = new TabActionMap(
            action, action, moreTextAction, Collections.singletonList(repeat));
        
        searchList.addTabActionMapAt(actionMap, 0);
        
        item.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                searchList.removeTabActionMap(actionMap);
                search.stop();
                ((Disposable)searchPanel).dispose();
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

    public void goHome() {
        homeNav.select();
    }
    
    private class Searcher implements ActionListener {
        private final SearchHandler searchHandler;
        
        Searcher(SearchHandler searchHandler) {
            this.searchHandler = searchHandler;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            // Get search text, and do search if non-empty.
            String searchText = searchBar.getSearchText();
            if (!searchText.isEmpty()) {
                if(searchHandler.doSearch(
                        DefaultSearchInfo.createKeywordSearch(searchText,  
                                searchBar.getCategory()))) {
                    searchBar.selectAllSearchText();
                }
            }
        }
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
                searchBar.requestSearchFocus();
            } else if (e.getActionCommand().equals(TabActionMap.REMOVE_COMMAND)) {
                item.remove();
            }
        }

        @Override
        public void handleSearchResult(Search search, SearchResult searchResult) {
        }
        
        @Override
        public void handleSponsoredResults(Search search, List<SponsoredResult> sponsoredResults) {
            // do nothing
        }
        
        void killBusy() {
            busyTimer.stop();
            putValue(TabActionMap.BUSY_KEY, Boolean.FALSE);
        }
        
        @Override
        public void searchStarted(Search search) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    busyTimer = new Timer(60000, new ActionListener() {
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
        public void searchStopped(Search search) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    killBusy();
                }
            });
        }
    }

    /**
     * Action which repeats a search.  Since it makes no sense to
     * repeat an unstarted search, this class only enables searches to be
     * repeated if they have already started.
     */
    private class RepeatSearchAction extends AbstractAction implements SearchListener {

        private final Search search;

        RepeatSearchAction(Search search) {
            super(I18n.tr("Find More Results"));
            this.search = search;
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            search.repeat();
        }

        @Override
        public void searchStarted(Search search) {
            setEnabled(true);
        }

        @Override public void searchStopped(Search search) { }
        @Override public void handleSponsoredResults(Search search, List<SponsoredResult> sponsoredResults) { }
        @Override public void handleSearchResult(Search search, SearchResult searchResult) {}
    }
}
