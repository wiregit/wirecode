package com.limegroup.gnutella.messagehandlers;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.GiveStatsVendorMessage;
import com.limegroup.gnutella.messages.vendor.StatisticVendorMessage;
import com.limegroup.gnutella.settings.FilterSettings;

/**
 * Handles incoming Give Stats VM requests, restricting responses to the
 * inspector ip addresses.
 */
public class GiveStatsVMHandler extends RestrictedResponder {

    public GiveStatsVMHandler() {
        super(FilterSettings.INSPECTOR_IP_ADDRESSES);
    }

    @Override
    protected void processAllowedMessage(Message msg, InetSocketAddress addr,
            ReplyHandler handler) {
        StatisticVendorMessage statVM = null;
        GiveStatsVendorMessage gsm = (GiveStatsVendorMessage) msg;
        try {
            // create the reply if we understand how
            if (StatisticVendorMessage.isSupported(gsm)) {
                statVM = new StatisticVendorMessage(gsm);
                // OK. Now send this message back to the client that asked for
                // stats
                handler.handleStatisticVM(statVM);
            }
        } catch (IOException iox) {
            return; // what can we really do now?
        }
    }

}
