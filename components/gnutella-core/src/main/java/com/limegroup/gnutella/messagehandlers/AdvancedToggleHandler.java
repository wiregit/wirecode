package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;

import org.limewire.collection.Periodic;
import org.limewire.statistic.StatisticsManager;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.AdvancedStatsToggle;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.FilterSettings;

/**
 * Handles incoming toggles for advanced stats and takes care
 * of turning them off.
 * 
 * Only messages coming from the inspector ips can turn
 * stats on. 
 */
public class AdvancedToggleHandler extends RestrictedResponder {

    /** Max time to keep stats on */
    private static int MAX_TIME = 60 * 60 * 1000;
    
    /** 
     * Whether stats were turned on as a response to a previous message
     * as opposed to the user turning them on.
     */
    private boolean on;
    
    /** Utility that will perform the shutting off. */
    private final Periodic shutOff;
    
    public AdvancedToggleHandler(NetworkManager networkManager) {
        super(FilterSettings.INSPECTOR_IP_ADDRESSES, networkManager);
        
        shutOff = new Periodic(new Runnable() {
            public void run (){
                synchronized(AdvancedToggleHandler.this) {
                    if (on && !StatisticsManager.instance().getRecordAdvancedStatsManual()) {
                        StatisticsManager.instance().setRecordAdvancedStats(false);
                        on = false;
                    }
                }
            }
        }, RouterService.getScheduledExecutorService());
    }
    
    @Override
    protected synchronized void processAllowedMessage(Message msg, InetSocketAddress addr,
            ReplyHandler handler) {
        AdvancedStatsToggle toggle = (AdvancedStatsToggle)msg;
        
        // if not allowed, don't process.
        if (!ApplicationSettings.USAGE_STATS.getValue())
            return;
        
        // if advanced stats are already turned on by the user
        // do not mess
        if (StatisticsManager.instance().getRecordAdvancedStatsManual())
            return;
        
        // if this is a shut off and we turned stats on, stop them.
        if (toggle.shutOffNow()) {
            if (on) {
                StatisticsManager.instance().setRecordAdvancedStats(false);
                on = false;
                shutOff.unschedule();
            }
            return;
        }
        
        
        int time = Math.min(MAX_TIME, toggle.getTime());
        
        // otherwise, turn the stats on and (re)schedule the shut off.
        on = true;
        StatisticsManager.instance().setRecordAdvancedStats(true);
        shutOff.rescheduleIfLater(time);
    }
}
