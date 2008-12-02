package com.limegroup.gnutella.statistics;

import junit.framework.Test;

import org.limewire.core.api.lifecycle.LifeCycleEvent;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.listener.EventListener;
import org.limewire.setting.StringArraySetting;

import com.limegroup.gnutella.util.LimeTestCase;


public class UptimeStatTimerTest extends LimeTestCase {

    public UptimeStatTimerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UptimeStatTimerTest.class);
    }
    
    public void testUpdateUptimeHistory() {
        UptimeStatTimer refreshTimer = new UptimeStatTimer(new NoOpLifeCycleManager());
        StringArraySetting uptimeHistory = ApplicationSettings.LAST_N_UPTIMES;
        assertEquals(new String[0], uptimeHistory.getValue());
        
        // ensure history is not updated
        for (int i = 0; i < 5; i++) { 
            refreshTimer.updateUptimeHistory(i, 10, 10);
            assertEquals(new String[0], uptimeHistory.getValue());
        }
        
        // test for initialization
        refreshTimer.updateUptimeHistory(10, 10, 2);
        assertEquals(new String[] { "10" }, uptimeHistory.getValue());
        
        // regular update, should replace old value
        refreshTimer.updateUptimeHistory(20, 10, 2);
        assertEquals(new String[] { "20" }, uptimeHistory.getValue());
        
        // new first time update, should append
        refreshTimer = new UptimeStatTimer(new NoOpLifeCycleManager());
        refreshTimer.updateUptimeHistory(10, 10, 2);
        assertEquals(new String[] { "20", "10" }, uptimeHistory.getValue());
        
        // new first time update should shift array
        refreshTimer = new UptimeStatTimer(new NoOpLifeCycleManager());
        refreshTimer.updateUptimeHistory(10, 10, 2);
        assertEquals(new String[] { "10", "10" }, uptimeHistory.getValue());
        
        // regular update, should replace
        refreshTimer.updateUptimeHistory(30, 10, 2);
        assertEquals(new String[] { "10", "30" }, uptimeHistory.getValue());
        
        // go back to shorter history length
        refreshTimer = new UptimeStatTimer(new NoOpLifeCycleManager());
        refreshTimer.updateUptimeHistory(10, 10, 1);
        assertEquals(new String[] { "10" }, uptimeHistory.getValue());
                
        refreshTimer.updateUptimeHistory(20, 10, 1);
        assertEquals(new String[] { "20" }, uptimeHistory.getValue());
        
        // now with longer history, but not first interval
        refreshTimer.updateUptimeHistory(30, 10, 2);
        assertEquals(new String[] { "30" }, uptimeHistory.getValue());
    }

    public void testUpdateUptimeHistoryWithFirstUptimeGreaterThanInterval() {
       UptimeStatTimer refreshTimer = new UptimeStatTimer(new NoOpLifeCycleManager());
       StringArraySetting uptimeHistory = ApplicationSettings.LAST_N_UPTIMES;
       assertEquals(new String[0], uptimeHistory.getValue());
      
       refreshTimer.updateUptimeHistory(20, 10, 2);
       assertEquals(new String[] { "20" }, uptimeHistory.getValue());
       
       refreshTimer.updateUptimeHistory(30, 10, 2);
       assertEquals(new String[] { "30" }, uptimeHistory.getValue());
    }
    
    private class NoOpLifeCycleManager implements LifeCycleManager {

        @Override
        public void addListener(EventListener<LifeCycleEvent> listener) {
            
        }

        @Override
        public boolean isLoaded() {
            return false;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isStarted() {
            return false;
        }

        @Override
        public boolean removeListener(EventListener<LifeCycleEvent> listener) {
            return false;
        }
        
    }
   
}
