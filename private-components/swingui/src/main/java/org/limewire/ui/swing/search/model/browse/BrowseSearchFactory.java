package org.limewire.ui.swing.search.model.browse;

import java.util.Collection;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.inject.LazySingleton;

import com.google.inject.Inject;

@LazySingleton
public class BrowseSearchFactory {
    
    private RemoteLibraryManager remoteLibraryManager;
    private BrowseFactory browseFactory;

    @Inject
    public BrowseSearchFactory(RemoteLibraryManager remoteLibraryManager,
    BrowseFactory browseFactory) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.browseFactory = browseFactory;
    }
    
    /**
     * @param friend The friend to browse.  Can not be anonymous or null.
     */
    public BrowseSearch createFriendBrowseSearch(Friend friend){
        assert(friend != null && !friend.isAnonymous());
        return new FriendSingleBrowseSearch(remoteLibraryManager, friend);
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
        return new AllFriendsBrowseSearch(remoteLibraryManager);
    }
   
    
}
