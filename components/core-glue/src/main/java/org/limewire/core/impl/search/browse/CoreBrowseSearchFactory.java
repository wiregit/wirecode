package org.limewire.core.impl.search.browse;

import java.util.Collection;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.friend.api.FriendPresence;
import org.limewire.inject.LazySingleton;

import com.google.inject.Inject;

@LazySingleton
class CoreBrowseSearchFactory implements BrowseSearchFactory {
    
    private final BrowseFactory browseFactory;

    @Inject
    public CoreBrowseSearchFactory(BrowseFactory browseFactory) {
        this.browseFactory = browseFactory;
    }
    
    @Override
    public BrowseSearch createBrowseSearch(FriendPresence presence){
        assert(presence != null);
        return new AnonymousSingleBrowseSearch(browseFactory, presence);
    }
    
    @Override
    public BrowseSearch createBrowseSearch(Collection<FriendPresence> presences) {
        return new MultipleBrowseSearch(this, presences);
    }
}
