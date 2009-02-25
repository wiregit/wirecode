package org.limewire.ui.swing.library;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
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

    private FriendFileList friendList;
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
            setFriendList(shareListManager.getGnutellaShareList());
        else
            setFriendList(shareListManager.getFriendShareList(friend));
    }
    
    /**
	 * Return the current friend that is being filtered on, null if no filtering is
     * occuring.
	 */
    public Friend getCurrentFriend() {
        return currentFriend;
    }
    
    private void setFriendList(FriendFileList friendList) {
        if(this.friendList != null)
            this.friendList.getSwingModel().removeListEventListener(this);
        this.friendList = friendList;
        if(friendList != null)
            this.friendList.getSwingModel().addListEventListener(this);
        
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
        if(friendList == null)
            return null;
        return friendList.getSwingModel();
    }
    
    /**
	 * Performs the actual filtering on the main EventList. Returns true
	 * if a LocalFileItem should be displayed, false otherwise.
	 */
    private class FriendMatcher implements Matcher<LocalFileItem> {

        private FriendFileList friendList;
        
        public void setFriendList(FriendFileList friendList) {
            this.friendList = friendList;
        }
        
        @Override
        public boolean matches(LocalFileItem item) {
            if(friendList == null)
                return true;
            else 
                return friendList.contains(item.getFile());
        }
    }

    /**
     *  When the friend filterlist is updated, notify the matcher of changes.
     *  
     *  Its important here to selectively fire the appropriate filter notification.
     *  Using constrained/relaxed increases the speed of the matcher updateer.
     */
    @Override
    public void listChanged(ListEvent<LocalFileItem> listChanges) {
        while(listChanges.next()) {
            // remove an item
            if(listChanges.getType() == ListEvent.DELETE) {
                fireConstrained(matcher);
            } else { //add or update item
                fireRelaxed(matcher);
            }
        }
    }
}
