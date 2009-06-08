package org.limewire.ui.swing.search;

import java.util.Collection;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.friends.chat.ChatFrame;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.model.BrowseStatusListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.SearchResultsModelFactory;
import org.limewire.ui.swing.search.model.browse.BrowseSearch;
import org.limewire.ui.swing.search.model.browse.BrowseSearchFactory;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RemoteHostActionsImpl implements RemoteHostActions {
    private static final Log LOG = LogFactory.getLog(RemoteHostActionsImpl.class);


    private final ChatFrame chatFrame;
    private final LibraryNavigator libraryNavigator;

    private final SearchResultsModelFactory searchResultsModelFactory;

    private final SearchResultsPanelFactory searchResultsPanelFactory;

    //Provider prevents circular dependency
    private final Provider<SearchNavigator> searchNavigator;


    private BrowseSearchFactory browseSearchFactory;


    @Inject
    public RemoteHostActionsImpl(ChatFrame chatFrame, SearchResultsModelFactory searchResultsModelFactory, 
            SearchResultsPanelFactory searchResultsPanelFactory, Provider<SearchNavigator> searchNavigator,
            LibraryNavigator libraryNavigator, Navigator navigator, BrowseSearchFactory browseSearchFactory) {
        this.chatFrame = chatFrame;
        this.searchResultsModelFactory = searchResultsModelFactory;
        this.searchResultsPanelFactory = searchResultsPanelFactory;
        this.searchNavigator = searchNavigator;
        this.libraryNavigator = libraryNavigator; 
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
    public void showFilesSharedWith(RemoteHost person) {
        LOG.debugf("showFilesSharedWith: {0}", person.getFriendPresence().getFriend());
        Friend friend = person.getFriendPresence().getFriend();
        libraryNavigator.selectFriendShareList(friend);
    }

    
    @Override
    public void viewLibraryOf(RemoteHost person) {
        assert(person != null);
        LOG.debugf("viewLibraryOf: {0}", person);
        browse(browseSearchFactory.createBrowseSearch(person),
                person.getFriendPresence().getFriend().getRenderName(), 
                I18n.tr("Browse {0}", person.getFriendPresence().getFriend().getRenderName()));
    }
    
    @Override
    public void viewLibraryOf(Friend friend) {
        assert(friend != null && !friend.isAnonymous());
        LOG.debugf("viewLibraryOf: {0}", friend);
        browse(browseSearchFactory.createFriendBrowseSearch(friend), friend.getRenderName(), 
                I18n.tr("Browse {0}", friend.getRenderName()));
    }


    @Override
    public void viewLibrariesOf(Collection<RemoteHost> people) {
         browse(browseSearchFactory.createBrowseSearch(people),
                 getTabTitle(people), getPanelTitle(people));
    }
    
    private String getTabTitle(Collection<RemoteHost> people){
        boolean hasP2P = false;
        boolean hasFriends = false;
        
        //see what the collection contains
        for (RemoteHost host : people){
            if(host.getFriendPresence().getFriend().isAnonymous()){
                hasP2P = true;
            } else {
                hasFriends = true;
            }
            if (hasP2P && hasFriends){
                //we are done checking if both are true
                break;
            }
        }
        
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
    
    private String getPanelTitle(Collection<RemoteHost> people){
        boolean hasP2P = false;
        boolean hasFriends = false;
        
        //see what the collection contains
        for (RemoteHost host : people){
            if(host.getFriendPresence().getFriend().isAnonymous()){
                hasP2P = true;
            } else {
                hasFriends = true;
            }
            if (hasP2P && hasFriends){
                //we are done checking if both are true
                break;
            }
        }
        
        if (hasP2P && hasFriends){
            return I18n.trn("Browse {0} Person", "Browse {0} People", people.size());
        } else if (hasP2P){
            return I18n.trn("Browse {0} P2P User", "Browse {0} P2P Users", people.size());            
        } else if (hasFriends){
            return I18n.trn("Browse {0} Friend", "Browse {0} Friends", people.size());            
        }
        
        //empty list
        return "";
    }
   
    
    private void browse(BrowseSearch search, String title, String panelTitle){
        //TODO: better SearchInfo
        SearchInfo searchInfo = DefaultSearchInfo.createKeywordSearch("", SearchCategory.ALL);
        
        SearchResultsModel searchModel = searchResultsModelFactory.createSearchResultsModel(searchInfo, search);
        final SearchResultsPanel searchPanel = searchResultsPanelFactory.createSearchResultsPanel(searchModel);
        
        
        search.addBrowseStatusListener(new BrowseStatusListener(){
            @Override
            public void statusChanged() {
                // TODO change status of panel
                //searchPanel.setBrowseStatus();
            }            
        });

        // Add search results display to the UI, and select its navigation item.
        SearchNavItem item = searchNavigator.get().addSearch(title, searchPanel, search);
        item.select();
        searchModel.start(new SwingSearchListener(searchModel, searchPanel, item));
        searchPanel.setTitle(panelTitle);
    }

}
