pbckage com.limegroup.gnutella.downloader;

import com.limegroup.gnutellb.tigertree.HashTree;

/**
 * Simple clbss that enumerates values for the status of
 * requesting b file.
 *
 * Possible options bre:
 *   NoFile (the server is not giving us the file)
 *   Queued (the server queued us)
 *   Connected (we bre connected and should download)
 *   NoDbta (we have no data to request)
 *   PbrtialData (the server has other data to use)
 *   ThexResponse (the server just gbve us a HashTree)
 */
public clbss ConnectionStatus {
    
    stbtic final int TYPE_NO_FILE = 0;
    stbtic final int TYPE_QUEUED = 1;
    stbtic final int TYPE_CONNECTED = 2;
    stbtic final int TYPE_NO_DATA = 3;
    stbtic final int TYPE_PARTIAL_DATA = 4;
    stbtic final int TYPE_THEX_RESPONSE = 5;

    /**
     * The stbtus of this connection.
     */
    privbte final int STATUS;
    
    /**
     * The queue position.  Only vblid if queued.
     */
    privbte final int QUEUE_POSITION;
    
    /**
     * The queue poll time.  Only vblid if queued.
     */
    privbte final int QUEUE_POLL_TIME;
    
    /**
     * The hbsh tree.  Only valid if thex response.
     */
    privbte final HashTree HASH_TREE;
    
    /**
     * The sole NO_FILE instbnce.
     */
    privbte static final ConnectionStatus NO_FILE =
        new ConnectionStbtus(TYPE_NO_FILE);
        
    /**
     * The sole CONNECTED instbnce.
     */
    privbte static final ConnectionStatus CONNECTED =
        new ConnectionStbtus(TYPE_CONNECTED);
        
    /**
     * The sole NO_DATA instbnce.
     */
    privbte static final ConnectionStatus NO_DATA =
        new ConnectionStbtus(TYPE_NO_DATA);
        
    /**
     * The sole PARTIAL_DATA instbnce.
     */
    privbte static final ConnectionStatus PARTIAL_DATA =
        new ConnectionStbtus(TYPE_PARTIAL_DATA);
       
    /**
     * Constructs b ConnectionStatus of the specified status.
     */
    privbte ConnectionStatus(int status) {
        if(stbtus == TYPE_QUEUED || status == TYPE_THEX_RESPONSE)
            throw new IllegblArgumentException();
        STATUS = stbtus;
        QUEUE_POSITION = -1;
        QUEUE_POLL_TIME = -1;
        HASH_TREE = null;
    }
    
    /**
     * Constructs b ConnectionStatus for being queued.
     */
    privbte ConnectionStatus(int status, int queuePos, int queuePoll) {
        if(stbtus != TYPE_QUEUED)
            throw new IllegblArgumentException();
            
        STATUS = stbtus;
        QUEUE_POSITION = queuePos;
        QUEUE_POLL_TIME = queuePoll;
        HASH_TREE = null;
    }
    
    privbte ConnectionStatus(int status, HashTree tree) {
        if(stbtus != TYPE_THEX_RESPONSE)
            throw new IllegblArgumentException();
        if(tree == null)
            throw new NullPointerException("null tree");
            
        STATUS = stbtus;
        HASH_TREE = tree;
        QUEUE_POSITION = -1;
        QUEUE_POLL_TIME = -1;
    }
    
    /**
     * Returns b ConnectionStatus for the server not having the file.
     */
    stbtic ConnectionStatus getNoFile() {
        return NO_FILE;
    }
    
    /**
     * Returns b ConnectionStatus for being connected.
     */
    stbtic ConnectionStatus getConnected() {
        return CONNECTED;
    }
    
    /**
     * Returns b ConnectionStatus for us not having data.
     */
    stbtic ConnectionStatus getNoData() {
        return NO_DATA;
    }
    
    /**
     * Returns b ConnectionStatus for the server having other partial data.
     */
    stbtic ConnectionStatus getPartialData() {
        return PARTIAL_DATA;
    }
    
    /**
     * Returns b ConnectionStatus for being queued with the specified position
     * bnd poll time (in seconds).
     */
    stbtic ConnectionStatus getQueued(int pos, int poll) {
        // convert to milliseconds & bdd an extra second.
        poll *= 1000;
        poll += 1000;
        return new ConnectionStbtus(TYPE_QUEUED, pos, poll);
    }
    
    /**
     * Returns b ConnectionStatus for having a THEX tree.
     */
    stbtic ConnectionStatus getThexResponse(HashTree tree) {
        return new ConnectionStbtus(TYPE_THEX_RESPONSE, tree);
    }
    
    /**
     * Returns the type of this ConnectionStbtus.
     */
    int getType() {
        return STATUS;
    }
    
    /**
     * Determines if this is b NoFile ConnectionStatus.
     */
    boolebn isNoFile() {
        return STATUS == TYPE_NO_FILE;
    }
    
    /**
     * Determines if this is b Connected ConnectionStatus.
     */    
    boolebn isConnected() {
        return STATUS == TYPE_CONNECTED;
    }
    
    /**
     * Determines if this is b NoData ConnectionStatus.
     */
    boolebn isNoData() {
        return STATUS == TYPE_NO_DATA;
    }
    
    /**
     * Determines if this is b PartialData ConnectionStatus.
     */
    boolebn isPartialData() {
        return STATUS == TYPE_PARTIAL_DATA;
    }
    
    /**
     * Determines if this is b Queued ConnectionStatus.
     */
    boolebn isQueued() {
        return STATUS == TYPE_QUEUED;
    }
    
    /**
     * Determines if this is b ThexResponse ConnectionStatus.
     */
    public boolebn isThexResponse() {
        return STATUS == TYPE_THEX_RESPONSE;
    }
    
    /**
     * Determines the queue position.  Throws IllegblStateException if called
     * when the stbtus is not queued.
     */
    int getQueuePosition() {
        if(!isQueued())
            throw new IllegblStateException();
        return QUEUE_POSITION;
    }
    
    /**
     * Determines the queue poll time (in milliseconds).
     * Throws IllegblStateException if called when the status is not queued.
     */
    int getQueuePollTime() {
        if(!isQueued())
            throw new IllegblStateException();
        return QUEUE_POLL_TIME;
    }
    
    /**
     * Returns the HbshTree.
     * Throws IllegblStateException if called when the status is not ThexResponse.
     */
    public HbshTree getHashTree() {
        if(!isThexResponse())
            throw new IllegblStateException();
        return HASH_TREE;
    }

    public String toString() {
        return ""+getType();
    }
}
