package org.limewire.core.impl.download;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadSource;
import org.limewire.core.api.download.DownloadState;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Downloader.DownloadStatus;

public class CoreDownloadItem implements DownloadItem {
   
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private Downloader downloader;
    private volatile int hashCode = 0;
//    private long size = 0;

    public CoreDownloadItem(Downloader downloader) {
        this.downloader = downloader;
        //TODO: put this back in - using timer for now
//        downloader.addListener(new EventListener<DownloadStatusEvent>(){
//
//            @Override
//            public void handleEvent(DownloadStatusEvent event) {
//                //TODO: fire correct property
//                long oldSize = size;
//                size = event.getSource().getAmountRead();
//                support.firePropertyChange("size", oldSize, size);
//            }
//            
//        });
    }

    public Downloader getDownloader(){
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
       downloader.stop();
    }

    @Override
    public Category getCategory() {
        // TODO Auto-generated method stub
        return Category.AUDIO;
    }

    @Override
    public double getCurrentSize() {
        // TODO right method?
        return downloader.getAmountRead();
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
        // TODO - check for div by zero?
        return (int) (100 * getCurrentSize() / getTotalSize());
    }

    @Override
    public String getRemainingDownloadTime() {
        double remaining = (getTotalSize() - getCurrentSize()) / 1024.0;
        float speed = getDownloadSpeed();
        if (speed > 0) {
            return CommonUtils.seconds2time((long) (remaining / speed));
        } else {
            // TODO: remaining time when speed is unknown
            return "unknown";
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
        return convertState(downloader.getState());
    }

    @Override
    public String getTitle() {
        // TODO return title, not file name
        return downloader.getFile().getName();
    }

    @Override
    public double getTotalSize() {
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
    
    private static DownloadState convertState(DownloadStatus status) {
        // TODO: double check states
        switch (status) {

        case SAVING:
        case HASHING:
            return DownloadState.FINISHING;

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
    public String getRemainingStateTime() {
        return CommonUtils.seconds2time(downloader.getRemainingStateTime());
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
    
   
}
