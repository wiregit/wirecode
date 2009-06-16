package org.limewire.core.api.search.browse;

import java.util.Arrays;
import java.util.List;

import org.limewire.core.api.friend.Friend;

public class BrowseStatus {
    
    public enum BrowseState {
        LOADED, FAILED, PARTIAL_FAIL, UPDATED, LOADING, UPDATED_PARTIAL_FAIL
    }    
    
    private final BrowseState state;
    private final List<Friend> failed;
    private final BrowseSearch search;
    
    public BrowseStatus(BrowseSearch search, BrowseState state, Friend... failedFriends){
        this.search = search;
        this.state = state;
        this.failed = Arrays.asList(failedFriends);
    }
    
    public BrowseState getState(){
        return state;
    }
    
    public List<Friend> getFailedFriends(){
        return failed;
    }

    public BrowseSearch getBrowseSearch() {
        return search;
    }
}
