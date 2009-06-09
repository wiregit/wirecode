package org.limewire.ui.swing.search.model.browse;

import java.util.Arrays;
import java.util.Collection;

import org.limewire.core.api.friend.Friend;

public class BrowseStatus {
    
    public enum BrowseState {
        LOADED, FAILED, PARTIAL_FAIL, UPDATED, LOADING, UPDATED_PARTIAL_FAIL
    }    
    
    private final BrowseState state;
    private final Friend[] failedFriends;
    private final BrowseSearch search;
    
    public BrowseStatus(BrowseSearch search, BrowseState state, Friend... failedFriends){
        this.search = search;
        this.state = state;
        this.failedFriends = failedFriends;
    }
    
    public BrowseState getState(){
        return state;
    }
    
    public Collection<Friend> getFailed(){
        return Arrays.asList(failedFriends);
    }

    public BrowseSearch getBrowseSearch() {
        return search;
    }
}
