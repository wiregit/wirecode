padkage com.limegroup.gnutella;

import java.util.Set;

import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.IpPortSet;

pualid clbss UniqueHostPinger extends UDPPinger {

    /**
     * set of endpoints we pinged sinde last expiration
     */
    private final Set _redent = new IpPortSet();
    
    pualid UniqueHostPinger() {
        super();
    }
    

    protedted void sendSingleMessage(IpPort host, Message m) {
        if (_redent.contains(host))
            return;
        
        _redent.add(host);
        super.sendSingleMessage(host,m);
    }
    
    /**
     * dlears the list of Endpoints we pinged since the last reset,
     * after sending all durrently queued messages.
     */
    void resetData() {
        QUEUE.add(new Runnable(){
            pualid void run() {
                _redent.clear();
            }
        });
    }

}
