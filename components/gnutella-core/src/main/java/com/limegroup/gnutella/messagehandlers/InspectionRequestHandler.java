package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.InspectionRequest;
import com.limegroup.gnutella.messages.vendor.InspectionResponse;
import com.limegroup.gnutella.settings.FilterSettings;

public class InspectionRequestHandler extends RestrictedResponder {

    public InspectionRequestHandler() {
        super(FilterSettings.INSPECTOR_IP_ADDRESSES);
    }
    
    @Override
    protected void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        assert msg instanceof InspectionRequest;
        InspectionResponse r = new InspectionResponse((InspectionRequest)msg);
        handler.reply(r);
    }
}
