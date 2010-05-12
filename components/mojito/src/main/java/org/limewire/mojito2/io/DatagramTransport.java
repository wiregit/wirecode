package org.limewire.mojito2.io;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;

public class DatagramTransport extends AbstractTransport implements Closeable {

    private static final Log LOG 
        = LogFactory.getLog(DatagramTransport.class);
    
    private static final int PACKET_SIZE = 4 * 1024;
    
    private static final ExecutorService EXECUTOR 
        = Executors.newCachedThreadPool(
            ExecutorsHelper.defaultThreadFactory(
                "DatagramTransportThread"));
    
    private final SocketAddress bindaddr;
    
    private final ExecutorService executor;
    
    private volatile DatagramSocket socket;
    
    private volatile boolean open = true;
    
    private Future<?> future = null;
    
    public DatagramTransport(int port) {
        this(EXECUTOR, new InetSocketAddress(port));
    }
    
    public DatagramTransport(InetAddress addr, int port) {
        this (EXECUTOR, new InetSocketAddress(addr, port));
    }

    public DatagramTransport(SocketAddress bindaddr) {
        this (EXECUTOR, bindaddr);
    }
    
    public DatagramTransport(ExecutorService executor, 
            SocketAddress bindaddr) {
        this.executor = executor;
        this.bindaddr = bindaddr;
    }
    
    /**
     * 
     */
    public SocketAddress getBindAddress() {
        return bindaddr;
    }
    
    /**
     * 
     */
    public synchronized DatagramSocket getDatagramSocket() {
        return socket;
    }
    
    @Override
    public synchronized void bind(Callback callback) throws IOException {
        if (!open) {
            throw new IOException();
        }
        
        if (future != null && !future.isDone()) {
            throw new IOException();
        }
            
        super.bind(callback);
        
        socket = new DatagramSocket(bindaddr);
        socket.setReuseAddress(true);
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                loop();
            }
        };
        
        future = executor.submit(task);
    }
    
    @Override
    public synchronized void unbind() {
        super.unbind();
        
        if (socket != null) {
            socket.close();
            socket = null;
        }
        
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }

    @Override
    public synchronized void close() throws IOException {
        open = false;
        unbind();
    }
    
    @Override
    public void send(final SocketAddress dst, final byte[] message, 
            final int offset, final int length) throws IOException {
        
        final DatagramSocket socket = this.socket;
        
        if (isBound() && socket != null && !socket.isClosed()) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        DatagramPacket packet = new DatagramPacket(
                                message, offset, length, dst);
                        
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
            DatagramSocket socket = null;
            while (open && (socket = this.socket) != null && !socket.isClosed()) {
                DatagramPacket packet = receive(
                        socket, PACKET_SIZE);
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
    private static DatagramPacket receive(
            DatagramSocket socket, int size) throws IOException {
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
                        handleMessage(src, data, offset, length);
                    } catch (IOException err) {
                        LOG.error("IOException", err);
                    }
                }
            };
            
            executor.execute(task);
        }
    }
}
