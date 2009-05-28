package org.limewire.ui.swing.search;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.friends.chat.ChatFrame;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.model.BrowseSearchResultsModel;
import org.limewire.ui.swing.search.model.SearchResultsModelFactory;
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


    @Inject
    public RemoteHostActionsImpl(ChatFrame chatFrame, SearchResultsModelFactory searchResultsModelFactory, 
            SearchResultsPanelFactory searchResultsPanelFactory, Provider<SearchNavigator> searchNavigator,
            LibraryNavigator libraryNavigator, Navigator navigator) {
        this.chatFrame = chatFrame;
        this.searchResultsModelFactory = searchResultsModelFactory;
        this.searchResultsPanelFactory = searchResultsPanelFactory;
        this.searchNavigator = searchNavigator;
        this.libraryNavigator = libraryNavigator; 
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
        BrowseSearchResultsModel searchModel = searchResultsModelFactory.createSearchResultsModel(person.getFriendPresence());
        final SearchResultsPanel searchPanel = searchResultsPanelFactory.createSearchResultsPanel(searchModel);

        // Add search results display to the UI, and select its navigation item.
        SearchNavItem item = searchNavigator.get().addSearch(
                I18n.tr("Browse: {0}", person.getFriendPresence().getFriend().getRenderName()), searchPanel, new EmptySearch());
        item.select();

        searchModel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (evt.getNewValue() instanceof LibraryState)
                    searchPanel.setBrowseState((LibraryState) evt.getNewValue());
            }
        });
        searchPanel.setBrowseState(searchModel.getLibraryState());
    }
    
    private static class EmptySearch implements Search {

        @Override
        public void addSearchListener(SearchListener searchListener) {
        }

        @Override
        public void removeSearchListener(SearchListener searchListener) {
        }

        @Override
        public SearchCategory getCategory() {
            return SearchCategory.ALL;
        }

        @Override
        public void repeat() {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    }
}
