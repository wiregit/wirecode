package org.limewire.ui.swing.library;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * Performs the filtering on My Library. A FriendFileList can be set on the matcher.
 * This fileList will be used to filter down content in My Library. If a file is contained
 * in both lists, the file will be displayed, otherwise it will not be displayed. 
 *
 * If not FriendFileList is set, all files will be shown.
 */
public class SharingMatchingEditor extends AbstractMatcherEditor<LocalFileItem> implements ListEventListener<LocalFileItem> {

    private EventList<LocalFileItem> friendList;
    private FriendMatcher matcher = new FriendMatcher();
    private Friend currentFriend;
    
    private final ShareListManager shareListManager;
    
    public SharingMatchingEditor(ShareListManager shareListManager) {
        this.shareListManager = shareListManager;
    }
    
    /**
	 * Set the current friend to filter with. If no filtering is
	 * to occur, set friend to null.
	 */
    public void setFriend(Friend friend) {
        currentFriend = friend;
        
        if(friend == null || friend.getId() == null) 
            setFriendList(null);
        else if(friend.getId().equals(Friend.P2P_FRIEND_ID))
            setFriendList(shareListManager.getGnutellaShareList().getModel());
        else
            setFriendList(shareListManager.getFriendShareList(friend).getModel());
    }
    
    /**
	 * Return the current friend that is being filtered on, null if no filtering is
     * occuring.
	 */
    public Friend getCurrentFriend() {
        return currentFriend;
    }
    
    private void setFriendList(EventList<LocalFileItem> friendList) {
        if(this.friendList != null)
            this.friendList.removeListEventListener(this);
        this.friendList = friendList;
        if(friendList != null)
            this.friendList.addListEventListener(this);
        
        matcher.setFriendList(friendList);
        
        //when changing lists, notify the EventList that a new 
       	//filter is being used
        if(friendList == null)
            fireMatchAll();
        else
            fireChanged(matcher);
    }
    
    /**
	 * Returns the current EventList that is being used to filter with.
	 */
    public EventList<LocalFileItem> getCurrentFilter() {
        return friendList;
    }
    
    /**
	 * Performs the actual filtering on the main EventList. Returns true
	 * if a LocalFileItem should be displayed, false otherwise.
	 */
    private class FriendMatcher implements Matcher<LocalFileItem> {

        private EventList<LocalFileItem> friendList;
        
        public void setFriendList(EventList<LocalFileItem> friendList) {
            this.friendList = friendList;
        }
        
        @Override
        public boolean matches(LocalFileItem item) {
            if(friendList == null)
                return true;
            else 
                return friendList.contains(item);
        }
    }

    /**
     *  When the friend filterlist is updated, notify the matcher of changes.
     */
    @Override
    public void listChanged(ListEvent<LocalFileItem> listChanges) {
        fireChanged(matcher);
    }
}
