package org.limewire.core.impl.upload;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.listener.SwingSafePropertyChangeSupport;

public class MockUploadItem implements UploadItem {
    
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    
    @Override
    public void cancel() {
        // TODO Auto-generated method stub

    }

    @Override
    public File getFile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFileName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getFileSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public UploadState getState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getTotalAmountUploaded() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener){
        support.removePropertyChangeListener(listener);
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

}
