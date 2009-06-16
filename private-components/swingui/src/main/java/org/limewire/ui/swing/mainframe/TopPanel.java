package org.limewire.ui.swing.mainframe;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.FlexibleTabList;
import org.limewire.ui.swing.components.FlexibleTabListFactory;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.NoOpAction;
import org.limewire.ui.swing.components.TabActionMap;
import org.limewire.ui.swing.friends.actions.FriendButtonPopupListener;
import org.limewire.ui.swing.home.HomeMediator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.painter.factories.SearchTabPainterFactory;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.KeywordAssistedSearchBuilder;
import org.limewire.ui.swing.search.SearchBar;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.SearchNavItem;
import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.search.SearchResultMediator;
import org.limewire.ui.swing.search.UiSearchListener;
import org.limewire.ui.swing.search.KeywordAssistedSearchBuilder.CategoryOverride;
import org.limewire.ui.swing.search.advanced.AdvancedSearchPanel;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.mozilla.browser.MozillaInitialization;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class TopPanel extends JXPanel implements SearchNavigator {
    
    @Resource Icon friendIcon;
    
    private final SearchBar searchBar;
    
    private final FlexibleTabList searchList;
    private final Navigator navigator;
    private final NavItem homeNav;
    private final NavItem libraryNav;
    private final KeywordAssistedSearchBuilder keywordAssistedSearchBuilder;
    private final Provider<AdvancedSearchPanel> advancedSearchPanel;
    private final SearchHandler searchHandler;
    @Resource private Icon browseIcon;
        
    @Inject
    public TopPanel(final SearchHandler searchHandler,
                    Navigator navigator,
                    final HomeMediator homeMediator,
                    final StoreMediator storeMediator,
                    SearchBar searchBar,
                    FlexibleTabListFactory tabListFactory,
                    BarPainterFactory barPainterFactory,
                    SearchTabPainterFactory tabPainterFactory,
                    final LibraryMediator myLibraryMediator,
                    KeywordAssistedSearchBuilder keywordAssistedSearchBuilder,
                    FriendButtonPopupListener friendListener,
                    Provider<AdvancedSearchPanel> advancedSearchPanel) {        
        GuiUtils.assignResources(this);
        this.searchHandler = searchHandler;
        this.searchBar = searchBar;
        this.navigator = navigator;
        this.searchBar.addSearchActionListener(new Searcher(searchHandler));        
        this.keywordAssistedSearchBuilder = keywordAssistedSearchBuilder;
        this.advancedSearchPanel = advancedSearchPanel;
        
        setName("WireframeTop");
        
        setBackgroundPainter(barPainterFactory.createTopBarPainter());
        
        homeNav = navigator.createNavItem(NavCategory.LIMEWIRE, HomeMediator.NAME, homeMediator);      
        libraryNav = navigator.createNavItem(NavCategory.LIBRARY, I18n.tr("My Library"), myLibraryMediator);
        JButton libraryButton = new IconButton(NavigatorUtils.getNavAction(libraryNav));
        libraryButton.setName("WireframeTop.homeButton");
        libraryButton.setToolTipText(I18n.tr("Library"));
        libraryButton.setText(null);
        libraryButton.setIconTextGap(1);
//        homeButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                homeMediator.getComponent().loadDefaultUrl();
//            }
//        });
        
//        JButton friendButton = new IconButton(signInAction);
        LimeComboBox friendButton = new LimeComboBox();
        friendButton.setIcon(friendIcon);
        friendButton.setName("WireframeTop.friendButton");
        friendButton.setToolTipText(I18n.tr("Friend Login"));
        friendButton.setText(null);
        friendButton.setIconTextGap(1);
        JPopupMenu menu = new JPopupMenu();
        friendButton.overrideMenu(menu);
        menu.addPopupMenuListener(friendListener);
        
        JButton storeButton;
        if(MozillaInitialization.isInitialized()) {
            NavItem storeNav = navigator.createNavItem(NavCategory.LIMEWIRE, StoreMediator.NAME, storeMediator);
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
                storeMediator.getComponent().loadDefaultUrl();
            }
        });
     
        searchList = tabListFactory.create();
        searchList.setName("WireframeTop.SearchList");
        searchList.setCloseAllText(I18n.tr("Close All Searches"));
        searchList.setCloseOneText(I18n.tr("Close Search"));
        searchList.setCloseOtherText(I18n.tr("Close Other searches"));
        searchList.setRemovable(true);
        searchList.setSelectionPainter(tabPainterFactory.createSelectionPainter());
        searchList.setHighlightPainter(tabPainterFactory.createHighlightPainter());
        searchList.setTabInsets(new Insets(0,10,2,10));

        setLayout(new MigLayout("gap 0, insets 0, fill, alignx leading"));
        add(libraryButton, "gapbottom 2, gaptop 0");
        add(friendButton, "gapbottom 2, gaptop 0");
        add(storeButton, "gapbottom 2, gaptop 0");

        add(searchBar, "gapleft 70, gapbottom 2, gaptop 0");
        add(searchList, "gapleft 10, gaptop 3, gapbottom 1, grow, push");
        
        // Do not show store if mozilla failed to load.
        if(!MozillaInitialization.isInitialized()) {
            storeButton.setVisible(false);
        }
        
