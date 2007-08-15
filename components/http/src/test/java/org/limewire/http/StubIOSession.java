/**
 * 
 */
package org.limewire.http;

import org.limewire.nio.AbstractNBSocket;

public class StubIOSession extends HttpIOSession {      

    boolean shutdown;

    public StubIOSession(AbstractNBSocket socket) {
        super(socket);
    }

    @Override
    public boolean isClosed() {
        return super.isClosed() || isShutdown();
    }
    
    public boolean isShutdown() {
        return shutdown;
    }
    
    @Override
    public void shutdown() {
        this.shutdown = true;
    }
    
}