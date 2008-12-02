package org.limewire.core.impl.upload;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.listener.SwingSafePropertyChangeSupport;

import com.limegroup.gnutella.Uploader;

public class CoreUploadItem implements UploadItem {

    private Uploader uploader;
    
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);

    public CoreUploadItem(Uploader uploader) {
        this.uploader = uploader;
    }

    @Override
    public void cancel() {
        uploader.stop();
    }

    @Override
    public File getFile() {
        return uploader.getFileDesc().getFile();
    }

    @Override
    public String getFileName() {
        return uploader.getFileName();
    }

    @Override
    public long getFileSize() {
        return uploader.getFileSize();
    }

    @Override
    public UploadState getState() {
        switch (uploader.getState()) {
        case COMPLETE:
            return UploadState.DONE;

        case CONNECTING:
        case UPLOADING:
        case THEX_REQUEST:
        case BROWSE_HOST:
        case PUSH_PROXY:
        case UPDATE_FILE:
            return UploadState.UPLOADING;

        case QUEUED:
            return UploadState.QUEUED;

        case LIMIT_REACHED:
        case INTERRUPTED:
        case FILE_NOT_FOUND:
        case UNAVAILABLE_RANGE:
        case MALFORMED_REQUEST:
        case SUSPENDED:
        case WAITING_REQUESTS:
        case BANNED_GREEDY:
        case FREELOADER:
            return UploadState.UNABLE_TO_UPLOAD;

        }
        
        throw new IllegalStateException("Unknown Upload status : " + uploader.getState());
    }

    @Override
    public long getTotalAmountUploaded() {
        return uploader.getTotalAmountUploaded();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uploader == null) ? 0 : uploader.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        //equal if Uploaders are equal
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CoreUploadItem other = (CoreUploadItem) obj;
        if (uploader == null) {
            if (other.uploader != null)
                return false;
        } else if (!uploader.equals(other.uploader))
            return false;
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
    
    void fireDataChanged() {
        support.firePropertyChange("state", null, getState());
    }
}
