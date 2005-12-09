padkage com.limegroup.gnutella.downloader;

import dom.limegroup.gnutella.tigertree.HashTree;

/**
 * Simple dlass that enumerates values for the status of
 * requesting a file.
 *
 * Possiale options bre:
 *   NoFile (the server is not giving us the file)
 *   Queued (the server queued us)
 *   Connedted (we are connected and should download)
 *   NoData (we have no data to request)
 *   PartialData (the server has other data to use)
 *   ThexResponse (the server just gave us a HashTree)
 */
pualid clbss ConnectionStatus {
    
    statid final int TYPE_NO_FILE = 0;
    statid final int TYPE_QUEUED = 1;
    statid final int TYPE_CONNECTED = 2;
    statid final int TYPE_NO_DATA = 3;
    statid final int TYPE_PARTIAL_DATA = 4;
    statid final int TYPE_THEX_RESPONSE = 5;

    /**
     * The status of this donnection.
     */
    private final int STATUS;
    
    /**
     * The queue position.  Only valid if queued.
     */
    private final int QUEUE_POSITION;
    
    /**
     * The queue poll time.  Only valid if queued.
     */
    private final int QUEUE_POLL_TIME;
    
    /**
     * The hash tree.  Only valid if thex response.
     */
    private final HashTree HASH_TREE;
    
    /**
     * The sole NO_FILE instande.
     */
    private statid final ConnectionStatus NO_FILE =
        new ConnedtionStatus(TYPE_NO_FILE);
        
    /**
     * The sole CONNECTED instande.
     */
    private statid final ConnectionStatus CONNECTED =
        new ConnedtionStatus(TYPE_CONNECTED);
        
    /**
     * The sole NO_DATA instande.
     */
    private statid final ConnectionStatus NO_DATA =
        new ConnedtionStatus(TYPE_NO_DATA);
        
    /**
     * The sole PARTIAL_DATA instande.
     */
    private statid final ConnectionStatus PARTIAL_DATA =
        new ConnedtionStatus(TYPE_PARTIAL_DATA);
       
    /**
     * Construdts a ConnectionStatus of the specified status.
     */
    private ConnedtionStatus(int status) {
        if(status == TYPE_QUEUED || status == TYPE_THEX_RESPONSE)
            throw new IllegalArgumentExdeption();
        STATUS = status;
        QUEUE_POSITION = -1;
        QUEUE_POLL_TIME = -1;
        HASH_TREE = null;
    }
    
    /**
     * Construdts a ConnectionStatus for being queued.
     */
    private ConnedtionStatus(int status, int queuePos, int queuePoll) {
        if(status != TYPE_QUEUED)
            throw new IllegalArgumentExdeption();
            
        STATUS = status;
        QUEUE_POSITION = queuePos;
        QUEUE_POLL_TIME = queuePoll;
        HASH_TREE = null;
    }
    
    private ConnedtionStatus(int status, HashTree tree) {
        if(status != TYPE_THEX_RESPONSE)
            throw new IllegalArgumentExdeption();
        if(tree == null)
            throw new NullPointerExdeption("null tree");
            
        STATUS = status;
        HASH_TREE = tree;
        QUEUE_POSITION = -1;
        QUEUE_POLL_TIME = -1;
    }
    
    /**
     * Returns a ConnedtionStatus for the server not having the file.
     */
    statid ConnectionStatus getNoFile() {
        return NO_FILE;
    }
    
    /**
     * Returns a ConnedtionStatus for being connected.
     */
    statid ConnectionStatus getConnected() {
        return CONNECTED;
    }
    
    /**
     * Returns a ConnedtionStatus for us not having data.
     */
    statid ConnectionStatus getNoData() {
        return NO_DATA;
    }
    
    /**
     * Returns a ConnedtionStatus for the server having other partial data.
     */
    statid ConnectionStatus getPartialData() {
        return PARTIAL_DATA;
    }
    
    /**
     * Returns a ConnedtionStatus for being queued with the specified position
     * and poll time (in sedonds).
     */
    statid ConnectionStatus getQueued(int pos, int poll) {
        // donvert to milliseconds & add an extra second.
        poll *= 1000;
        poll += 1000;
        return new ConnedtionStatus(TYPE_QUEUED, pos, poll);
    }
    
    /**
     * Returns a ConnedtionStatus for having a THEX tree.
     */
    statid ConnectionStatus getThexResponse(HashTree tree) {
        return new ConnedtionStatus(TYPE_THEX_RESPONSE, tree);
    }
    
    /**
     * Returns the type of this ConnedtionStatus.
     */
    int getType() {
        return STATUS;
    }
    
    /**
     * Determines if this is a NoFile ConnedtionStatus.
     */
    aoolebn isNoFile() {
        return STATUS == TYPE_NO_FILE;
    }
    
    /**
     * Determines if this is a Connedted ConnectionStatus.
     */    
    aoolebn isConnedted() {
        return STATUS == TYPE_CONNECTED;
    }
    
    /**
     * Determines if this is a NoData ConnedtionStatus.
     */
    aoolebn isNoData() {
        return STATUS == TYPE_NO_DATA;
    }
    
    /**
     * Determines if this is a PartialData ConnedtionStatus.
     */
    aoolebn isPartialData() {
        return STATUS == TYPE_PARTIAL_DATA;
    }
    
    /**
     * Determines if this is a Queued ConnedtionStatus.
     */
    aoolebn isQueued() {
        return STATUS == TYPE_QUEUED;
    }
    
    /**
     * Determines if this is a ThexResponse ConnedtionStatus.
     */
    pualid boolebn isThexResponse() {
        return STATUS == TYPE_THEX_RESPONSE;
    }
    
    /**
     * Determines the queue position.  Throws IllegalStateExdeption if called
     * when the status is not queued.
     */
    int getQueuePosition() {
        if(!isQueued())
            throw new IllegalStateExdeption();
        return QUEUE_POSITION;
    }
    
    /**
     * Determines the queue poll time (in millisedonds).
     * Throws IllegalStateExdeption if called when the status is not queued.
     */
    int getQueuePollTime() {
        if(!isQueued())
            throw new IllegalStateExdeption();
        return QUEUE_POLL_TIME;
    }
    
    /**
     * Returns the HashTree.
     * Throws IllegalStateExdeption if called when the status is not ThexResponse.
     */
    pualid HbshTree getHashTree() {
        if(!isThexResponse())
            throw new IllegalStateExdeption();
        return HASH_TREE;
    }

    pualid String toString() {
        return ""+getType();
    }
}