//        navigator.addNavigationListener(new NavigationListener() {
//            @Override
//            public void categoryRemoved(NavCategory category) {
//                if(category == NavCategory.SEARCH_RESULTS) {
//                    libraryNavigator.selectLibrary();
//                }
//            }
//            
//            @Override public void categoryAdded(NavCategory category) {}
//            @Override public void itemAdded(NavCategory category, NavItem navItem) {}
//            @Override public void itemRemoved(NavCategory category, NavItem navItem) {}
//            @Override public void itemSelected(NavCategory category, NavItem navItem, NavSelectable selectable, NavMediator navMediator) {}
//      ;  });
    };

    @Override
    public boolean requestFocusInWindow() {
        return searchBar.requestFocusInWindow();
    }
    

    @Override
    public SearchNavItem addSearch(String title, final JComponent searchPanel, final BrowseSearch search, SearchResultsModel model) {
        return addSearch(title, searchPanel, search, model, browseIcon);
    }

    @Override
    public SearchNavItem addSearch(String title, final JComponent searchPanel, final Search search, SearchResultsModel model) {
        return addSearch(title, searchPanel, search, model, null);
    }
    
    @Override
    public SearchNavItem addAdvancedSearch() {
        String title = I18n.tr("Advanced Search");
        final AdvancedSearchPanel advancedPanel = advancedSearchPanel.get();
        advancedPanel.addSearchListener(new UiSearchListener() {
            @Override
            public void searchTriggered(SearchInfo searchInfo) {
                searchHandler.doSearch(searchInfo);
            }
        });
        final NavItem item = navigator.createNavItem(NavCategory.SEARCH_RESULTS, title, new SearchResultMediator(advancedPanel));
        final SearchAction action = new SearchAction(item);
        
        final Action moreTextAction = new NoOpAction();
        final TabActionMap actionMap = new TabActionMap(
                action, action, moreTextAction, new ArrayList<Action>());
            
            searchList.addTabActionMapAt(actionMap, 0);
            
            item.addNavItemListener(new SearchTabNavItemListener(action, actionMap, item, advancedPanel) {
                @Override
                protected SearchCategory getCategory() {
                    return SearchCategory.ALL;
                }
            });
            
        final SearchNavItem searchNavItem =  new SearchNavItemImpl(item, action);
        
        advancedPanel.addSearchListener(new UiSearchListener() {
            @Override
             public void searchTriggered(SearchInfo searchInfo) {
                searchNavItem.remove();
             } 
         });
        
        return searchNavItem;
    }
    
    private SearchNavItem addSearch(String title, final JComponent searchPanel, final Search search, SearchResultsModel model, Icon icon) {
        final NavItem item = navigator.createNavItem(NavCategory.SEARCH_RESULTS, title, new SearchResultMediator(searchPanel));
        final SearchAction action = new SearchAction(item);
        action.putValue(Action.LARGE_ICON_KEY, icon);
        search.addSearchListener(action);

        final Action moreTextAction = new NoOpAction();
        final Action moreResults = new MoreResultsAction(search).register();
        final Action repeatSearch = new RepeatSearchAction(search, model).register();
        final Action stopSearch = new StopSearchAction(search).register();

        final TabActionMap actionMap = new TabActionMap(
            action, action, moreTextAction, Arrays.asList(stopSearch, repeatSearch, TabActionMap.SEPARATOR, moreResults));
        
        searchList.addTabActionMapAt(actionMap, 0);
        
        item.addNavItemListener(new SearchTabNavItemListener(action, actionMap, item, (Disposable)searchPanel) {
           @Override
           protected SearchCategory getCategory() {
                return search.getCategory();
            } 
        });
        
        return new SearchNavItemImpl(item, action);
    }

    public void goHome() {
        homeNav.select();
    }
    
    /**
     * Specialized NavItemListener that handles disposing the SearchPanel after a search is closed, 
     * removes the panel from the search list. and updates the search bar text and category when a 
     * new search tab is selected. 
     */
    private abstract class SearchTabNavItemListener implements NavItemListener {
        private final SearchAction action;

        private final TabActionMap actionMap;

        private final NavItem item;

        private final Disposable panel;

        private SearchTabNavItemListener(SearchAction action, TabActionMap actionMap, NavItem item,
                Disposable panel) {
            this.action = action;
            this.actionMap = actionMap;
            this.item = item;
            this.panel = panel;
        }

        @Override
        public void itemRemoved() {
            searchList.removeTabActionMap(actionMap);
            panel.dispose();
        }

        @Override
        public void itemSelected(boolean selected) {
            searchBar.setText(item.getId());
            searchBar.setCategory(getCategory());
            action.putValue(Action.SELECTED_KEY, selected);
        }
        /**
         * Returns the category for this search tab. 
         */
        abstract protected SearchCategory getCategory();
    }

    private final class SearchNavItemImpl implements SearchNavItem {
        private final NavItem item;

        private final SearchAction action;

        private SearchNavItemImpl(NavItem item, SearchAction action) {
            this.item = item;
            this.action = action;
        }

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
                
                SearchCategory category = searchBar.getCategory();
                String query = searchText;
                
                // Check if the category was overridden by a keyword 
                CategoryOverride categoryOverride = keywordAssistedSearchBuilder.parseCategoryOverride(query);
                
                // Do not allow searches in the Other category
                if (categoryOverride != null && categoryOverride.getCategory() != SearchCategory.OTHER) {
                    
                    // Set new category and trim the category text out of the search string
                    //  for the actual search
                    category = categoryOverride.getCategory();
                    query = categoryOverride.getCutQuery();

                    // Update the UI with the new category
                    searchBar.setCategory(category);
                }
                    
                // Attempt to parse an advanced search from the search query
                SearchInfo search = keywordAssistedSearchBuilder.attemptToCreateAdvancedSearch(
                        query, category);
                
                // Fall back on the normal search
                if (search == null) {
                    search = DefaultSearchInfo.createKeywordSearch(query, category);
                }
                
                if(searchHandler.doSearch(search)) {
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
    
    private static abstract class SearchListenerAction extends AbstractAction implements SearchListener{
        protected final Search search;

        public SearchListenerAction(String tr, Search search) {
            super(tr);
            this.search = search;
        }        
    
        /**
         * Registers the SearchListenerAction as a SearchListener on search.
         * 
         * @return this
         */
        public SearchListenerAction register(){
            search.addSearchListener(this);
            return this;
        }
        
        @Override
        public void handleSearchResult(Search search, SearchResult searchResult) {}
        @Override
        public void handleSponsoredResults(Search search, List<SponsoredResult> sponsoredResults) {}
    }

    /**
     * Action which repeats a search.  Since it makes no sense to
     * repeat an unstarted search, this class only enables searches to be
     * repeated if they have already started.
     */
    private static class MoreResultsAction extends SearchListenerAction {

        MoreResultsAction(Search search) {
            super(I18n.tr("Find More Results"), search);
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
    }
    
    /**
     * Clears the current search results and repeats the search
     */
    private static class RepeatSearchAction extends SearchListenerAction {
        private SearchResultsModel model;

        RepeatSearchAction(Search search, SearchResultsModel model) {
            super(I18n.tr("Repeat Search"), search);
            this.model = model;
            setEnabled(false);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            model.clear();
            search.repeat();
        }
        
        @Override
        public void searchStopped(Search search) {
            setEnabled(true);
        }

        @Override
        public void searchStarted(Search search) {}
        
    }
    
    /**Stops the search*/
    private static class StopSearchAction extends SearchListenerAction {
        StopSearchAction(Search search) {
            super(I18n.tr("Stop Search"), search);
            setEnabled(false);
        }
                
        @Override
        public void actionPerformed(ActionEvent e) {
            search.stop();
        }          

        @Override
        public void searchStarted(Search search) {
            setEnabled(true);
        }

        @Override
        public void searchStopped(Search search) {
            setEnabled(false);
        } 
    }
}
