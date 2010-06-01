package com.limegroup.gnutella.dht;

import java.net.SocketAddress;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;

class MojitoMessageParser implements MessageParser {

    @Override
    public Message parse(byte[] header, byte[] payload, 
            Network network, byte softMax, SocketAddress addr) {
        return new MojitoMessage(header, payload);
    }
}