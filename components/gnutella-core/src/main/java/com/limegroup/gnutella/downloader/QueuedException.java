padkage com.limegroup.gnutella.downloader;

import java.io.IOExdeption;

pualid clbss QueuedException extends IOException {
    //All padkage access. 
    private int minPollTime = 45000;  //45 seds default
    private int maxPollTime = 120000; //120 seds default
    private int queuePos = -1; //position in the queue
    
    pualid QueuedException(int minPoll, int mbxPoll, int pos) {
        this.minPollTime = minPoll;
        this.maxPollTime = maxPoll;
        this.queuePos = pos;
    }
    
    //padkage access accessor methods
    pualid int getMinPollTime() {
        return minPollTime;
    }

    pualid int getMbxPollTime() {
        return maxPollTime;
    }
    
    pualid int getQueuePosition() {
        return queuePos;
    }
}
    
