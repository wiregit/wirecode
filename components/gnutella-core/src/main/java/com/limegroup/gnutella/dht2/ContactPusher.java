package com.limegroup.gnutella.dht2;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.Buffer;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.DHTSettings;
import org.limewire.mojito2.routing.Contact;

import com.google.inject.Provider;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.connection.ConnectionCapabilities;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

class ContactPusher implements Closeable {
    
    private static final ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory("ContactPusherThread"));
    
    /**
     * Contacts to forward to passive leafs.
     */
    private final Buffer<Contact> contactsToForward 
        = new Buffer<Contact>(10);
    
    private final Provider<ConnectionManager> connectionManager;
    
    private final long frequency;
    
    private final TimeUnit unit;
    
    private ScheduledFuture<?> future = null;
    
    private boolean open = true;
    
    public ContactPusher(Provider<ConnectionManager> connectionManager) {
        this (connectionManager, 60, TimeUnit.SECONDS);
    }
    
    public ContactPusher(Provider<ConnectionManager> connectionManager, 
            long frequency, TimeUnit unit) {
        
        this.connectionManager = connectionManager;
        this.frequency = frequency;
        this.unit = unit;
    }
    
    @Override
    public synchronized void close() {
        open = false;
        
        if (future != null) {
            future.cancel(true);
        }
    }
    
    public synchronized void addContact(Contact contact) {
        if (!open || !DHTSettings.ENABLE_PASSIVE_LEAF_DHT_MODE.getValue()) {
            return;
        }
        
        contactsToForward.add(contact);
        
        if (future == null || future.isDone()) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    push();
                }
            };
            
            future = EXECUTOR.scheduleWithFixedDelay(
                    task, frequency, frequency, unit);
        }
    }
    
    private void push() {
        List<Contact> contacts = null;
        synchronized (this) {
            if (contactsToForward.isEmpty()) {
                future.cancel(true);
                return;
            }
            
            contacts = new ArrayList<Contact>(10);
            for (Contact c : contactsToForward) {
                contacts.add(c);
            }
        }
        
        DHTContactsMessage msg = new DHTContactsMessage(
                contacts.toArray(new Contact[0]));
        
        List<RoutedConnection> list 
            = connectionManager.get().getInitializedClientConnections();
        
        for (RoutedConnection mc : list) {
            ConnectionCapabilities capabilities 
                = mc.getConnectionCapabilities();
            
            if (mc.isPushProxyFor() && capabilities.remoteHostIsPassiveLeafNode() > -1) {
                mc.send(msg);
            }
        }
    }
}