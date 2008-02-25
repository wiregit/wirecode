package com.limegroup.bittorrent;

import java.util.concurrent.ScheduledExecutorService;

import com.limegroup.bittorrent.handshaking.BTConnectionFetcher;

public interface ManagedTorrent extends Torrent, BTLinkListener {

    /**
     * notification that a request to the tracker(s) has started.
     */
    void setScraping();

    /**
     * Accessor for the info hash
     * 
     * @return byte[] containing the info hash
     */
    byte[] getInfoHash();

    /**
     * Accessor for meta info
     * 
     * @return <tt>BTMetaInfo</tt> for this torrent
     */
    BTMetaInfo getMetaInfo();

    /**
     * @return the <tt>TorrentContext</tt> for this torrent
     */
    TorrentContext getContext();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#isComplete()
     */
    boolean isComplete();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#start()
     */
    void start();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#stop()
     */
    void stop();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#pause()
     */
    void pause();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#resume()
     */
    boolean resume();

    void trackerRequestFailed();

    /**
     * @return the state of this torrent
     */
    TorrentState getState();

    /**
     * adds location to try
     * 
     * @param to a TorrentLocation for this download
     */
    void addEndpoint(TorrentLocation to);

    /**
     * Stops the torrent because of tracker failure.
     */
    void stopVoluntarily();

    /**
     * @return true if we need to fetch any more connections
     */
    boolean needsMoreConnections();

    /**
     * @return true if a fetched connection should be added.
     */
    boolean shouldAddConnection(TorrentLocation loc);

    /**
     * adds a fetched connection
     * 
     * @return true if it was added
     */
    boolean addConnection(final BTLink btc);

    /**
     * @return the next time we should announce to the tracker
     */
    long getNextTrackerRequestTime();

    /**
     * @return a peer we should try to connect to next
     */
    TorrentLocation getTorrentLocation();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#isPaused()
     */
    boolean isPaused();

    /**
     * two torrents are equal if their infoHashes are.
     */
    boolean equals(Object o);

    /**
     * @return if the torrent is active - either downloading or seeding, saving
     *         or verifying
     */
    boolean isActive();

    /**
     * @return if the torrent can be paused
     */
    boolean isPausable();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#getNumConnections()
     */
    int getNumConnections();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#getNumPeers()
     */
    int getNumPeers();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#getNumBusyPeers()
     */
    int getNumNonInterestingPeers();

    int getNumChockingPeers();

    long getTotalUploaded();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#getTotalDownloaded()
     */
    long getTotalDownloaded();

    /**
     * @return the ratio of uploaded / downloaded data.
     */
    float getRatio();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#getAmountLost()
     */
    long getAmountLost();

    boolean hasNonBusyLocations();

    /**
     * @return the time until a recently failed location can be retried, or
     *         Long.MAX_VALUE if no such found.
     */
    long getNextLocationRetryTime();

    /**
     * @return true if continuing is hopeless
     */
    boolean shouldStop();

    /**
     * @return the <tt>BTConnectionFetcher</tt> for this torrent.
     */
    BTConnectionFetcher getFetcher();

    /**
     * @return the <tt>SchedulingThreadPool</tt> executing network- related
     *         tasks
     */
    ScheduledExecutorService getNetworkScheduledExecutorService();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#measureBandwidth()
     */
    void measureBandwidth();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.Torrent#getMeasuredBandwidth(boolean)
     */
    float getMeasuredBandwidth(boolean downstream);

    int getTriedHostCount();

    /**
     * @return true if this torrent is currently uploading
     */
    boolean isUploading();

    /**
     * @return true if this torrent is currently suspended A torrent is
     *         considered suspended if there are connections interested in it
     *         but all are choked.
     */
    boolean isSuspended();

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.BTLinkListener#linkClosed(com.limegroup.bittorrent.BTLink)
     */
    public void linkClosed(BTLink closed);

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.BTLinkListener#countDownloaded(int)
     */
    public void countDownloaded(int downloaded);

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.BTLinkListener#linkInterested(com.limegroup.bittorrent.BTLink)
     */
    public void linkInterested(BTLink interested);

    /*
     * 0
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.BTLinkListener#linkNotInterested(com.limegroup.bittorrent.BTLink)
     */
    public void linkNotInterested(BTLink notInterested);
    
    public void chunkVerified(int in);

}