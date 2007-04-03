package com.limegroup.gnutella.uploader;

import java.net.InetAddress;

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

    public UploadSession(UploadSlotManager slotManager, InetAddress host) {
        this.slotManager = slotManager;
        this.host = host;
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
        // TODO Auto-generated method stub
        
    }

    public QueueStatus getQueueStatus() {
        return queueStatus;
    }
    
    public void setQueueStatus(QueueStatus status) {
        this.queueStatus  = status;
        
        if (status == QueueStatus.ACCEPTED || status == QueueStatus.BYPASS) {
            lastPollTime = 0;
        } else if (status == QueueStatus.QUEUED) {
            lastPollTime = System.currentTimeMillis();
        }
    }

    public boolean isAccepted() {
        return queueStatus == QueueStatus.ACCEPTED;
    }

    public boolean isQueued() {
        return queueStatus == QueueStatus.QUEUED;
    }

}
