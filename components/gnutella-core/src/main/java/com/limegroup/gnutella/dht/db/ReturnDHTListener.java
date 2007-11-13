package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.result.FindValueResult;

public interface ReturnDHTListener {
    
    
    /**
     * takes care of returning future values from the DHT
     */
    public FindValueResult returnValue();

}
