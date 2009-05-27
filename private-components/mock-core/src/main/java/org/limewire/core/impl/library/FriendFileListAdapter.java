package org.limewire.core.impl.library;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FriendFileList;

public class FriendFileListAdapter extends FileListAdapter implements FriendFileList {

   
    @Override
    public boolean isCategoryAutomaticallyAdded(Category category) {
        return false;
    }
    
    @Override
    public void setCategoryAutomaticallyAdded(Category category, boolean added) {
    }
    
    @Override
    public void clearCategory(Category category) {

    }

    @Override
    public void addSnapshotCategory(Category category) {

    }
}
