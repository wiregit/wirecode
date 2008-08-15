package com.limegroup.gnutella.gui;

import junit.framework.Test;

import org.limewire.setting.StringArraySetting;

import com.limegroup.gnutella.settings.ApplicationSettings;


public class RefreshTimerTest extends GUIBaseTestCase {

    public RefreshTimerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RefreshTimerTest.class);
    }
    
    public void testUpdateUptimeHistory() {
        RefreshTimer refreshTimer = new RefreshTimer();
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
        refreshTimer = new RefreshTimer();
        refreshTimer.updateUptimeHistory(10, 10, 2);
        assertEquals(new String[] { "20", "10" }, uptimeHistory.getValue());
        
        // new first time update should shift array
        refreshTimer = new RefreshTimer();
        refreshTimer.updateUptimeHistory(10, 10, 2);
        assertEquals(new String[] { "10", "10" }, uptimeHistory.getValue());
        
        // regular update, should replace
        refreshTimer.updateUptimeHistory(30, 10, 2);
        assertEquals(new String[] { "10", "30" }, uptimeHistory.getValue());
        
        // go back to shorter history length
        refreshTimer = new RefreshTimer();
        refreshTimer.updateUptimeHistory(10, 10, 1);
        assertEquals(new String[] { "10" }, uptimeHistory.getValue());
                
        refreshTimer.updateUptimeHistory(20, 10, 1);
        assertEquals(new String[] { "20" }, uptimeHistory.getValue());
        
        // now with longer history, but not first interval
        refreshTimer.updateUptimeHistory(30, 10, 2);
        assertEquals(new String[] { "30" }, uptimeHistory.getValue());
    }

    public void testUpdateUptimeHistoryWithFirstUptimeGreaterThanInterval() {
       RefreshTimer refreshTimer = new RefreshTimer();
       StringArraySetting uptimeHistory = ApplicationSettings.LAST_N_UPTIMES;
       assertEquals(new String[0], uptimeHistory.getValue());
      
       refreshTimer.updateUptimeHistory(20, 10, 2);
       assertEquals(new String[] { "20" }, uptimeHistory.getValue());
       
       refreshTimer.updateUptimeHistory(30, 10, 2);
       assertEquals(new String[] { "30" }, uptimeHistory.getValue());
    }

    /**
     * Ensures array copying works for history length 1.
     */
    public void testHistoryLength1() {
        RefreshTimer refreshTimer = new RefreshTimer();
        
        refreshTimer.updateUptimeHistory(10, 10, 1);
        
        refreshTimer = new RefreshTimer();
        refreshTimer.updateUptimeHistory(20, 10, 1);
    }
    
    public void testHistoryLengthChangedFrom1ToHigher() {
        RefreshTimer refreshTimer = new RefreshTimer();
        
        refreshTimer.updateUptimeHistory(10, 10, 1);
        
        refreshTimer = new RefreshTimer();
        refreshTimer.updateUptimeHistory(20, 10, 2);
    }
    
}
