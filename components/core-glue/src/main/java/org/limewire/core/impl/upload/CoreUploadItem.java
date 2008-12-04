package org.limewire.core.impl.upload;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.upload.UploadErrorState;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.impl.URNImpl;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.listener.SwingSafePropertyChangeSupport;

import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.library.FileDesc;

class CoreUploadItem implements UploadItem {

    private Uploader uploader;

    private boolean isStopped = false;

    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    
    private Map<FilePropertyKey, Object> propertiesMap;
    
    public final static long UNKNOWN_TIME = Long.MAX_VALUE;

    public CoreUploadItem(Uploader uploader) {
        this.uploader = uploader;
    }

    @Override
    public void cancel() {
        uploader.stop();
        isStopped = true;
        fireDataChanged();
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
        if (isStopped) {
            return UploadState.CANCELED;
        }

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
        // equal if Uploaders are equal
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
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    void fireDataChanged() {
        support.firePropertyChange("state", null, getState());
    }

    @Override
    public Category getCategory() {
        return CategoryConverter.categoryForFile(getFile());
    }

    @Override
    public String getHost() {
        return uploader.getHost();
    }
    
    @Override
    public String toString(){
        return "CoreUploadItem: " + getFileName() + ", " + getState() + ", " + getHost();
    }

    @Override
    public int getQueuePosition() {
        return uploader.getQueuePosition();
    }
    
    @Override
    public float getUploadSpeed() {
        try {
            uploader.measureBandwidth();
            return uploader.getMeasuredBandwidth();
        } catch (InsufficientDataException e) {
            return 0;
        }
    }
    
    @Override
    public long getRemainingUploadTime() {
        double remaining = (getFileSize() - getTotalAmountUploaded()) / 1024.0;
        float speed = getUploadSpeed();
        if (speed > 0) {
            return (long) (remaining / speed);
        } else {
            return UNKNOWN_TIME;
        }

    }
    
    @Override
    public UploadErrorState getErrorState() {
        switch (uploader.getState()) {
        case LIMIT_REACHED:
        case BANNED_GREEDY:
        case FREELOADER:
            return UploadErrorState.LIMIT_REACHED;
        case INTERRUPTED:
        case SUSPENDED:
        case WAITING_REQUESTS:
            return UploadErrorState.INTERRUPTED;
        case FILE_NOT_FOUND:
        case MALFORMED_REQUEST:
        case UNAVAILABLE_RANGE:
            return UploadErrorState.FILE_ERROR;       
        }
        throw new IllegalStateException("Non-error UploaderState: " + uploader.getState());
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return getPropertiesMap().get(key);
    }

    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            String stringValue = value.toString();
            return stringValue;
        } else {
            return null;
        }
    }

    @Override
    public URN getUrn() {
        com.limegroup.gnutella.URN urn = uploader.getFileDesc().getSHA1Urn();
        if(urn != null) {
            return new URNImpl(urn);
        }
        return null;
    }
    
    /**
     * Lazily builds the properties map for this local file item. Uses double
     * checked locking to prevent multiple threads from creating this map.
     */
    private Map<FilePropertyKey, Object> getPropertiesMap() {        
        if (propertiesMap == null) {
            synchronized (this) {
                if (propertiesMap == null) {
                    reloadProperties();
                }
            }
        }
        return propertiesMap;
    }
    
    /**
     * Reloads the properties map to whatever values are stored in the
     * LimeXmlDocs for this file.
     */
    private void reloadProperties() {
        synchronized (this) {
            Map<FilePropertyKey, Object> reloadedMap = Collections
                    .synchronizedMap(new HashMap<FilePropertyKey, Object>());
            FileDesc fileDesc = uploader.getFileDesc();
            FilePropertyKeyPopulator.populateProperties(fileDesc.getFileName(), fileDesc.getFile()
                    .lastModified(), fileDesc.getFileSize(), reloadedMap, fileDesc.getXMLDocument());
            propertiesMap = reloadedMap;
        }
    }
}
