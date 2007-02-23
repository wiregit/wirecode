package com.limegroup.gnutella.uploader;

import java.net.InetAddress;

import com.limegroup.gnutella.BandwidthTrackerImpl;
import com.limegroup.gnutella.Uploader;

public class UploadSession extends BandwidthTrackerImpl implements UploadSlotUser {

    /**
     * The min and max allowed times (in milliseconds) between requests by
     * queued hosts.
     */
    public static final int MIN_POLL_TIME = 45000; // 45 sec

    public static final int MAX_POLL_TIME = 120000; // 120 sec

    private Uploader uploader;

    private final InetAddress host;

    private final UploadSlotManager slotManager;

    /** The last time this session was polled if queued */
    private volatile long lastPollTime;

    public UploadSession(UploadSlotManager slotManager, InetAddress host) {
        this.slotManager = slotManager;
        this.host = host;
    }

    public void setUploader(Uploader uploader) {
        this.uploader = uploader;
    }

    public Uploader getUploader() {
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
        boolean tooSoon = lastPollTime + MIN_POLL_TIME > now;
        lastPollTime = now;
        return tooSoon;
    }

    public void releaseSlot() {
        // TODO Auto-generated method stub
        
    }

    public void measureBandwidth() {
        // TODO Auto-generated method stub
        
    }

}
