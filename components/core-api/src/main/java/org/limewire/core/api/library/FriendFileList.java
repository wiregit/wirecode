package org.limewire.core.api.library;

public interface FriendFileList extends LocalFileList {

    public boolean isAddNewAudioAlways();
    
    public void setAddNewAudioAlways(boolean value);
    
    public boolean isAddNewVideoAlways();
    
    public void setAddNewVideoAlways(boolean value);
    
    public boolean isAddNewImageAlways();
    
    public void setAddNewImageAlways(boolean value);
}
