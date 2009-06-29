package org.limewire.core.impl.search.browse;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.friend.api.Friend;
import org.limewire.inject.LazySingleton;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@LazySingleton
class CoreBrowseSearchFactory implements BrowseSearchFactory {
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final BrowseFactory browseFactory;
    private final ExecutorService backgroundExecutor;

    @Inject
    public CoreBrowseSearchFactory(RemoteLibraryManager remoteLibraryManager,
            BrowseFactory browseFactory,
            @Named("backgroundExecutor") ExecutorService backgroundExecutor) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.browseFactory = browseFactory;
        this.backgroundExecutor = backgroundExecutor;
    }
    
    /**
     * @param friend The friend to browse.  Can not be anonymous or null.
     */
    public BrowseSearch createFriendBrowseSearch(Friend friend){
        assert(friend != null && !friend.isAnonymous());
        return new FriendSingleBrowseSearch(remoteLibraryManager, friend, backgroundExecutor);
    }
    
    /**
     * 
     * @param person The host to browse.  Can be a friend or anonymous.  Can not be null.
     */
    public BrowseSearch createBrowseSearch(RemoteHost person){
        assert(person != null);
        if(person.getFriendPresence().getFriend().isAnonymous()){
            return new AnonymousSingleBrowseSearch(browseFactory, person.getFriendPresence());
        } else {
            return createFriendBrowseSearch(person.getFriendPresence().getFriend());
        }        
    }
    
    public BrowseSearch createBrowseSearch(Collection<RemoteHost> people){
        return new MultipleBrowseSearch(this, people);
    }
    
    public BrowseSearch createAllFriendsBrowseSearch(){
        return new AllFriendsBrowseSearch(remoteLibraryManager, backgroundExecutor);
    }
   
    
}
