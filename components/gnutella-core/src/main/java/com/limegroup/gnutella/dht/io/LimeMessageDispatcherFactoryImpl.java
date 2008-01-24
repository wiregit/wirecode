package com.limegroup.gnutella.dht.io;

import org.limewire.inspection.InspectionPoint;
import org.limewire.mojito.Context;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;

@Singleton
public class LimeMessageDispatcherFactoryImpl implements
        MessageDispatcherFactory {

    private final Provider<com.limegroup.gnutella.MessageDispatcher> messageDispatcher;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<SecureMessageVerifier> secureMessageVerifier;
    private final Provider<UDPService >udpService;
    private final MessageFactory messageFactory;
    
    @InspectionPoint("dht sent messages")
    private final Message.MessageCounter dhtMessageCounter = new Message.MessageCounter(50);

    @Inject
    public LimeMessageDispatcherFactoryImpl(
            Provider<com.limegroup.gnutella.MessageDispatcher> messageDispatcher,
            Provider<MessageRouter> messageRouter,
            Provider<SecureMessageVerifier> secureMessageVerifier, Provider<UDPService> udpService,
            MessageFactory messageFactory) {
        this.messageDispatcher = messageDispatcher;
        this.messageRouter = messageRouter;
        this.secureMessageVerifier = secureMessageVerifier;
        this.udpService = udpService;
        this.messageFactory = messageFactory;
    }

    public MessageDispatcher create(Context context) {
        return new LimeMessageDispatcherImpl(context, udpService,
                secureMessageVerifier, messageRouter, messageDispatcher, messageFactory, dhtMessageCounter);
    }

}
