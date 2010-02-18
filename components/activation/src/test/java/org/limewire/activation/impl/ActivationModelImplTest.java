package org.limewire.activation.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.listener.EventListener;
import org.limewire.util.BaseTestCase;

public class ActivationModelImplTest extends BaseTestCase {
    
    private ActivationModel model;
    private ActivationItemFactory factory;
    
    public ActivationModelImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ActivationModelImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        model = new ActivationModelImpl(Executors.newSingleThreadExecutor());
        factory = new ActivationItemFactoryImpl();
    }
    
    public void testSingleModule() {
        assertEquals(0, model.size());
        assertEquals(0, model.getActivationItems().size());
        assertFalse(model.isActive(ActivationID.AVG_MODULE));
        assertFalse(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        
        ActivationItem item = factory.createActivationItem(1, "Downloads", 
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), ActivationItem.Status.ACTIVE);
        List<ActivationItem> items = new ArrayList<ActivationItem>();
        items.add(item);
        
        model.setActivationItems(items);
        
        assertEquals(1, model.size());
        assertEquals(1, model.getActivationItems().size());
        assertFalse(model.isActive(ActivationID.AVG_MODULE));
        assertTrue(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
    }
    
    public void testListenersAddEvent() throws TimeoutException {
        assertEquals(0, model.size());
        assertEquals(0, model.getActivationItems().size());
        assertFalse(model.isActive(ActivationID.AVG_MODULE));
        assertFalse(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        
        ActivationItem item = factory.createActivationItem(1, "Downloads", 
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), ActivationItem.Status.ACTIVE);
        List<ActivationItem> items = new ArrayList<ActivationItem>();
        items.add(item);
        
        Listener listener = new Listener();
        model.addListener(listener);
        model.setActivationItems(items);
        
        List<ActivationModuleEvent> events = listener.waitForEventsOrTimeout(1, 200);
        assertEquals(1, events.size());
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, events.get(0).getData());
    }
    
    public void testListenersUpdateEvent() throws TimeoutException {
        System.out.println("In testListenersUpdateEvent...");
        assertEquals(0, model.size());
        assertEquals(0, model.getActivationItems().size());
        assertFalse(model.isActive(ActivationID.AVG_MODULE));
        assertFalse(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));

        ActivationItem downloads = factory.createActivationItem(1, "Downloads", 
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), ActivationItem.Status.ACTIVE);
        ActivationItem tech = factory.createActivationItem(3, "Tech support", 
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), ActivationItem.Status.ACTIVE);
        
        //add AVG Module to model
        List<ActivationItem> items = new ArrayList<ActivationItem>();
        items.add(tech);

        //add listener
        Listener listener = new Listener();
        model.addListener(listener);
        
        model.setActivationItems(items);
        assertEquals(1, listener.waitForEventsOrTimeout(1, 200).size());
        assertEquals(1, model.size());
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertFalse(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        
        items.clear();
        items.add(downloads);
        
        // replace AVG module with Downloads Module
        model.setActivationItems(items);
        
        List<ActivationModuleEvent> events = listener.waitForEventsOrTimeout(2, 400);
        assertEquals(2, events.size());
        assertEquals(ActivationID.TECH_SUPPORT_MODULE, events.get(0).getData());
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, events.get(1).getData());
        
        assertFalse(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertTrue(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
    }
    
    public void testNoEventFired() throws Exception {
        assertEquals(0, model.size());
        assertEquals(0, model.getActivationItems().size());
        assertFalse(model.isActive(ActivationID.TECH_SUPPORT_MODULE));

        ActivationItem tech = factory.createActivationItem(3, "Tech support", 
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), ActivationItem.Status.ACTIVE);
        
        List<ActivationItem> items = new ArrayList<ActivationItem>();
        items.add(tech);

        // add listener
        Listener listener = new Listener();
        model.addListener(listener);
        model.setActivationItems(items);
        assertEquals(1, listener.waitForEventsOrTimeout(1, 200).size());
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        
        // replace AVG module with Downloads Module
        //readd the same module as activated
        model.setActivationItems(items);
        
        List<ActivationModuleEvent> events = new ArrayList<ActivationModuleEvent>();
        try {
            events.addAll(listener.waitForEventsOrTimeout(2, 200));
            fail("Expected timeout");
        } catch (TimeoutException e) {
            // expected timeout exception
        }
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertEquals(0, events.size());
    }
    
    public void testOneEventFiredOnTwoUpdates() throws TimeoutException {
        assertEquals(0, model.size());
        assertEquals(0, model.getActivationItems().size());
        assertFalse(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        assertFalse(model.isActive(ActivationID.TECH_SUPPORT_MODULE));

        ActivationItem downloads = factory.createActivationItem(1, "Downloads", 
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), ActivationItem.Status.ACTIVE);
        ActivationItem tech = factory.createActivationItem(3, "Tech support", 
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), ActivationItem.Status.ACTIVE);
        
        List<ActivationItem> items = new ArrayList<ActivationItem>();
        items.add(tech);

        Listener listener = new Listener();
        model.addListener(listener);
        model.setActivationItems(items);
        assertEquals(1, listener.waitForEventsOrTimeout(1, 200).size());
        assertFalse(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        
        // Replace AVG module with Dowloads Module
        // Read the same module as activated
        items.add(downloads);
        model.setActivationItems(items);
        List<ActivationModuleEvent> events = listener.waitForEventsOrTimeout(1, 200);
        
        assertTrue(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertEquals(1, events.size());
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, events.get(0).getData());
    }
    
    public void testTwoEventsFiredOnThreeUpdates() throws TimeoutException {
        assertEquals(0, model.size());
        assertEquals(0, model.getActivationItems().size());
        assertFalse(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        assertFalse(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertFalse(model.isActive(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE));

        ActivationItem downloads = factory.createActivationItem(1, "Downloads", 
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), ActivationItem.Status.ACTIVE);
        ActivationItem search = factory.createActivationItem(2, "Search", 
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), ActivationItem.Status.ACTIVE);
        ActivationItem tech = factory.createActivationItem(3, "Tech support", 
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), ActivationItem.Status.ACTIVE);
        
        List<ActivationItem> items = new ArrayList<ActivationItem>();
        items.add(tech);
        items.add(search);

        Listener listener = new Listener();
        model.addListener(listener);
        model.setActivationItems(items);
        assertEquals(2, listener.waitForEventsOrTimeout(2, 200).size());
        
        assertFalse(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertTrue(model.isActive(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE));
        
        // Replace AVG module with Downloads Module
        //readd the same module as activated
        items.add(downloads);
        items.remove(search);
        model.setActivationItems(items);
        
        List<ActivationModuleEvent> events = listener.waitForEventsOrTimeout(2, 200);
        assertTrue(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertFalse(model.isActive(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE));
        assertEquals(2, events.size());
        assertEquals(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE, events.get(0).getData());
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, events.get(1).getData());
        assertEquals(ActivationItem.Status.EXPIRED, events.get(0).getStatus());
        assertEquals(ActivationItem.Status.ACTIVE, events.get(1).getStatus());
    }
    
    private class Listener implements EventListener<ActivationModuleEvent> {

        private final List<ActivationModuleEvent> events = new ArrayList<ActivationModuleEvent>();
        private int startingSize = 0;
        
        @Override
        public synchronized void handleEvent(ActivationModuleEvent event) {
            events.add(event);
            notifyAll();
        }


        public synchronized List<ActivationModuleEvent> waitForEventsOrTimeout(int numEventsToOccur, long milliSeconds) 
        throws TimeoutException {
            
            long before = System.currentTimeMillis();
            long after;
            int totalNumEventsExpected = startingSize + numEventsToOccur;
            while (events.size() < totalNumEventsExpected) {
                try {
                    wait(milliSeconds);
                } catch (InterruptedException e) {
                    // ignoring exception
                }
                after = System.currentTimeMillis();
                if ((after - before) > milliSeconds) {
                    throw new TimeoutException("Timed out waiting for " + numEventsToOccur + " events to occur.");
                }
            }
            List<ActivationModuleEvent> newEvents = new ArrayList<ActivationModuleEvent>();
            int totalNumEvents = events.size();
            if (totalNumEvents > startingSize) {
                newEvents.addAll(events.subList(startingSize, totalNumEvents));   
            }
            startingSize = totalNumEvents;
            return newEvents;
        }
    }

}
