package org.limewire.ui.swing.library;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;

public class SharingMatchingEditor extends AbstractMatcherEditor<LocalFileItem> {

    private EventList<LocalFileItem> friendList;
    private FriendMatcher matcher = new FriendMatcher();
    
    public void setFriendList(EventList<LocalFileItem> friendList) {
//        if(this.friendList != null)
//            this.friendList.removeListEventListener(this);
        this.friendList = friendList;
//        if(friendList != null)
//            this.friendList.addListEventListener(this);
        
        matcher.setFriendList(friendList);
        
        if(friendList == null)
            fireMatchAll();
        else
            fireChanged(matcher);
    }
    
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
}
