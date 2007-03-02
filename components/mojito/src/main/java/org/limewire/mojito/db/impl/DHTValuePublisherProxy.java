/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

package org.limewire.mojito.db.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFutureListener;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueEntityPublisher;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;

/**
 * The DHTValuePublisherProxy proxies multiple DHTValueEntityPublisher
 * instances. In other words you may use it to combine multiple data
 * sources.
 */
public class DHTValuePublisherProxy implements DHTValueEntityPublisher, DHTFutureListener<StoreResult> {

    private final Set<DHTValueEntityPublisher> proxy
        = Collections.synchronizedSet(new LinkedHashSet<DHTValueEntityPublisher>());
    
    /**
     * Adds the given DHTValueEntityPublisher to the proxy
     */
    public void add(DHTValueEntityPublisher publisher) {
        if (publisher == null) {
            throw new NullPointerException("DHTValueEntityPublisher is null");
        }
        proxy.add(publisher);
    }
    
    /**
     * Removes the given DHTValueEntityPublisher from the proxy
     */
    public void remove(DHTValueEntityPublisher publisher) {
        if (publisher == null) {
            throw new NullPointerException("DHTValueEntityPublisher is null");
        }
        
        proxy.remove(publisher);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#get(org.limewire.mojito.KUID)
     */
    public DHTValueEntity get(KUID primaryKey) {
        DHTValueEntity entity = null;
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                entity = publisher.get(primaryKey);
                if (entity != null) {
                    return entity;
                }
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#values()
     */
    public Collection<DHTValueEntity> getValues() {
        Collection<DHTValueEntity> values = new ArrayList<DHTValueEntity>();
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                values.addAll(publisher.getValues());
            }
        }
        return values;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#values()
     */
    public Collection<DHTValueEntity> getValuesToPublish() {
        Collection<DHTValueEntity> publish = new ArrayList<DHTValueEntity>();
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                publish.addAll(publisher.getValuesToPublish());
            }
        }
        return publish;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#getValuesToForward()
     */
    public Collection<DHTValueEntity> getValuesToForward() {
        Collection<DHTValueEntity> forward = new ArrayList<DHTValueEntity>();
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                forward.addAll(publisher.getValuesToForward());
            }
        }
        return forward;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#handleContactChange(org.limewire.mojito.routing.Contact)
     */
    public void changeContact(Contact node) {
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                publisher.changeContact(node);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTFutureListener#handleFutureSuccess(java.lang.Object)
     */
    public void handleFutureSuccess(StoreResult result) {
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                if (publisher instanceof DHTFutureListener) {
                    ((DHTFutureListener<StoreResult>)publisher).handleFutureSuccess(result);
                }
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTFutureListener#handleFutureCancelled(java.util.concurrent.CancellationException)
     */
    public void handleCancellationException(CancellationException e) {
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                if (publisher instanceof DHTFutureListener) {
                    ((DHTFutureListener<StoreResult>)publisher).handleCancellationException(e);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTFutureListener#handleFutureFailure(java.util.concurrent.ExecutionException)
     */
    public void handleExecutionException(ExecutionException e) {
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                if (publisher instanceof DHTFutureListener) {
                    ((DHTFutureListener<StoreResult>)publisher).handleExecutionException(e);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTFutureListener#handleFutureInterrupted(java.lang.InterruptedException)
     */
    public void handleInterruptedException(InterruptedException e) {
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                if (publisher instanceof DHTFutureListener) {
                    ((DHTFutureListener<StoreResult>)publisher).handleInterruptedException(e);
                }
            }
        }
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                buffer.append(publisher.toString()).append("\n");
            }
        }
        return buffer.toString();
    }
}
