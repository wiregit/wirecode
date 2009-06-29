package org.limewire.ui.swing.search;

import java.util.Collection;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.friend.api.Friend;
import org.limewire.inject.LazySingleton;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.friends.chat.ChatFrame;
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
        browse(browseSearchFactory.get().createBrowseSearch(person), 
                DefaultSearchInfo.createBrowseSearch(SearchType.SINGLE_BROWSE),
                person.getFriendPresence().getFriend().getRenderName());
    }
    
    @Override
    public void viewLibraryOf(Friend friend) {
        assert(friend != null && !friend.isAnonymous());
        LOG.debugf("viewLibraryOf: {0}", friend);
        browse(browseSearchFactory.get().createFriendBrowseSearch(friend), 
                DefaultSearchInfo.createBrowseSearch(SearchType.SINGLE_BROWSE),
                friend.getRenderName());
    }


    @Override
    public void viewLibrariesOf(Collection<RemoteHost> people) {
         browse(browseSearchFactory.get().createBrowseSearch(people),
                 DefaultSearchInfo.createBrowseSearch(SearchType.MULTIPLE_BROWSE),
                 getTabTitle(people));
    }
    
    @Override
    public void browseAllFriends() {
         browse(browseSearchFactory.get().createAllFriendsBrowseSearch(),
                 DefaultSearchInfo.createBrowseSearch(SearchType.ALL_FRIENDS_BROWSE),
                 I18n.tr("All Friends"));
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
    
    private void browse(BrowseSearch search, SearchInfo searchInfo, String title) {       
        SearchResultsPanel searchPanel = browsePanelFactory.createBrowsePanel(search, searchInfo);
        // Add search results display to the UI, and select its navigation item.
        SearchNavItem item = searchNavigator.get().addSearch(title, searchPanel, search, searchPanel.getModel());
        item.select();
        searchPanel.getModel().start(new SwingSearchListener(searchPanel.getModel(), searchPanel, item));
        searchPanel.setBrowseTitle(title);
    }

}
