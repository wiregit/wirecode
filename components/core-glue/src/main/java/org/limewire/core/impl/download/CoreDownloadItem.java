package org.limewire.core.impl.download;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadSource;
import org.limewire.core.api.download.DownloadState;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadStatus;

public class CoreDownloadItem implements DownloadItem {
    // TODO: override hash and equals - should be equal if downloader is the same

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private Downloader downloader;
    private volatile int hashCode = 0;

    public CoreDownloadItem(Downloader downloader) {
        this.downloader = downloader;
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
    public DownloadSource getDownloadSource(int index) {
        //TODO: getDownload source
        return new CoreDownloadSource("name");
    }

    @Override
    public int getDownloadSourceCount() {
        return downloader.getNumHosts();
    }

    @Override
    public int getPercent() {
        // TODO - check for div by zero?
        return (int) (100 * getCurrentSize() / getTotalSize());
    }

    @Override
    public String getRemainingTime() {
        // TODO Auto-generated method stub
        return "remaining time todo";
    }

    @Override
    public DownloadState getState() {
        // TODO Auto-generated method stub
        return convertState(downloader.getState());
    }

    @Override
    public String getTitle() {
        // TODO Auto-generated method stub
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
    public void addDownloadSource(DownloadSource source) {
        // TODO Auto-generated method stub
        
    }
    
    private static DownloadState convertState(DownloadStatus status) {
        // TODO: actually return proper state
        switch (status) {

        case SAVING:
            return DownloadState.FINISHING;

        case DOWNLOADING:
            return DownloadState.DOWNLOADING;

        case FETCHING:
        case CONNECTING:
        case RESUMING:
        case INITIALIZING:
        case WAITING_FOR_CONNECTIONS:
            return DownloadState.CONNECTING;

        case COMPLETE:
            return DownloadState.DONE;

        case REMOTE_QUEUED:
        case HASHING:
        case ITERATIVE_GUESSING:
        case QUERYING_DHT:
        case QUEUED:
            return DownloadState.QUEUED;

        case PAUSED:
            return DownloadState.PAUSED;

        case WAITING_FOR_GNET_RESULTS:
        case WAITING_FOR_USER:
        case BUSY:
            return DownloadState.STALLED;

        case GAVE_UP:
        case ABORTED:
        case DISK_PROBLEM:
        case CORRUPT_FILE:
        case INVALID:
        case RECOVERY_FAILED:
        case IDENTIFY_CORRUPTION:
            return DownloadState.ERROR;

        default:
            return DownloadState.DOWNLOADING;
        }
    }

    public boolean equals(Object o){
        if(o ==null || !(o instanceof CoreDownloadItem)){
            return false;
        }
        return getDownloader().equals(((CoreDownloadItem)o).getDownloader());
    }
    
    //TODO: better hashCode
    public int hashCode(){
        if(hashCode == 0){
           hashCode =  37* getDownloader().hashCode();
        }
        return hashCode;
    }
}
