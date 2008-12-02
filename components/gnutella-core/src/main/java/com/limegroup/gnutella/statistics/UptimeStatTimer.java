package com.limegroup.gnutella.statistics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Timer;

import org.limewire.core.api.lifecycle.LifeCycleEvent;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.listener.EventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This class handles the timer that updates uptime statistics.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
@Singleton
public final class UptimeStatTimer implements EventListener<LifeCycleEvent>  {

    /**
     * The interval between statistics updates in milliseconds.
     */
    private final int UPDATE_TIME = 1000;

    /**
     * The interval between statistics updates in seconds for convenience and
     * added efficiency..
     */
    private final int UPDATE_TIME_IN_SECONDS = UPDATE_TIME / 1000;

    /**
     * variable for timer that updates the stats.
     */
    private Timer _timer;

    /**
     * Current uptime in seconds.
     */
    @InspectablePrimitive("currentUptime")
    private static volatile long currentUptime = 0;

    /**
     * The interval in seconds at which to update the history of the last n
     * uptimes.
     */
    private static final int UPTIME_HISTORY_SNAPSHOT_INTERVAL = 60;

    /**
     * The number of uptimes to remember.
     */
    private static final int LAST_N_UPTIMES = 20;

    private final AtomicBoolean firstUtimeUpdate = new AtomicBoolean(true);

    /**
     * Creates the timer and the ActionListener associated with it.
     */
    @Inject
    public UptimeStatTimer(LifeCycleManager lifeCycleManager) {
        ActionListener refreshStats = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshStats();
            }
        };

        _timer = new Timer(UPDATE_TIME, refreshStats);
        
        if(lifeCycleManager.isStarted()) {
            startTimer();
        } else {
            lifeCycleManager.addListener(this);
        }
    }

    /**
     * Starts the timer that updates the statistics.
     */
    private void startTimer() {
        _timer.start();
    }

    /**
     * Refreshes all of uptime statistics.
     */
    private void refreshStats() {
        currentUptime += UPDATE_TIME_IN_SECONDS;

        int totalUptime = ApplicationSettings.TOTAL_UPTIME.getValue() + UPDATE_TIME_IN_SECONDS;
        ApplicationSettings.TOTAL_UPTIME.setValue(totalUptime);
        ApplicationSettings.AVERAGE_UPTIME.setValue(totalUptime
                / ApplicationSettings.SESSIONS.getValue());

        updateUptimeHistory(currentUptime, UPTIME_HISTORY_SNAPSHOT_INTERVAL, LAST_N_UPTIMES);
    }

    /**
     * Updates the uptime history with the current uptime.
     * 
     * @param currentUptime the current uptime in seconds
     * @param interval the interval at which to update the history
     * @param historyLength the number of entries to remember in the history
     */
    void updateUptimeHistory(long currentUptime, int interval, int historyLength) {
        if (currentUptime == 0) {
            return;
        }
        // only update setting every so many seconds
        if (currentUptime % interval == 0) {
            String[] lastNUptimes = ApplicationSettings.LAST_N_UPTIMES.getValue();
            // first time this session
            if (firstUtimeUpdate.getAndSet(false)) {
                String[] copy;
                if (lastNUptimes.length < historyLength) {
                    copy = new String[lastNUptimes.length + 1];
                    System.arraycopy(lastNUptimes, 0, copy, 0, lastNUptimes.length);
                } else {
                    copy = new String[historyLength];
                    System.arraycopy(lastNUptimes, 1, copy, 0, copy.length - 1);
                }
                copy[copy.length - 1] = Long.toString(currentUptime);
                ApplicationSettings.LAST_N_UPTIMES.setValue(copy);
            } else {
                // very defensive, should never happen
                if (lastNUptimes.length == 0) {
                    ApplicationSettings.LAST_N_UPTIMES.setValue(new String[] { Long
                            .toString(currentUptime) });
                } else {
                    lastNUptimes[lastNUptimes.length - 1] = Long.toString(currentUptime);
                    ApplicationSettings.LAST_N_UPTIMES.setValue(lastNUptimes);
                }
            }
        }
    }

    @Override
    public void handleEvent(LifeCycleEvent event) {
       if(LifeCycleEvent.STARTED == event) {
           startTimer();
       }
    }
}
