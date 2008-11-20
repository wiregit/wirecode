package org.limewire.ui.swing.library.sharing;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.listener.SwingSafePropertyChangeSupport;

public class FileShareModel implements LibraryShareModel {
    private LocalFileItem fileItem;

    private LocalFileList gnutellaList;

    private ShareListManager shareListManager;
    
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);

    public FileShareModel(ShareListManager shareListManager) {
        this.shareListManager = shareListManager;
        gnutellaList = shareListManager.getGnutellaShareList();
    }   
    
    @Override
    public void shareFriend(SharingTarget friend) {        
        if (friend.isGnutellaNetwork()) {
            gnutellaList.addFile(fileItem.getFile());
        } else {
            shareListManager.getOrCreateFriendShareList(friend.getFriend()).addFile(fileItem.getFile());
        }
    }
    
    @Override
    public void unshareFriend(SharingTarget friend) {
        if (friend.isGnutellaNetwork()) {
            gnutellaList.removeFile(fileItem.getFile());
        } else {
            //TODO: need to handle share all settings here
            shareListManager.getOrCreateFriendShareList(friend.getFriend()).removeFile(fileItem.getFile());
        }
    }

    /**
     * @param fileItem  The LocalFileItem whose sharing info will be displayed
     */
    public void setFileItem(LocalFileItem fileItem){
        LocalFileItem oldFileItem = this.fileItem;
        this.fileItem = fileItem;
        support.firePropertyChange("fileItem", oldFileItem, fileItem);
    }  
    
    @Override
    public boolean isGnutellaNetworkSharable() {
        return (fileItem.getCategory() != Category.DOCUMENT || LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue());
    }
 
    @Override
    public boolean isShared(SharingTarget friend){
        //Handle gnutella 
        if(friend.isGnutellaNetwork()){
            return gnutellaList.contains(fileItem.getFile());
        }
        
        //check for share all settings
        switch (fileItem.getCategory()) {
        case AUDIO:
            if (shareListManager.getOrCreateFriendShareList(friend.getFriend()).isAddNewAudioAlways()){
                return true;
                }
            break;            
        case VIDEO:
            if (shareListManager.getOrCreateFriendShareList(friend.getFriend()).isAddNewVideoAlways()){
                return true;
                }
            break; 
        case IMAGE:
            if (shareListManager.getOrCreateFriendShareList(friend.getFriend()).isAddNewImageAlways()){
                return true;
                }
            break; 
        }
        
        //check individual file share settings
        return shareListManager.getOrCreateFriendShareList(friend.getFriend()).contains(fileItem.getFile());
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
