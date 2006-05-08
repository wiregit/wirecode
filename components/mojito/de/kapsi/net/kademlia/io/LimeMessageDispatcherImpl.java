/*
 * Lime Kademlia Distributed Hash Table (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.kapsi.net.kademlia.io;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.ReadWriteObserver;
import com.limegroup.gnutella.util.ProcessingQueue;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.messages.Message;

public class LimeMessageDispatcherImpl extends MessageDispatcher implements ReadWriteObserver {

    private static final Log LOG = LogFactory.getLog(LimeMessageDispatcherImpl.class);
    
    private static final long CLEANUP = 3L * 1000L;
    
    private ProcessingQueue processQueue = new ProcessingQueue("LimeDHTMessageDispatcherQueue", true);
    
    private boolean running = false;
    
    public LimeMessageDispatcherImpl(Context context) {
        super(context);
        
        context.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (isRunning()) {
                    handleClenup();
                }
            }
        }, CLEANUP, CLEANUP);
    }

    protected boolean allow(Message message) {
        return true;
    }

    public void bind(SocketAddress address) throws IOException {
        if (isOpen()) {
            throw new IOException("Already open");
        }
        
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        
        DatagramSocket socket = channel.socket();
        socket.setReuseAddress(false);
        socket.setReceiveBufferSize(Message.MAX_MESSAGE_SIZE);
        socket.setSendBufferSize(Message.MAX_MESSAGE_SIZE);
        
        socket.bind(address);
        
        setDatagramChannel(channel);
    }

    public void stop() {
        running = false;
        setDatagramChannel(null);
    }
    
    protected void interestRead(boolean on) {
        NIODispatcher.instance().interestRead(getDatagramChannel(), on);
    }

    protected void interestWrite(boolean on) {
        NIODispatcher.instance().interestWrite(getDatagramChannel(), on);
    }

    public boolean isRunning() {
        return running;
    }

    protected void process(Runnable runnable) {
        processQueue.add(runnable);
    }

    public void shutdown() {
        stop();
    }

    public void handleIOException(IOException iox) {
        LOG.error(iox);
    }
    
    public void run() {
        running = true;
        
        DatagramChannel channel = getDatagramChannel();
        if (channel != null) {
            NIODispatcher.instance().registerReadWrite(channel, this);
        }
    }
}
