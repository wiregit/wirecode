package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;

import org.limewire.io.IP;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringArraySetting;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPReplyHandler;
import com.limegroup.gnutella.filters.IPList;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.RoutableGGEPMessage;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;

/**
 * A message handler that responds to messages only to hosts
 * contained in a simppable whitelist.
 */
abstract class RestrictedResponder implements SimppListener, MessageHandler {
    /** list of hosts that we can send responses to */
    private volatile IPList allowed;
    /** setting to check for updates to the host list */
    private final StringArraySetting setting;
    /** an optional verifier to very secure messages */
    private final SecureMessageVerifier verifier;
    /** The last version of the routable message that was routed */
    private final IntSetting lastRoutedVersion;
    
    public RestrictedResponder(StringArraySetting setting) {
        this(setting, null, null);
    }
    
    /**
     * @param setting the setting containing the list of allowed
     * hosts to respond to.
     * @param verifier the <tt>SignatureVerifier</tt> to use.  Null if we
     * want to process all messages.
     */
    public RestrictedResponder(StringArraySetting setting, 
            SecureMessageVerifier verifier,
            IntSetting lastRoutedVersion) {
        this.setting = setting;
        this.verifier = verifier;
        this.lastRoutedVersion = lastRoutedVersion;
        allowed = new IPList();
        allowed.add("*.*.*.*");
        SimppManager.instance().addListener(this);
        updateAllowed();
    }
    
    private void updateAllowed() {
        IPList newCrawlers = new IPList();
        try {
            for (String ip : setting.getValue())
                newCrawlers.add(new IP(ip));
            if (newCrawlers.isValidFilter(false))
                allowed = newCrawlers;
        } catch (IllegalArgumentException badSimpp) {}
    }
    
    public void simppUpdated(int newVersion) {
        updateAllowed();
    }
    
    public final void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        if (msg instanceof RoutableGGEPMessage) {
            // if we have a verifier, verify
            if (verifier != null)
                verifier.verify((SecureMessage)msg, new SecureCallback(addr, handler));
            else
                processRoutableMessage((RoutableGGEPMessage)msg, addr, handler);
        } else {
            // just check the return address.
            if (!allowed.contains(new IP(handler.getAddress())))
                return;
            processAllowedMessage(msg, addr, handler);
        }
    }
    
    /** Processes a routable message */
    private void processRoutableMessage(RoutableGGEPMessage msg, InetSocketAddress addr, ReplyHandler handler) {
        
        // if the message specifies a return address, use that 
        if (msg.getReturnAddress() != null)
            handler = new UDPReplyHandler(msg.getReturnAddress().getInetAddress(),
                    msg.getReturnAddress().getPort());
        
        if (!allowed.contains(new IP(handler.getAddress())))
            return;
        
        // check if its a newer version than the last we routed.
        // messages w/o version just go through.
        int routableVersion = msg.getRoutableVersion();
        if (routableVersion >= 0) {
            synchronized(lastRoutedVersion) {
                if (routableVersion <= lastRoutedVersion.getValue())
                    return;
                lastRoutedVersion.setValue(routableVersion);
            }
        }
        
        processAllowedMessage(msg, addr, handler);
        
    }
    
    /** 
     * small listener that will process a message if it verifies.
     */
    private class SecureCallback implements SecureMessageCallback {
        private final InetSocketAddress addr;
        private final ReplyHandler handler;
        SecureCallback(InetSocketAddress addr, ReplyHandler handler) {
            this.addr = addr;
            this.handler = handler;
        }
        
        public void handleSecureMessage(SecureMessage sm, boolean passed) {
            if (!passed)
                return;
            processRoutableMessage((RoutableGGEPMessage)sm, addr, handler);
        }
    }

    /**
     * Process the specified message because it has been approved.
     */
    protected abstract void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler);
}
