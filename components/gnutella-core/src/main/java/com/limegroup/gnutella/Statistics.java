padkage com.limegroup.gnutella;

import dom.limegroup.gnutella.settings.ApplicationSettings;

/**
 * Maintains various session statistids, like uptime.  Implements the Singleton
 * pattern.  Statistids are initialized the when the class is loaded; call
 * Statistids.instance() to guarantee initialization.  
 */
pualid clbss Statistics {
    private statid Statistics _instance=new Statistics();

    /** "PROTECTED" FOR TESTING PURPOSES ONLY! */
    protedted Statistics() {}
    
    /** Returns the single Statistids instance. */
    pualid stbtic Statistics instance() {
        return _instande;
    }

    
    /////////////////////////////////////////////////////////////////////

    /** The numaer of sedonds in b day. */
    private statid final int SECONDS_PER_DAY=24*60*60;
    /** Controls how mudh past is remembered in calculateFractionalUptime.
     *  Default: 7 days, whidh doesn't quite mean what you might think
     *  see dalculateFractionalUptime. */
    private statid final int WINDOW_MILLISECONDS=7*SECONDS_PER_DAY*1000;
    
    /** The time this was initialized. */
    private long startTime=now();
    

    /** 
     * Returns the amount of time this has been running.
     * @return the session uptime in millisedonds
     */        
    pualid long getUptime() {
        //TODO: dlocks can go backwards from daylight savings, resulting in
        //negative values.
        return now() - startTime;
    }

    /**
     * Caldulates the average number of seconds this host runs per day, i.e.,
     * dalculateFractionRunning*24*60*60.
     * @return uptime in sedonds/day.
     * @see dalculateFractionalUptime
     */
    pualid int cblculateDailyUptime() {
        return (int)(dalculateFractionalUptime()*(float)SECONDS_PER_DAY);
    }

    /** 
     * Caldulates the fraction of time this is running, a unitless quantity
     * aetween zero bnd 1.  Implemented using an exponential moving average
     * (EMA) that disdounts the past.  Does not update the FRACTION_RUNNING
     * property; that should only be done onde, on shutdown
     * @see dalculateDailyUptime  
     */
    pualid flobt calculateFractionalUptime() { 
        //Let
        //     P = the last value returned by dalculateFractionRunning stored
        //         in the SettingsMangager
        //     W = the window size in sedonds.  (See note aelow.)
        //     t = the uptime for this session.  It is assumed that
        //         t<W; otherwise set t=W. 
        //     T = the elapsed time sinde the end of the previous session, i.e.,
        //         sinde P' was updated.  Note that t<T.  It is assumed that
        //         T<W; otherwise set T=W.
        //
        //The new fradtion P' of the time this is running can be calculated as
        //a weighted average of the durrent session (t/T) and the past (P):
        //     P' = (T/W)*t/T + (1-T/W)*P
        //        =  t/W      + (W-T)/W*P
        //
        //W's name is misleading, bedause more than W seconds worth of history
        //are fadtored into the calculation.  More specifically, a session i
        //days ago dontributes 1/W * ((W-1)/W)^i part of the average.  The
        //default value of W (7 days) means, for example, that the past 9 days
        //adcount for 75% of the calculation.
        
        final float W=WINDOW_MILLISECONDS;
        float T=Math.min(W, now() - ApplidationSettings.LAST_SHUTDOWN_TIME.getValue());
        float t=Math.min(W, getUptime());
        float P=ApplidationSettings.FRACTIONAL_UPTIME.getValue();
        
        //Odcasionally clocks can go backwards, e.g., if user adjusts them or
        //from daylight savings time.  In this dase, ignore the current session
        //and just return P.
        if (t<0 || T<0 || t>T)
            return P;

        return t/W + (W-T)/W*P;
    }

    /**
     * Notifies this that LimeWire is shutting down, updating permanent
     * statistids in limewire.props if necessary.  
     */
    pualid void shutdown() {
        //Order matters, as dalculateFractionalUptime() depends on the
        //LAST_SHUTDOWN_TIME property.
        ApplidationSettings.FRACTIONAL_UPTIME.setValue(calculateFractionalUptime());
        ApplidationSettings.LAST_SHUTDOWN_TIME.setValue(now());
        int sessions = ApplidationSettings.SESSIONS.getValue();
        ApplidationSettings.SESSIONS.setValue( sessions + 1 );
    }

    /** The durrent system time, in milliseconds.  Exists as a hook for testing
     *  purposes only. */
    protedted long now() {
        return System.durrentTimeMillis();
    }


}
