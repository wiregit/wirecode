package com.limegroup.gnutella.dht.db;

import org.limewire.concurrent.OnewayExchanger;

public class BlockingSearchListener<Result> implements SearchListener<Result> {

    final OnewayExchanger<Result, RuntimeException> exchanger = new OnewayExchanger<Result, RuntimeException>();
    
    public void handleResult(Result result) {
        exchanger.setValue(result);
    }

    public void handleSearchDone(boolean success) {
        if (!success) {
            exchanger.setValue(null);
        }
    }
    
    public Result getResult() {
        try {
            return exchanger.get();
        } catch (InterruptedException ie) {
            return null;
        }
    }
}
