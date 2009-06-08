package org.limewire.ui.swing.search;

import java.util.Collection;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.friends.chat.ChatFrame;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.model.BrowseSearch;
import org.limewire.ui.swing.search.model.BrowseStatusListener;
import org.limewire.ui.swing.search.model.MultipleBrowseSearch;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.SearchResultsModelFactory;
import org.limewire.ui.swing.search.model.SingleBrowseSearch;
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


    private final RemoteLibraryManager remoteLibraryManager;
    private final BrowseFactory browseFactory;


    @Inject
    public RemoteHostActionsImpl(ChatFrame chatFrame, SearchResultsModelFactory searchResultsModelFactory, 
            SearchResultsPanelFactory searchResultsPanelFactory, Provider<SearchNavigator> searchNavigator,
            LibraryNavigator libraryNavigator, Navigator navigator, RemoteLibraryManager remoteLibraryManager,
            BrowseFactory browseFactory) {
        this.chatFrame = chatFrame;
        this.searchResultsModelFactory = searchResultsModelFactory;
        this.searchResultsPanelFactory = searchResultsPanelFactory;
        this.searchNavigator = searchNavigator;
        this.libraryNavigator = libraryNavigator; 
        this.remoteLibraryManager = remoteLibraryManager;
        this.browseFactory = browseFactory;
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
        LOG.debugf("viewLibraryOf: {0}", person.getFriendPresence().getFriend());
        // Create search results data model and display panel.
        
        browse(new SingleBrowseSearch(remoteLibraryManager, browseFactory, person.getFriendPresence()),
                I18n.tr("Browse: {0}", person.getFriendPresence().getFriend().getRenderName()));
    }


    @Override
    public void viewLibrariesOf(Collection<RemoteHost> people) {
         browse(new MultipleBrowseSearch(remoteLibraryManager, browseFactory, people),
                 I18n.trn("Browse: {0} person", "Browse: {0} people", people.size()));
    }
    
    private void browse(BrowseSearch search, String title){
        //TODO: better SearchInfo
        SearchInfo searchInfo = DefaultSearchInfo.createKeywordSearch("this is a browse", SearchCategory.ALL);
        
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
        //TODO: better title
        SearchNavItem item = searchNavigator.get().addSearch(title, searchPanel, search);
        item.select();
        searchModel.start(new SwingSearchListener(searchModel, searchPanel, item));
    }

}
