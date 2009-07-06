package org.limewire.ui.swing.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.inject.LazySingleton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.friends.chat.ChatFrame;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
class RemoteHostActionsImpl implements RemoteHostActions {
    private static final Log LOG = LogFactory.getLog(RemoteHostActionsImpl.class);


    private final ChatFrame chatFrame;
//    private final LibraryNavigator libraryNavigator;

    //Provider prevents circular dependency
    private final Provider<SearchNavigator> searchNavigator;


    private final Provider<BrowseSearchFactory> browseSearchFactory;


    private final BrowsePanelFactory browsePanelFactory;
    
    private Map<String,SearchNavItem> browseNavItemCache = new HashMap<String, SearchNavItem>();
    private static final String ALL_FRIENDS_KEY = "ALL_FRIENDS";


    @Inject
    public RemoteHostActionsImpl(ChatFrame chatFrame,  
            BrowsePanelFactory browsePanelFactory, Provider<SearchNavigator> searchNavigator,
            Navigator navigator, Provider<BrowseSearchFactory> browseSearchFactory) {
        this.chatFrame = chatFrame;
        this.browsePanelFactory = browsePanelFactory;
        this.searchNavigator = searchNavigator;
//        this.libraryNavigator = libraryNavigator; 
        this.browseSearchFactory = browseSearchFactory;
    }
 

    @Override
    public void chatWith(RemoteHost person) {
        LOG.debugf("chatWith: {0}", person.getFriendPresence().getFriend());
        Friend friend = person.getFriendPresence().getFriend();
        chatFrame.setVisibility(true);
        chatFrame.fireConversationStarted(friend.getId());

        // TODO make sure the input box for chat gets focus, the code is
        // calling requestFocusInWindow, but I think it is getting some
        // weirdness because the search window is currently the active one, not
        // the chat
    }

    
    @Override
    public void viewLibraryOf(RemoteHost person) {
        assert(person != null);
        LOG.debugf("viewLibraryOf: {0}", person);
        
        if (navigateIfTabExists(person.getFriendPresence().getFriend().getId())) {
            return;
        }
        
        browse(browseSearchFactory.get().createBrowseSearch(person.getFriendPresence()), 
                DefaultSearchInfo.createBrowseSearch(SearchType.SINGLE_BROWSE),
                person.getFriendPresence().getFriend().getRenderName(), person.getFriendPresence().getFriend().getId());
    }
    
    @Override
    public void viewLibraryOf(Friend friend) {
        assert(friend != null && !friend.isAnonymous());
        LOG.debugf("viewLibraryOf: {0}", friend);
        
        if(navigateIfTabExists(friend.getId())) {
            return;
        }
        
        browse(browseSearchFactory.get().createFriendBrowseSearch(friend), 
                DefaultSearchInfo.createBrowseSearch(SearchType.SINGLE_BROWSE),
                friend.getRenderName(), friend.getId());
    }


    @Override
    public void viewLibrariesOf(Collection<RemoteHost> people) {
        List<FriendPresence> presences = new ArrayList<FriendPresence>(people.size());
        for (RemoteHost host : people) {
            presences.add(host.getFriendPresence());
        }
        browse(browseSearchFactory.get().createBrowseSearch(presences), 
                DefaultSearchInfo.createBrowseSearch(SearchType.MULTIPLE_BROWSE),
                getTabTitle(people),
                null);
    }
    
    @Override
    public void browseAllFriends() {
        if(navigateIfTabExists(ALL_FRIENDS_KEY)){
            return;
        }
        
        browse(browseSearchFactory.get().createAllFriendsBrowseSearch(),
                DefaultSearchInfo.createBrowseSearch(SearchType.ALL_FRIENDS_BROWSE), 
                I18n.tr("All Friends"), ALL_FRIENDS_KEY);
    }
    
    private String getTabTitle(Collection<RemoteHost> people){
        boolean hasP2P = hasP2P(people);
        boolean hasFriends = hasFriend(people);
        
        if (hasP2P && hasFriends){
            return I18n.trn("{0} Person", "{0} People", people.size());
        } else if (hasP2P){
            return I18n.trn("{0} P2P User", "{0} P2P Users", people.size());            
        } else if (hasFriends){
            return I18n.trn("{0} Friend", "{0} Friends", people.size());            
        }
        
        //empty list
        return "";
    }

    private boolean hasP2P(Collection<RemoteHost> people) {
        for (RemoteHost host : people) {
            if (host.getFriendPresence().getFriend().isAnonymous()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFriend(Collection<RemoteHost> people) {
        for (RemoteHost host : people) {
            if (!host.getFriendPresence().getFriend().isAnonymous()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @param key - null to not cache browse
     */
    private void browse(BrowseSearch search, SearchInfo searchInfo, String title, String key) {  
        
        SearchResultsPanel searchPanel = browsePanelFactory.createBrowsePanel(search, searchInfo);
        // Add search results display to the UI, and select its navigation item.
        SearchNavItem item = searchNavigator.get().addSearch(title, searchPanel, search, searchPanel.getModel());
        item.select();
        searchPanel.getModel().start(new SwingSearchListener(searchPanel.getModel(), searchPanel, item));
        searchPanel.setBrowseTitle(title);
        
        if (key != null) {
            browseNavItemCache.put(key, item);
            item.addNavItemListener(new ItemRemovalListener(key));
        }
        
    }
    
    /** 
     * @return true if the tab exists
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
        public void itemRemoved() {
            browseNavItemCache.remove(key);
        }

        @Override
        public void itemSelected(boolean selected) {
            //do nothing            
        }
        
    }

}
