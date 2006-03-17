/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.response;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.ResponseMessage;

public class StoreResponse extends ResponseMessage {
    
    public static final int SUCCEEDED = 0x00;
    public static final int FAILED = 0x01;
    
    private int requesting;
    private Collection storeStatus;

    public StoreResponse(int vendor, int version, KUID nodeId, 
            KUID messageId, int requesting, Collection storeStatus) {
        super(vendor, version, nodeId, messageId);
        
        this.requesting = requesting;
        this.storeStatus = storeStatus;
    }
    
    public int getRequestCount() {
        return requesting;
    }
    
    public Collection getStoreStatus() {
        return storeStatus;
    }
    
    public static class StoreStatus {
        
        private KUID key;
        private int status;
        
        public StoreStatus(KUID key, int status) {
            this.key = key;
            this.status = status;
        }
        
        public KUID getKey() {
            return key;
        }
        
        public int getStatus() {
            return status;
        }
    }
}
