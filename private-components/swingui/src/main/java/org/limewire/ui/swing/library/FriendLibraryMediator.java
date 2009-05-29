package org.limewire.ui.swing.library;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.library.nav.NavMediator;

import com.google.inject.Inject;

public class FriendLibraryMediator implements NavMediator<JComponent> {
    
    /** Friend that is currently selected, null if all files are being shown*/
    private Friend currentFriend;
    
    private final JLabel label = new JLabel("friend panel");
    
    @Inject
    public FriendLibraryMediator() {
    }
    
    public Friend getSelectedFriend() {
         return currentFriend;
    }
    
    public void setFriend(Friend friend) {
        currentFriend = friend;
    }
    
    @Override
    public JComponent getComponent() {
        return label;
    }
    
    /**
     * Adds a listener that is notified when the friend in the current view has changed.
     */
    public void addFriendListener(ListSourceChanger.ListChangedListener listener) {
//        currentFriendFilterChanger.addListener(listener);
    }
}
