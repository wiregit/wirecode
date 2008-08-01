package org.limewire.core.impl.download;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadSource;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.util.QueueTimeCalculator;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;

public class CoreDownloadItem implements DownloadItem {
   
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private Downloader downloader;
    private volatile int hashCode = 0;
    private volatile boolean cancelled = false;
    /**
     * size in bytes.  FINISHING state is only shown for files greater than this size.
     */
    //set to 0 to show FINISHING state regardless of size
    private final long finishingThreshold = 0;
    private QueueTimeCalculator queueTimeCalculator;

    public CoreDownloadItem(Downloader downloader) {
        this(downloader, null);
    }

    public CoreDownloadItem(Downloader downloader, QueueTimeCalculator queueTimeCalculator) {
        this.downloader = downloader;
        this.queueTimeCalculator = queueTimeCalculator;

        downloader.addListener(new EventListener<DownloadStatusEvent>() {
            @Override
            public void handleEvent(DownloadStatusEvent event) {
                // broadcast the status has changed
                support.firePropertyChange("state", null, getState());
            }
        });
    }

    private Downloader getDownloader(){
        return downloader;
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener){
        support.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener){
        support.removePropertyChangeListener(listener);
    }

    @Override
    public void cancel() {
       cancelled = true;
       support.firePropertyChange("state", null, getState());
       downloader.stop();
    }

    @Override
    public Category getCategory() {
        // TODO Auto-generated method stub
        return Category.AUDIO;
    }

    @Override
    public long getCurrentSize() {
        if(getState() == DownloadState.DONE){
            return getTotalSize();
        } else {
            return downloader.getAmountRead();
        }
    }

    @Override
    public List<DownloadSource> getSources() {
        //TODO: getSources
        return Collections.singletonList((DownloadSource)new CoreDownloadSource("name"));
    }

    @Override
    public int getDownloadSourceCount() {
        return downloader.getNumHosts();
    }

    @Override
    public int getPercentComplete() {
        DownloadState state = getState();
        if(state == DownloadState.FINISHING || state == DownloadState.DONE){
            return 100;
        }

        // TODO - check for div by zero?
        return (int) (100 * getCurrentSize() / getTotalSize());
    }

    @Override
    public long getRemainingDownloadTime() {
        double remaining = (getTotalSize() - getCurrentSize()) / 1024.0;
        float speed = getDownloadSpeed();
        if (speed > 0) {
            return (long) (remaining / speed);
        } else {
            return UNKNOWN_TIME;
        }

    }
    
    @Override
    public float getDownloadSpeed(){
        try {
            return downloader.getMeasuredBandwidth();
        } catch (InsufficientDataException e) {
            return 0;
        }  
    }

    @Override
    public DownloadState getState() {
        if(cancelled){
            return DownloadState.CANCELLED;
        }
        return convertState(downloader.getState());
    }

    @Override
    public String getTitle() {
        // TODO return title, not file name
        return downloader.getSaveFile().getName();
    }

    @Override
    public long getTotalSize() {
        return downloader.getContentLength();
    }

    @Override
    public void pause() {
        downloader.pause();

    }

    @Override
    public void resume() {
        downloader.resume();

    }
    
    @Override
    public int getQueuePosition(){
        return downloader.getQueuePosition();
    }
    
    private DownloadState convertState(DownloadStatus status) {
        // TODO: double check states - some are not right
        switch (status) {

        case SAVING:
        case HASHING:
            if (getTotalSize() > finishingThreshold) {
                return DownloadState.FINISHING;
            } else {
                return DownloadState.DONE;
            }

        case DOWNLOADING:
        case FETCHING://"FETCHING" is downloading .torrent file
        case IDENTIFY_CORRUPTION: // or should this be FINISHING?  doesn't seem to be used
            return DownloadState.DOWNLOADING;

        
        case CONNECTING:
        case RESUMING:
        case INITIALIZING:
        case WAITING_FOR_GNET_RESULTS:
        case QUERYING_DHT:
        case BUSY:
        case WAITING_FOR_CONNECTIONS:
        case ITERATIVE_GUESSING:
            return DownloadState.CONNECTING;

        case COMPLETE:
            return DownloadState.DONE;

        case REMOTE_QUEUED:
            return DownloadState.REMOTE_QUEUED;
            

        case QUEUED:
            return DownloadState.LOCAL_QUEUED;

        case PAUSED:
            return DownloadState.PAUSED;

        
        case WAITING_FOR_USER:
        case GAVE_UP://"GAVE_UP" means no sources
            return DownloadState.STALLED;

        case ABORTED:
            return DownloadState.CANCELLED;

        case DISK_PROBLEM:
        case CORRUPT_FILE:
        case RECOVERY_FAILED:
        case INVALID:
            return DownloadState.ERROR;
            

       
        default:
            return DownloadState.DOWNLOADING;
        }
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof CoreDownloadItem)) {
            return false;
        }
        return getDownloader().equals(((CoreDownloadItem) o).getDownloader());
    }
    
    //TODO: better hashCode
    public int hashCode(){
        if(hashCode == 0){
           hashCode =  37* getDownloader().hashCode();
        }
        return hashCode;
    }

    @Override
    public ErrorState getErrorState() {
        switch (downloader.getState()) {
        case CORRUPT_FILE:
        case RECOVERY_FAILED:
            return ErrorState.CORRUPT_FILE;
        case DISK_PROBLEM:
            return ErrorState.DISK_PROBLEM;
        case INVALID:
            return ErrorState.FILE_NOT_SHARABLE;
       // TODO: what state is this?
          //  return ErrorState.UNABLE_TO_CONNECT;
        default:
            return ErrorState.NONE;
        }
    }

    @Override
    public long getRemainingQueueTime() {
        if(queueTimeCalculator == null){
            return DownloadItem.UNKNOWN_TIME;
        }
        return queueTimeCalculator.getRemainingQueueTime(this);
    }

 
    @Override
    public int getLocalQueuePriority() {
        return downloader.getInactivePriority();
    }

    @Override
    public void setQueueTimeCalculator(QueueTimeCalculator queueTimeCalculator) {
        this.queueTimeCalculator = queueTimeCalculator;
    }
    
   
}
