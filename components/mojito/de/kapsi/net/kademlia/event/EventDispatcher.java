/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class EventDispatcher extends TimerTask implements Runnable {

    private static final Logger LOG = LogManager.getLogger(EventDispatcher.class);
   
    private final Object LOCK = new Object();
    private List events = new ArrayList();
    
    private boolean running = false;
    
    public void add(Runnable event) {
        if (event == null) {
            LOG.error("Discarding Event as it is null");
            return;
        }
        
        if (!running) {
            LOG.info("Discarding Event as the EventDispatcher is not running");
            return;
        }
        
        synchronized(LOCK) {
            events.add(event);
        }
    }
    
    public boolean cancel() {
        running = false;
        return super.cancel();
    }
    
    public void run() {
        running = true;
        List dispatch = null;
        int size = 0;
        
        synchronized(LOCK) {
            dispatch = events;
            size = dispatch.size();
            
            events = new ArrayList(Math.max(10, size));
        }
        
        for(int i = 0; i < size; i++) {
            try {
                // TODO profile which is better!
                //((Runnable)dispatch.set(i, null)).run();
                ((Runnable)dispatch.get(i)).run();
            } catch (Throwable t) {
                t.printStackTrace();
                LOG.error(t);
            }
        }
        dispatch = null;    
    }
    
    public String toString() {
        return "EventDispatcherTask";
    }
}
