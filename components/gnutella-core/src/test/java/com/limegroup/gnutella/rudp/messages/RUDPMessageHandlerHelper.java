package com.limegroup.gnutella.rudp.messages;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;

public class RUDPMessageHandlerHelper {
    
    public static void setHandlers(MessageHandler ack, MessageHandler data,
                                   MessageHandler fin, MessageHandler keepAlive,
                                   MessageHandler syn) throws Exception {
        ProviderHacks.getMessageRouter().setUDPMessageHandler(LimeAckMessageImpl.class, ack);
        ProviderHacks.getMessageRouter().setUDPMessageHandler(LimeDataMessageImpl.class, data);
        ProviderHacks.getMessageRouter().setUDPMessageHandler(LimeFinMessageImpl.class, fin);
        ProviderHacks.getMessageRouter().setUDPMessageHandler(LimeKeepAliveMessageImpl.class, keepAlive);
        ProviderHacks.getMessageRouter().setUDPMessageHandler(LimeSynMessageImpl.class, syn);
    }
    
    public static void addHandler(MessageHandler handler) {
        ProviderHacks.getMessageRouter().addUDPMessageHandler(LimeAckMessageImpl.class, handler);
        ProviderHacks.getMessageRouter().addUDPMessageHandler(LimeDataMessageImpl.class, handler);
        ProviderHacks.getMessageRouter().addUDPMessageHandler(LimeFinMessageImpl.class, handler);
        ProviderHacks.getMessageRouter().addUDPMessageHandler(LimeKeepAliveMessageImpl.class, handler);
        ProviderHacks.getMessageRouter().addUDPMessageHandler(LimeSynMessageImpl.class, handler);
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
        PrivilegedAccessor.setValue(o, field, ProviderHacks.getMessageRouter().getUDPMessageHandler(clazz));
    }

}
