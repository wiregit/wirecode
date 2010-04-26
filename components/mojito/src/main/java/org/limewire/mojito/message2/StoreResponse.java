package org.limewire.mojito.message2;

import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;

public interface StoreResponse extends ResponseMessage {

    public StoreStatusCode[] getStoreStatusCodes();
}
