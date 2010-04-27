package org.limewire.mojito.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.mojito.io.MessageDispatcher2.AbstractTransport;
import org.limewire.mojito.message2.Message;
import org.limewire.mojito.message2.MessageInputStream;
import org.limewire.mojito.message2.MessageOutputStream;
import org.limewire.security.MACCalculatorRepositoryManager;

public class DatagramTransport extends AbstractTransport implements Closeable {

    private static final Log LOG 
        = LogFactory.getLog(DatagramTransport.class);
    
    private static final int PACKET_SIZE = 4 * 1024;
    
    private final ExecutorService executor 
        = ExecutorsHelper.newSingleThreadExecutor(
            ExecutorsHelper.defaultThreadFactory("DatagramTransportThread"));
    
    private final DatagramSocket socket;
    
    private final MACCalculatorRepositoryManager calculator;
    
    private volatile boolean open = true;
    
    private Future<?> future = null;
    
    public DatagramTransport(int port, 
            MACCalculatorRepositoryManager calculator) throws IOException {
        this(new InetSocketAddress(port), calculator);
    }
    
    public DatagramTransport(InetAddress addr, int port, 
            MACCalculatorRepositoryManager calculator) throws IOException {
        this(new InetSocketAddress(addr, port), calculator);
    }

    public DatagramTransport(SocketAddress bindaddr, 
            MACCalculatorRepositoryManager calculator) throws IOException {
        socket = new DatagramSocket(bindaddr);
        this.calculator = calculator;
    }
    
    /**
     * 
     */
    public DatagramSocket getDatagramSocket() {
        return socket;
    }
    
    /**
     * 
     */
    public MACCalculatorRepositoryManager getCalculator() {
        return calculator;
    }
    
    /**
     * 
     */
    public synchronized void bind(ExecutorService executor) throws IOException {
        if (!open) {
            throw new IOException();
        }
        
        if (future != null) {
            throw new IOException();
        }
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                loop();
            }
        };
        
        future = executor.submit(task);
    }
    
    @Override
    public synchronized void close() throws IOException {
        open = false;
        
        if (executor != null) {
            executor.shutdownNow();
        }
        
        if (socket != null) {
            socket.close();
        }
        
        if (future != null) {
            future.cancel(true);
        }
    }
    
    @Override
    public void send(final SocketAddress dst, final Message message) throws IOException {
        
        if (isBound() && !socket.isClosed()) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] data = serialize(message);
                        
                        DatagramPacket packet = new DatagramPacket(
                                data, 0, data.length, dst);
                        
                        socket.send(packet);
                    } catch (IOException err) {
                        LOG.error("IOException", err);
                    }
                }
            };
            
            executor.execute(task);
        }
    }

    /**
     * 
     */
    private void loop() {
        try {
            while (open) {
                DatagramPacket packet = receive(PACKET_SIZE);
                process(packet);
            }
        } catch (IOException err) {
            LOG.error("IOException", err);
        } finally {
            shutdown();
        }
    }
    
    /**
     * 
     */
    private DatagramPacket receive(int size) throws IOException {
        byte[] dst = new byte[size];
        DatagramPacket packet 
            = new DatagramPacket(dst, 0, dst.length);
        socket.receive(packet);
        return packet;
    }
    
    /**
     * 
     */
    private void shutdown() {
        try {
            close();
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    }
    
    /**
     * 
     */
    private void process(final DatagramPacket packet) {
        if (isBound()) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    SocketAddress src = packet.getSocketAddress();
                    byte[] data = packet.getData();
                    int offset = packet.getOffset();
                    int length = packet.getLength();
                    
                    try {
                        Message msg = deserialize(src, data, offset, length);
                        handleMessage(msg);
                    } catch (IOException err) {
                        LOG.error("IOException", err);
                    }
                }
            };
            
            executor.execute(task);
        }
    }
    
    /**
     * 
     */
    private static byte[] serialize(Message message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8 * 128);
        MessageOutputStream out = new MessageOutputStream(baos);
        out.writeMessage(message);
        out.close();
        
        return baos.toByteArray();
    }
    
    /**
     * 
     */
    private Message deserialize(SocketAddress src, byte[] message, 
            int offset, int length) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(message, offset, length);
        MessageInputStream in = new MessageInputStream(bais, calculator);
        
        try {
            return in.readMessage(src);
        } finally {
            in.close();
        }
    }
}
