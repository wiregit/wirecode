package com.limegroup.gnutella.dht2;

import java.net.SocketAddress;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;

public class MojitoMessageParser implements MessageParser {

    @Override
    public Message parse(byte[] header, byte[] payload, 
            Network network, byte softMax, SocketAddress addr) {
        return new MojitoMessage(header, payload);
    }
}
