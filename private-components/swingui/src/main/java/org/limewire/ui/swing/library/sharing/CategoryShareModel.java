package org.limewire.ui.swing.library.sharing;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.SwingSafePropertyChangeSupport;

public class CategoryShareModel implements LibraryShareModel{

    private Category category;
    private ShareListManager shareListManager;
    
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    
    public CategoryShareModel(ShareListManager shareListManager){
        this.shareListManager = shareListManager;
    }

    public void setCategory(Category category) {
        Category oldCategory = this.category;
        this.category = category;
        support.firePropertyChange("category", oldCategory, category);
    }
 
    @Override
    public void shareFriend(SharingTarget friend) {

        switch (category) {
        case AUDIO:
            shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewAudioAlways(true);
            break;
        case VIDEO:
            shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewVideoAlways(true);
            break;
        case IMAGE:
            shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewImageAlways(true);
            break;
        default:
            throw new IllegalStateException("Can not share " + category + " collection");
        }
    }

    @Override
    public void unshareFriend(SharingTarget friend) {

        switch (category) {
        case AUDIO:
            shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewAudioAlways(false);
            break;
        case VIDEO:
            shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewVideoAlways(false);
            break;
        case IMAGE:
            shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewImageAlways(false);
            break;
        default:
            throw new IllegalStateException("Can not share " + category + " collection");
        }
    }

    public boolean isShared(SharingTarget friend) {
        switch (category) {
        case AUDIO:
            return shareListManager.getOrCreateFriendShareList(friend.getFriend()).isAddNewAudioAlways();
        case VIDEO:
            return shareListManager.getOrCreateFriendShareList(friend.getFriend()).isAddNewVideoAlways();
        case IMAGE:
            return shareListManager.getOrCreateFriendShareList(friend.getFriend()).isAddNewImageAlways();
        default:
            throw new IllegalStateException("Can not share " + category + " collection");
        }
    }


    @Override
    public boolean isGnutellaNetworkSharable() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener){
        support.removePropertyChangeListener(listener);
    }
}
