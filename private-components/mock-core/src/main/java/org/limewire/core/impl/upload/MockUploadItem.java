package org.limewire.core.impl.upload;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.impl.friend.MockFriend;
import org.limewire.core.impl.friend.MockFriendPresence;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.URN;
import org.limewire.listener.SwingSafePropertyChangeSupport;

public class MockUploadItem implements UploadItem {
    
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    private volatile UploadState state;
    private String fileName;
    private long fileSize;
    private volatile long amtUploaded;
    private Category category;
    private RemoteHost uploadRemoteHost;
    private final long startTime;
    private String hostname;
    private volatile boolean running = true;
    
    public MockUploadItem(UploadState state, String fileName, long fileSize, long amtUploaded, Category category, String hostname){
        this.state = state;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.amtUploaded = amtUploaded;
        this.category = category;
        this.hostname = hostname;
        startTime = System.currentTimeMillis();
        
        if (this.state == UploadState.UPLOADING) {
            start();
        }
    }
    
    private boolean isRunning() {
        return running;
    }
    
    private void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning() && getTotalAmountUploaded() < getFileSize()) {
                    setTotalAmountUploaded(getTotalAmountUploaded() + 512);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // eat InterruptedException
                    }
                }
            }
        }).start();
    }
    
    @Override
    public void cancel() {
        setState(UploadState.CANCELED);
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
    
    private void setTotalAmountUploaded(long amtUploaded) {
        long oldAmount = this.amtUploaded;
        this.amtUploaded = (amtUploaded < getFileSize()) ? amtUploaded : getFileSize();
        if (this.amtUploaded == getFileSize()) {
            setState(UploadState.DONE);
        } else {
            support.firePropertyChange("totalAmountUploaded", oldAmount, this.amtUploaded);
        }
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
    public String toString(){
        return "MockUploadItem: " + getFileName() + ", " + getState();
    }

    @Override
    public int getQueuePosition() {
        return 2;
    }

    @Override
    public long getRemainingUploadTime() {
        float speed = getUploadSpeed();
        if (speed > 0) {
            double remaining = (getFileSize() - getTotalAmountUploaded()) / 1024.0;
            return (long) (remaining / speed);
        } else {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public float getUploadSpeed() {
        return 1;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        // TODO Auto-generated method stub
        return "property for " + key;
    }

    @Override
    public String getPropertyString(FilePropertyKey filePropertyKey) {
        // TODO Auto-generated method stub
        return "filePropertyKey for " + filePropertyKey;
    }

    @Override
    public URN getUrn() {
        return new URN() {
            @Override
            public int compareTo(URN o) {
                return toString().compareTo(o.toString());
            }
        };
    }
    
    @Override
    public Collection<File> getCompleteFiles() {
        return Collections.singletonList(new File(fileName));
    }

    @Override
    public File getFile() {
        return new File(fileName);
    }

    @Override
    public UploadItemType getUploadItemType() {
        return UploadItemType.GNUTELLA;
    }

    @Override
    public int getNumUploadConnections() {
        return 0;
    }

    @Override
    public BrowseType getBrowseType() {
        return BrowseType.FRIEND;
    }

    @Override
    public RemoteHost getRemoteHost() {
        if (uploadRemoteHost == null) {
            uploadRemoteHost = new MockUploadRemoteHost();
        }
        return uploadRemoteHost;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }
    
    @Override
    public float getSeedRatio() {
        return -1;
    }

    @Override
    public boolean isFinished() {
        UploadState state = getState();
        return (state == UploadState.DONE || state == UploadState.BROWSE_HOST_DONE);
    }
    
    @Override
    public boolean isStarted() {
        return true;
    }
    
    @Override
    public void pause() {
        
    }

    @Override
    public void resume() {
        
    }
    
    private class MockUploadRemoteHost implements RemoteHost {

        @Override
        public FriendPresence getFriendPresence() {
            return new MockFriendPresence(new MockFriend("uploader"), "uploader");
        }

        @Override
        public boolean isBrowseHostEnabled() {
            return true;
        }

        @Override
        public boolean isChatEnabled() {
            return false;
        }

        @Override
        public boolean isSharingEnabled() {
            return false;
        }
    }

    @Override
    public String getRenderName() {
        return hostname;
    }

    @Override
    public List<SourceInfo> getTransferDetails() {
        return Collections.emptyList();
    }
}
