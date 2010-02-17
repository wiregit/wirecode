package org.limewire.activation.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

import junit.framework.Test;

import org.limewire.activation.api.ActivationEvent;
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
    
    public void testListenersAddEvent() {
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
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail();
        }
        
        assertEquals(1, listener.events.size());
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, listener.events.get(0).getData());
    }
    
    public void testListenersUpdateEvent() {
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

        model.setActivationItems(items);
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail();
        }
        
        assertEquals(1, model.size());
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertFalse(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        
        items.clear();
        items.add(downloads);
        
        //add listener and replace AVG module with Dwonloads Module
        Listener listener = new Listener();
        model.addListener(listener);
        model.setActivationItems(items);
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail();
        }
        
        assertEquals(2, listener.events.size());
        assertEquals(ActivationID.TECH_SUPPORT_MODULE, listener.events.get(0).getData());
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, listener.events.get(1).getData());
        
        assertFalse(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertTrue(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
    }
    
    public void testNoEventFired() {
        assertEquals(0, model.size());
        assertEquals(0, model.getActivationItems().size());
        assertFalse(model.isActive(ActivationID.TECH_SUPPORT_MODULE));

        ActivationItem tech = factory.createActivationItem(3, "Tech support", 
                new Date(System.currentTimeMillis()), new Date(System.currentTimeMillis()), ActivationItem.Status.ACTIVE);
        
        List<ActivationItem> items = new ArrayList<ActivationItem>();
        items.add(tech);

        model.setActivationItems(items);
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail();
        }
        
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        
        //add listener and replace AVG module with Dwonloads Module
        Listener listener = new Listener();
        model.addListener(listener);
        
        //readd the same module as activated
        model.setActivationItems(items);
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail();
        }
        
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertEquals(0, listener.events.size());
    }
    
    public void testOneEventFiredOnTwoUpdates() {
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

        model.setActivationItems(items);
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail();
        }
        
        assertFalse(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        
        //add listener and replace AVG module with Dwonloads Module
        Listener listener = new Listener();
        model.addListener(listener);
        
        //readd the same module as activated
        items.add(downloads);
        model.setActivationItems(items);
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail();
        }
        
        assertTrue(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertEquals(1, listener.events.size());
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, listener.events.get(0).getData());
    }
    
    public void testTwoEventsFiredOnThreeUpdates() {
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

        model.setActivationItems(items);
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail();
        }
        
        assertFalse(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertTrue(model.isActive(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE));
        
        //add listener and replace AVG module with Dwonloads Module
        Listener listener = new Listener();
        model.addListener(listener);
        
        //readd the same module as activated
        items.add(downloads);
        items.remove(search);
        model.setActivationItems(items);
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail();
        }
        
        assertTrue(model.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE));
        assertTrue(model.isActive(ActivationID.TECH_SUPPORT_MODULE));
        assertFalse(model.isActive(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE));
        assertEquals(2, listener.events.size());
        assertEquals(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE, listener.events.get(0).getData());
        assertEquals(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE, listener.events.get(1).getData());
        assertEquals(ActivationItem.Status.EXPIRED, listener.events.get(0).getStatus());
        assertEquals(ActivationItem.Status.ACTIVE, listener.events.get(1).getStatus());
    }
    
    private class Listener implements EventListener<ActivationModuleEvent> {

        List<ActivationModuleEvent> events = new ArrayList<ActivationModuleEvent>();
        
        @Override
        public void handleEvent(ActivationModuleEvent event) {
            events.add(event);
        }
    }

}
