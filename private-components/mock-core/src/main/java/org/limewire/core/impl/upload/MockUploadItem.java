package org.limewire.core.impl.upload;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.listener.SwingSafePropertyChangeSupport;

public class MockUploadItem implements UploadItem {
    
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    private UploadState state;
    private String fileName;
    private long fileSize;
    private long amtUploaded;
    private Category category;
    
    public MockUploadItem(UploadState state, String fileName, long fileSize, long amtUploaded, Category category){
        this.state = state;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.amtUploaded = amtUploaded;
        this.category = category;
    }
    
    @Override
    public void cancel() {
        setState(UploadState.CANCELED);
    }

    @Override
    public File getFile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public UploadState getState() {
        return state;
    }

    @Override
    public long getTotalAmountUploaded() {
        return amtUploaded;
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener){
        support.removePropertyChangeListener(listener);
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
    
    private void setState(UploadState newState){
        UploadState oldState = state;
        state = newState;
        support.firePropertyChange("state", oldState, state);
    }
    
    @Override
    public Category getCategory(){
        return category;
    }

    @Override
    public String getHost() {
        return "Carmine";
    }

}
