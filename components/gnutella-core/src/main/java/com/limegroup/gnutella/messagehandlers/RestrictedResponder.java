package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;

import org.limewire.io.IP;
import org.limewire.setting.StringArraySetting;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.filters.IPList;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;

/**
 * A message handler that responds to messages only to hosts
 * contained in a simppable whitelist.
 */
abstract class RestrictedResponder implements SimppListener, MessageHandler {
    private volatile IPList allowed;
    private final StringArraySetting setting;
    
    /**
     * @param setting the setting containing the list of allowed
     * hosts to respond to.
     */
    public RestrictedResponder(StringArraySetting setting) {
        this.setting = setting;
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
        if (!allowed.contains(new IP(handler.getAddress())))
            return;
        processAllowedMessage(msg, addr, handler);
    }
    
    /**
     * Process the specified message because it has been approved.
     */
    protected abstract void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler);
}
