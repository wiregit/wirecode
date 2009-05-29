package org.limewire.ui.swing.search.model;

import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.NotImplementedException;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class BrowseSearch implements Search {
    private final CopyOnWriteArrayList<SearchListener> searchListeners = new CopyOnWriteArrayList<SearchListener>();
    
    private final FriendPresence friendPresence;
    private final RemoteLibraryManager remoteLibraryManager;
    private final BrowseFactory browseFactory;
    private Browse browse;

    public BrowseSearch(RemoteLibraryManager remoteLibraryManager, BrowseFactory browseFactory, FriendPresence friendPresence){
        this.friendPresence = friendPresence;
        this.browseFactory = browseFactory;
        this.remoteLibraryManager = remoteLibraryManager;
    }

    @Override
    public void addSearchListener(SearchListener searchListener) {
        searchListeners.add(searchListener);
    }
    
    @Override
    public void removeSearchListener(SearchListener searchListener) {
        searchListeners.remove(searchListener);
    }

    @Override
    public SearchCategory getCategory() {
        return SearchCategory.ALL;
    }

       @Override
    public void repeat() {
        throw new NotImplementedException("BrowseSearch.repeat() not implemented");

    }

    @Override
    public void start() {      
        if (isAnonymousBrowse()){
            startAnonymousBrowse();
        } else {
            //TODO: RemoteFileItems are going away
            EventList<RemoteFileItem> remoteFileItems = new BasicEventList<RemoteFileItem>();

            if (friendPresence != null) {
                PresenceLibrary presenceLibrary = remoteLibraryManager.addPresenceLibrary(friendPresence);
                remoteFileItems.addAll(presenceLibrary.getModel());
            } else {
                remoteFileItems.addAll(remoteLibraryManager.getAllFriendsFileList().getModel());

            }

            for (RemoteFileItem item : remoteFileItems) {
                for (SearchListener listener : searchListeners) {
                    listener.handleSearchResult(this, item.getSearchResult());
                }
            }
        }
    }
    
    private void startAnonymousBrowse() {
        System.out.println(friendPresence.getPresenceId());
        browse = browseFactory.createBrowse(friendPresence);
        browse.start(new BrowseListener() {
            @Override
            public void browseFinished(boolean success) {
                //TODO some kind of browse state based on this
                System.out.println("browsefinished "+success);
            }

            @Override
            public void handleBrowseResult(final SearchResult searchResult) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("handle " + searchResult.getFileName());
                        for (SearchListener listener : searchListeners) {
                            listener.handleSearchResult(BrowseSearch.this, searchResult);
                        }
                    }
                });
            }
        });
    }
    

    private void stopAnonymousBrowse() {
        assert (browse != null);
        browse.stop();
    }
    
    private boolean isAnonymousBrowse(){
        return friendPresence != null && friendPresence.getFriend().isAnonymous();
    }

    @Override
    public void stop() {
        if(isAnonymousBrowse()){
            stopAnonymousBrowse();
        }
    }

}
