package org.limewire.rudp;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.rudp.messages.RUDPMessage;

/** 
 * Dispatches messages.
 */
public class DefaultMessageDispatcher implements MessageDispatcher {
    
    private static final Log LOG = LogFactory.getLog(DefaultMessageDispatcher.class);
    
    private final Executor executor = ExecutorsHelper.newProcessingQueue("RUDPDispatch");
    private volatile UDPMultiplexor multiplexor;
    
    void setUDPMultiplexor(UDPMultiplexor plexor) {
        if(multiplexor != null)
            LOG.warn("Overriding old multiplexor!");
        multiplexor = plexor;
    }
    
    public void dispatch(RUDPMessage message, InetSocketAddress from) {
        executor.execute(new Dispatch(multiplexor, message, from));
    }
    
    private static class Dispatch implements Runnable {
        private final UDPMultiplexor plexor;
        private final RUDPMessage msg;
        private final InetSocketAddress from;
        
        Dispatch(UDPMultiplexor plexor, RUDPMessage msg, InetSocketAddress from) {
            this.plexor = plexor;
            this.msg = msg;
            this.from = from;
        }
        
        public void run() {
            plexor.routeMessage(msg, from);
        }
    }

}
