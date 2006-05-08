package de.kapsi.net.kademlia.io;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.messages.Message;

public class MessageDispatcherImpl extends MessageDispatcher implements Runnable {

    private static final Log LOG = LogFactory.getLog(MessageDispatcherImpl.class);
    
    private static final long SLEEP = 50L;
    
    private Selector selector;
    
    public MessageDispatcherImpl(Context context) {
        super(context);
    }
    
    public void bind(SocketAddress address) throws IOException {
        if (isOpen()) {
            throw new IOException("Already open");
        }
        
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
        
        DatagramSocket socket = channel.socket();
        socket.setReuseAddress(false);
        socket.setReceiveBufferSize(Message.MAX_MESSAGE_SIZE);
        socket.setSendBufferSize(Message.MAX_MESSAGE_SIZE);
        
        socket.bind(address);
        
        setDatagramChannel(channel);
    }
    
    public void start() {
        run();
    }
    
    public void stop() {
        try {
            if (selector != null) {
                selector.close();
                getDatagramChannel().close();
            }
        } catch (IOException err) {
            LOG.error("", err);
        }
    }
    
    public boolean isRunning() {
        return isOpen() && getDatagramChannel().isRegistered();
    }

    protected boolean allow(Message message) {
        return true;
    }
    
    protected void process(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception err) {
            LOG.error("An exception occured during processing", err);
        }
    }
    
    private void interest(int ops, boolean on) {
        try {
            DatagramChannel channel = getDatagramChannel();
            SelectionKey sk = channel.keyFor(selector);
            if (sk != null && sk.isValid()) {
                synchronized(channel.blockingLock()) {
                    if (on) {
                        sk.interestOps(sk.interestOps() | ops);
                    } else {
                        sk.interestOps(sk.interestOps() & ~ops);
                    }
                }
            }
        } catch (CancelledKeyException ignore) {}
    }

    protected void interestRead(boolean on) {
        interest(SelectionKey.OP_READ, on);
    }
    
    protected void interestWrite(boolean on) {
        interest(SelectionKey.OP_WRITE, on);
    }
    
    public void run() {
        
        while(isRunning()) {
            
            try {
                selector.select(SLEEP);
                
                // READ
                handleRead();
                
                // WRITE
                handleWrite();
                
                // CLEANUP
                handleClenup();
            } catch (ClosedChannelException err) {
                // thrown as close() is called asynchron
                //LOG.error(err);
            } catch (IOException err) {
                LOG.fatal("MessageHandler IO exception: ",err);
            }
        }
    }
}
