package org.limewire.http;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.log4j.Logger;

class IdleConnectionCloser implements Runnable {

    private static final Logger LOG = Logger.getLogger(IdleConnectionCloser.class);

    private static final long IDLE_TIME = 30 * 1000; // 30 seconds.

    private final ClientConnectionManager manager;

    IdleConnectionCloser(ClientConnectionManager manager){
        this.manager = manager;
    }
    
    public void run() {
        try {
            manager.closeIdleConnections(IDLE_TIME);
        } catch (Throwable t) {
            LOG.error(t);
        }
    }
}
