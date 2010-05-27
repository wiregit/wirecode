package org.limewire.mojito.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.security.MACCalculatorRepositoryManager;

/**
 * The default implementation of {@link MessageCodec}
 */
public class DefaultMessageCodec extends AbstractMessageCodec {

    protected final MACCalculatorRepositoryManager calculator;
    
    public DefaultMessageCodec() {
        this (new MACCalculatorRepositoryManager());
    }
    
    public DefaultMessageCodec(MACCalculatorRepositoryManager calculator) {
        this.calculator = calculator;
    }
    
    @Override
    public Message decode(SocketAddress src, byte[] message, int offset, int length)
            throws IOException {
        
        ByteArrayInputStream bais = new ByteArrayInputStream(message, offset, length);
        MessageInputStream in = new MessageInputStream(bais, calculator);
        
        try {
            return in.readMessage(src);
        } finally {
            in.close();
        }
    }

    @Override
    public byte[] encode(SocketAddress dst, Message message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8 * 128);
        MessageOutputStream out = new MessageOutputStream(baos);
        out.writeMessage(message);
        out.close();
        
        return baos.toByteArray();
    }
}
