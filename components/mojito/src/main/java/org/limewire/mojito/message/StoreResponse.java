package org.limewire.mojito.message;

public interface StoreResponse extends ResponseMessage {

    public StoreStatusCode[] getStoreStatusCodes();
}
