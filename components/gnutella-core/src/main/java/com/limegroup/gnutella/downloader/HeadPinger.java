package com.limegroup.gnutella.downloader;

import java.util.Collection;
import java.util.Iterator;

import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.Cancellable;
import com.limegroup.gnutella.util.IpPort;

/**
 * A primitive pinger that doesn't register or unregister listeners.
 * Ideally this would be the base class, and all the listener manipulation
 * should be happening in the derived classes. 
 */
public class HeadPinger extends UDPPinger {

    protected void send(Collection c, MessageListener l, Cancellable canceller, Message message){
        Iterator iter = c.iterator();
        while(iter.hasNext() && !canceller.isCancelled()) 
            sendSingleMessage((IpPort)iter.next(),message);
    }

}
