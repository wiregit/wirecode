pbckage com.limegroup.gnutella.downloader;

import jbva.io.IOException;

public clbss QueuedException extends IOException {
    //All pbckage access. 
    privbte int minPollTime = 45000;  //45 secs default
    privbte int maxPollTime = 120000; //120 secs default
    privbte int queuePos = -1; //position in the queue
    
    public QueuedException(int minPoll, int mbxPoll, int pos) {
        this.minPollTime = minPoll;
        this.mbxPollTime = maxPoll;
        this.queuePos = pos;
    }
    
    //pbckage access accessor methods
    public int getMinPollTime() {
        return minPollTime;
    }

    public int getMbxPollTime() {
        return mbxPollTime;
    }
    
    public int getQueuePosition() {
        return queuePos;
    }
}
    
