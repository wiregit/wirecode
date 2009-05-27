package org.limewire.core.impl.library;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.util.NotImplementedException;

import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.FileViewChangeEvent.Type;

public class SimpleFriendFileListImpl extends LocalFileListImpl implements FriendFileList {

    
    private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this); 
    private final FileCollection gnutellaCollection;
    private final FileView gnutellaView;
    

    public SimpleFriendFileListImpl(CoreLocalFileItemFactory coreLocalFileItemFactory,
            FileCollection gnutellaFileCollection, FileView gnutellaFileView,
            CombinedShareList combinedShareList) {
        super(combinedShareList.createMemberList(), coreLocalFileItemFactory);
        this.gnutellaCollection = gnutellaFileCollection;
        this.gnutellaView = gnutellaFileView;
        this.gnutellaView.addListener(newEventListener());
        combinedShareList.addMemberList(baseList);
    }
    
    @Override
    protected FileCollection getMutableCollection() {
        return gnutellaCollection;
    }
    
    @Override
    protected FileView getFileView() {
        return gnutellaView;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    @Override
    public void clearCategory(Category category) {
        throw new NotImplementedException();
    }

    @Override
    public void addSnapshotCategory(Category category) {
        throw new NotImplementedException();
    }
    
    @Override
    public boolean isCategoryAutomaticallyAdded(Category category) {
        return false;
    }
    
    @Override
    public void setCategoryAutomaticallyAdded(Category category, boolean added) {
        throw new NotImplementedException();
    }
    
    
    @Override
    protected void collectionUpdate(Type type, boolean shared) {
        throw new NotImplementedException();
    }
}