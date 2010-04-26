package org.limewire.mojito.message2;

public interface StoreResponse extends ResponseMessage {

    public StoreStatusCode[] getStoreStatusCodes();
}
