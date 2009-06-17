package org.limewire.ui.swing.library.sharing;

import org.limewire.core.api.friend.Friend;

class EditableSharingData {

    private final Friend friend;
    private boolean isSelected;
    
    public EditableSharingData(Friend friend, boolean isSelected) {
        this.friend = friend;
        this.isSelected = isSelected;
    }
    
    public String getName() {
        return friend.getRenderName();
    }
    
    public String getId() {
        return friend.getId();
    }
    
    public boolean isSelected() {
        return isSelected;
    }
    
    public void setIsSelected(boolean value) {
        this.isSelected = value;
    }
}
