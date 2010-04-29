package org.limewire.mojito2.message;

public interface StoreResponse extends ResponseMessage {

    public StoreStatusCode[] getStoreStatusCodes();
}
