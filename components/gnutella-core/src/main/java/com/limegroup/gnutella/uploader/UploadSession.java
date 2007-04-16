package com.limegroup.gnutella.uploader;

import java.net.InetAddress;

import org.limewire.http.HttpIOSession;

import com.limegroup.gnutella.BandwidthTrackerImpl;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager.QueueStatus;

public class UploadSession extends BandwidthTrackerImpl implements UploadSlotUser {

    /**
     * The min and max allowed times (in milliseconds) between requests by
     * queued hosts.
     */
    public static final int MIN_POLL_TIME = 45000; // 45 sec

    public static final int MAX_POLL_TIME = 120000; // 120 sec

    private HTTPUploader uploader;

    private final InetAddress host;

    private final UploadSlotManager slotManager;

    /** The last time this session was polled if queued */
    private volatile long lastPollTime;

    private QueueStatus queueStatus = QueueStatus.UNKNOWN;

    private HttpIOSession ioSession;

    public UploadSession(UploadSlotManager slotManager, InetAddress host, HttpIOSession ioSession) {
        this.slotManager = slotManager;
        this.host = host;
        this.ioSession = ioSession;
    }

    public void setUploader(HTTPUploader uploader) {
        this.uploader = uploader;
    }

    public HTTPUploader getUploader() {
        return uploader;
    }

    int positionInQueue() {
        return slotManager.positionInQueue(this);
    }

    public String getHost() {
        return host.getHostAddress();
    }

    public InetAddress getConnectedHost() {
        return host;
    }

    /**
     * Notifies the session of a queue poll.
     * 
     * @return true if the poll was too soon.
     */
    public boolean poll() {
        long now = System.currentTimeMillis();
        return lastPollTime + MIN_POLL_TIME > now;
    }

    /**
     * HTTP uploads are not interruptable.
     */
    public void releaseSlot() {
        throw new UnsupportedOperationException();        
    }

    public void measureBandwidth() {
        HTTPUploader uploader = getUploader();
        if (uploader != null) {
            // this will invoke UploadSession.measureBandwidth(int);
            uploader.measureBandwidth();
        }
    }

    public QueueStatus getQueueStatus() {
        return queueStatus;
    }
    
    public void setQueueStatus(QueueStatus status) {
        this.queueStatus = status;
        updatePollTime(status);
    }

    public boolean canUpload() {
        return queueStatus == QueueStatus.ACCEPTED || queueStatus == QueueStatus.BYPASS;
    }

    public boolean isAccepted() {
        return queueStatus == QueueStatus.ACCEPTED;
    }

    public boolean isQueued() {
        return queueStatus == QueueStatus.QUEUED;
    }

    public void updatePollTime(QueueStatus status) {
        if (status == QueueStatus.ACCEPTED || status == QueueStatus.BYPASS) {
            lastPollTime = 0;
        } else if (status == QueueStatus.QUEUED) {
            lastPollTime = System.currentTimeMillis();
        }        
    }
    
    public HttpIOSession getIOSession() {
        return ioSession;
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[host=" + getHost() + ",queueStatus=" + queueStatus + "]";
    }

}
