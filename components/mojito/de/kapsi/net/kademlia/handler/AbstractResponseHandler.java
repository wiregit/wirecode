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
 
package de.kapsi.net.kademlia.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.ResponseListener;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.settings.NetworkSettings;

public abstract class AbstractResponseHandler extends MessageHandler 
        implements ResponseHandler {
    
    private int errors = 0;
    
    private long time;
    private long timeout;
    
    private int maxErrors;
    
    protected final List listeners = new ArrayList();
    
    private volatile boolean stop = false;
    
    public AbstractResponseHandler(Context context) {
        this(context, -1L, -1);
    }
    
    public AbstractResponseHandler(Context context, long timeout) {
        this(context, timeout, -1);
    }
    
    public AbstractResponseHandler(Context context, int maxErrors) {
        this(context, -1L, maxErrors);
    }
    
    public AbstractResponseHandler(Context context, long timeout, int maxErrors) {
        super(context);
        
        if (timeout < 0L) {
            this.timeout = NetworkSettings.MAX_TIMEOUT.getValue();
        } else {
            this.timeout = timeout;
        }
        
        if (maxErrors < 0) {
            if (NetworkSettings.USE_RANDOM_MAX_ERRORS.getValue()) {
                // MIN_RETRIES <= x <= MAX_ERRORS
                this.maxErrors = Math.max(NetworkSettings.MIN_RETRIES.getValue(), 
                        (int)(Math.random() * NetworkSettings.MAX_ERRORS.getValue()));
            } else {
                this.maxErrors = NetworkSettings.MAX_ERRORS.getValue();
            }
        } else {
            this.maxErrors = maxErrors;
        }
    }
    
    public void stop() {
        stop = true;
    }
    
    public boolean isStopped() {
        return stop;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public long timeout() {
        return timeout;
    }
    
    public void addTime(long time) {
        this.time += time;
    }
    
    public long time() {
        return time;
    }
    
    protected void resetErrors() {
        errors = 0;
    }
    
    public int getErrors() {
        return errors;
    }
    
    public void setMaxErrors(int maxErrors) {
        this.maxErrors = maxErrors;
    }
    
    public int getMaxErrors() {
        return maxErrors;
    }
    
    public void handleResponse(ResponseMessage response, long time) throws IOException {
        
        response(response, time);
        fireResponse(response, time);
    }

    public void handleTimeout(KUID nodeId, 
            SocketAddress dst, RequestMessage request, long time) throws IOException {
        
        if (!isStopped()) {
            if (errors++ >= maxErrors) {
                timeout(nodeId, dst, request, time());
                fireTimeout(nodeId, dst, request, time());
            } else {
                resend(nodeId, dst, request);
            }
        } else {
            timeout(nodeId, dst, request, time());
        }
    }
    
    protected void resend(KUID nodeId, SocketAddress dst, RequestMessage message) throws IOException {
        context.getMessageDispatcher().send(nodeId, dst, message, this);
    }
    
    protected abstract void response(ResponseMessage message, long time) throws IOException;
    
    protected abstract void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException;
    
    protected void fireResponse(final ResponseMessage response, final long time) {
        context.fireEvent(new Runnable() {
            public void run() {
                if (!isStopped()) {
                    for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                        ResponseListener listener = (ResponseListener)it.next();
                        listener.response(response, time);
                    }
                }
            }
        });
    }
    
    protected void fireTimeout(final KUID nodeId, final SocketAddress address, 
            final RequestMessage request, final long time) {
        context.fireEvent(new Runnable() {
            public void run() {
                if (!isStopped()) {
                    for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                        ResponseListener listener = (ResponseListener)it.next();
                        listener.timeout(nodeId, address, request, time);
                    }
                }
            }
        });
    }
}
