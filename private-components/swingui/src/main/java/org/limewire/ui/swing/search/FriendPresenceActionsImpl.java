package org.limewire.ui.swing.search;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.friend.api.FriendPresence;
import org.limewire.inject.LazySingleton;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
class FriendPresenceActionsImpl implements FriendPresenceActions {

    @InspectablePrimitive(value = "browse host", category = DataCategory.USAGE)
    private volatile int numBrowseHost = 0;

    //Provider prevents circular dependency
    private final Provider<SearchNavigator> searchNavigator;
    private final Provider<BrowseSearchFactory> browseSearchFactory;
    private final BrowsePanelFactory browsePanelFactory;
    private final Map<String,SearchNavItem> browseNavItemCache = new HashMap<String, SearchNavItem>();

    @Inject
    public FriendPresenceActionsImpl(
            BrowsePanelFactory browsePanelFactory, Provider<SearchNavigator> searchNavigator,
            Navigator navigator, Provider<BrowseSearchFactory> browseSearchFactory) {
        this.browsePanelFactory = browsePanelFactory;
        this.searchNavigator = searchNavigator;
        this.browseSearchFactory = browseSearchFactory;
    }
 
    @Override
    public void viewLibrariesOf(Collection<FriendPresence> people) {
        numBrowseHost += people.size();
        if(people.size() == 1) {
            FriendPresence person = people.iterator().next();
            if (!navigateIfTabExists(person.getFriend().getId())) {
                browse(browseSearchFactory.get().createBrowseSearch(person), 
                        DefaultSearchInfo.createBrowseSearch(SearchType.SINGLE_BROWSE),
                        person.getFriend().getRenderName(), person.getFriend().getId());
            }
        } else if(!people.isEmpty()) {
            browse(browseSearchFactory.get().createBrowseSearch(people), 
                    DefaultSearchInfo.createBrowseSearch(SearchType.MULTIPLE_BROWSE),
                    getTabTitle(people),
                    null);
        }
    }
    
    private String getTabTitle(Collection<FriendPresence> people){
        if(!people.isEmpty())
            return I18n.trn("{0} P2P User", "{0} P2P Users", people.size());
        return "";
    }

    /**
     * @param key - null to not cache browse
     */
    private void browse(BrowseSearch search, SearchInfo searchInfo, String title, String key) {  
        
        SearchResultsPanel searchPanel = browsePanelFactory.createBrowsePanel(search, searchInfo);
        // Add search results display to the UI, and select its navigation item.
        SearchNavItem item = searchNavigator.get().addSearch(title, searchPanel, search, searchPanel.getModel());
        item.select();
        searchPanel.getModel().start(new SwingSearchListener(searchPanel.getModel(), item));
        searchPanel.setBrowseTitle(title);
        
        if (key != null) {
            browseNavItemCache.put(key, item);
            item.addNavItemListener(new ItemRemovalListener(key));
        }
    }
    
    /** 
     * Selects a tab in search results if there's already an open browse
     * for the key.  Returns true if an existing tab was selected.
     */
    private boolean navigateIfTabExists(String key){
        if (key != null && browseNavItemCache.get(key) != null) {
            browseNavItemCache.get(key).select();
            return true;
        }
        
        return false;
    }
    
    private class ItemRemovalListener implements NavItemListener {
        private String key;

        public ItemRemovalListener(String key){
            this.key = key;
        }

        @Override
        public void itemRemoved(boolean wasSelected) {
            browseNavItemCache.remove(key);
        }

        @Override
        public void itemSelected(boolean selected) {
            //do nothing            
        }        
    }
}
