package com.limegroup.gnutella.messagehandlers;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import junit.framework.Test;

import org.limewire.statistic.StatisticsManager;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.vendor.AdvancedStatsToggle;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class AdvancedToggleHandlerTest extends LimeTestCase {

    public AdvancedToggleHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AdvancedToggleHandlerTest.class);
    }    
    static InetSocketAddress addr;
    
    public void setUp() throws Exception {
        FilterSettings.INSPECTOR_IP_ADDRESSES.setValue(new String[]{"127.0.0.1"});
        ApplicationSettings.USAGE_STATS.setValue(true);
        addr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 1000);
        StatisticsManager.instance().setRecordAdvancedStatsManual(false);
        PrivilegedAccessor.setValue(AdvancedToggleHandler.class, "MAX_TIME", 60 * 60 * 1000);
    }
    
    /**
     * Tests that if the usage stats setting is off, the message does nothing.
     */
    public void testSettingRespected() throws Exception {
        ApplicationSettings.USAGE_STATS.setValue(false);
        AdvancedStatsToggle toggle = new AdvancedStatsToggle(100);
        AdvancedToggleHandler handler = new AdvancedToggleHandler();
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
        handler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
    }
    
    /**
     * Tests turning on, and after a while shutting off of stats.
     */
    public void testTurnOn() throws Exception {
        AdvancedStatsToggle toggle = new AdvancedStatsToggle(100);
        AdvancedToggleHandler handler = new AdvancedToggleHandler();
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
        handler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        Thread.sleep(120);
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
    }

    /**
     * Tests immediately shutting off stats with a toggle message.
     */
    public void testImmediateShutOff() throws Exception {
        AdvancedStatsToggle toggle = new AdvancedStatsToggle(500);
        AdvancedToggleHandler handler = new AdvancedToggleHandler();
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
        handler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        
        // send the shut offf toggle
        AdvancedStatsToggle toggleOff = new AdvancedStatsToggle(-1);
        handler.handleMessage(toggleOff, addr, new ReplyHandlerStub());
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
    }
    
    /**
     * Tests that if the user had turned the stats on, the stats will not
     * get shut off after the timeout
     */
    public void testUserOnNotSchedule() throws Exception {
        AdvancedStatsToggle toggle = new AdvancedStatsToggle(50);
        AdvancedToggleHandler handler = new AdvancedToggleHandler();
        StatisticsManager.instance().setRecordAdvancedStatsManual(true);
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        handler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());

        // time has expired, but stats are still on
        Thread.sleep(100);
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
    }
    
    /**
     * Tests that if the user had turned the stats on, they will not get
     * shut off by shutoff message
     */
    public void testUserOnNotShut() throws Exception {
        AdvancedStatsToggle toggle = new AdvancedStatsToggle(-1);
        AdvancedToggleHandler handler = new AdvancedToggleHandler();
        StatisticsManager.instance().setRecordAdvancedStatsManual(true);
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        handler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
    }
    
    /**
     * Tests that the time to keep stats can be extended.
     */
    public void testExtend() throws Exception {
        AdvancedStatsToggle toggle = new AdvancedStatsToggle(100);
        AdvancedToggleHandler handler = new AdvancedToggleHandler();
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
        handler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        
        // sleep some time, send another message
        Thread.sleep(80);
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        handler.handleMessage(toggle, addr, new ReplyHandlerStub());
        
        // now sleep more - it should not be off for another 100ms
        Thread.sleep(80);
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        
        // now it should be off.
        Thread.sleep(30);
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
    }
    
    /**
     * Tests that the stats cannot be turned on for more than the maximum
     * time.
     */
    public void testMaxTime() throws Exception {
        PrivilegedAccessor.setValue(AdvancedToggleHandler.class, "MAX_TIME", 100);
        AdvancedStatsToggle toggle = new AdvancedStatsToggle(1000);
        AdvancedToggleHandler handler = new AdvancedToggleHandler();
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
        handler.handleMessage(toggle, addr, new ReplyHandlerStub());
        assertTrue(StatisticsManager.instance().getRecordAdvancedStats());
        
        // the message asked for 1000ms, but after a 100 ms stats will be off.
        Thread.sleep(110);
        assertFalse(StatisticsManager.instance().getRecordAdvancedStats());
    }
}
