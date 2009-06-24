package org.limewire.core.api.search.browse;

import java.util.Arrays;
import java.util.List;

import org.limewire.friend.api.Friend;

public class BrowseStatus {
    
    public enum BrowseState {
        LOADED(true), FAILED(false), PARTIAL_FAIL(true), UPDATED(true), LOADING(true), UPDATED_PARTIAL_FAIL(true), OFFLINE(false), 
        /** The list of files from all friends is empty */
        NO_FRIENDS_SHARING(false) ;
        
        private boolean ok;
        BrowseState(boolean ok){
            this.ok = ok;
        }
        
        /**
         * @return true if any files have loaded or if there is a chance of files loading (including UPDATED), false if
         * the browse has failed or there are no files to load
         */
        public boolean isOK(){
            return ok;
        }
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
