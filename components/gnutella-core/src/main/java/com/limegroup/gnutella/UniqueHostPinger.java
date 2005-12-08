pbckage com.limegroup.gnutella;

import jbva.util.Set;

import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.IpPortSet;

public clbss UniqueHostPinger extends UDPPinger {

    /**
     * set of endpoints we pinged since lbst expiration
     */
    privbte final Set _recent = new IpPortSet();
    
    public UniqueHostPinger() {
        super();
    }
    

    protected void sendSingleMessbge(IpPort host, Message m) {
        if (_recent.contbins(host))
            return;
        
        _recent.bdd(host);
        super.sendSingleMessbge(host,m);
    }
    
    /**
     * clebrs the list of Endpoints we pinged since the last reset,
     * bfter sending all currently queued messages.
     */
    void resetDbta() {
        QUEUE.bdd(new Runnable(){
            public void run() {
                _recent.clebr();
            }
        });
    }

}
