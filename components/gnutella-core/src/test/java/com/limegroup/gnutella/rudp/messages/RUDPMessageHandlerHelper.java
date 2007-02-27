package com.limegroup.gnutella.rudp.messages;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;

public class RUDPMessageHandlerHelper {
    
    public static void setHandlers(MessageHandler ack, MessageHandler data,
                                   MessageHandler fin, MessageHandler keepAlive,
                                   MessageHandler syn) throws Exception {
        RouterService.getMessageRouter().setUDPMessageHandler(LimeAckMessageImpl.class, ack);
        RouterService.getMessageRouter().setUDPMessageHandler(LimeDataMessageImpl.class, data);
        RouterService.getMessageRouter().setUDPMessageHandler(LimeFinMessageImpl.class, fin);
        RouterService.getMessageRouter().setUDPMessageHandler(LimeKeepAliveMessageImpl.class, keepAlive);
        RouterService.getMessageRouter().setUDPMessageHandler(LimeSynMessageImpl.class, syn);
    }
    
    public static void addHandler(MessageHandler handler) {
        RouterService.getMessageRouter().addUDPMessageHandler(LimeAckMessageImpl.class, handler);
        RouterService.getMessageRouter().addUDPMessageHandler(LimeDataMessageImpl.class, handler);
        RouterService.getMessageRouter().addUDPMessageHandler(LimeFinMessageImpl.class, handler);
        RouterService.getMessageRouter().addUDPMessageHandler(LimeKeepAliveMessageImpl.class, handler);
        RouterService.getMessageRouter().addUDPMessageHandler(LimeSynMessageImpl.class, handler);
    }
    
    public static void setHandlerFields(Object object, String ack, String data,
                                   String fin, String keepAlive, String syn) throws Exception {
        set(object, ack,       LimeAckMessageImpl.class);
        set(object, data,      LimeDataMessageImpl.class);
        set(object, fin,       LimeFinMessageImpl.class);
        set(object, keepAlive, LimeKeepAliveMessageImpl.class);
        set(object, syn,       LimeSynMessageImpl.class);
    }
    
    private static void set(Object o, String field, Class<? extends Message> clazz) throws Exception {
        PrivilegedAccessor.setValue(o, field, RouterService.getMessageRouter().getUDPMessageHandler(clazz));
    }

}
