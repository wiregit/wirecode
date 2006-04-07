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
 
package de.kapsi.net.kademlia.event;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import de.kapsi.net.kademlia.Context;

/**
 * A simple event dispatcher that runs asynchronously to MessageDispatcher.
 */
public class EventDispatcher extends TimerTask implements Runnable {

    private static final Logger LOG = LogManager.getLogger(EventDispatcher.class);
   
    private final Object LOCK = new Object();
    private List events = new ArrayList();
    
    private Context context;
    
    public EventDispatcher(Context context) {
        this.context = context;
    }
    
    public void add(Runnable event) {
        if (event == null) {
            LOG.error("Discarding Event as it is null");
            return;
        }
        
        if (!context.isRunning()) {
            LOG.info("Discarding Event as the Context is not running");
            return;
        }
        
        synchronized(LOCK) {
            events.add(event);
        }
    }
    
    public void run() {
        List dispatch = null;
        int size = 0;
        
        synchronized(LOCK) {
            dispatch = events;
            size = dispatch.size();
            
            events = new ArrayList(Math.max(10, size));
        }
        
        for(int i = 0; i < size; i++) {
            try {
                ((Runnable)dispatch.get(i)).run();
            } catch (Throwable t) {
                t.printStackTrace();
                System.out.println("Class: "+((Runnable)dispatch.get(i)).getClass().getName());
                LOG.error(t);
            }
        }
        dispatch = null;    
    }
    
    public String toString() {
        return "EventDispatcher";
    }
}
