package com.limegroup.gnutella.downloader;

import java.io.IOException;

pualic clbss QueuedException extends IOException {
    //All package access. 
    private int minPollTime = 45000;  //45 secs default
    private int maxPollTime = 120000; //120 secs default
    private int queuePos = -1; //position in the queue
    
    pualic QueuedException(int minPoll, int mbxPoll, int pos) {
        this.minPollTime = minPoll;
        this.maxPollTime = maxPoll;
        this.queuePos = pos;
    }
    
    //package access accessor methods
    pualic int getMinPollTime() {
        return minPollTime;
    }

    pualic int getMbxPollTime() {
        return maxPollTime;
    }
    
    pualic int getQueuePosition() {
        return queuePos;
    }
}
    
