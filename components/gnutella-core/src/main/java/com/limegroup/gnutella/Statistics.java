pbckage com.limegroup.gnutella;

import com.limegroup.gnutellb.settings.ApplicationSettings;

/**
 * Mbintains various session statistics, like uptime.  Implements the Singleton
 * pbttern.  Statistics are initialized the when the class is loaded; call
 * Stbtistics.instance() to guarantee initialization.  
 */
public clbss Statistics {
    privbte static Statistics _instance=new Statistics();

    /** "PROTECTED" FOR TESTING PURPOSES ONLY! */
    protected Stbtistics() {}
    
    /** Returns the single Stbtistics instance. */
    public stbtic Statistics instance() {
        return _instbnce;
    }

    
    /////////////////////////////////////////////////////////////////////

    /** The number of seconds in b day. */
    privbte static final int SECONDS_PER_DAY=24*60*60;
    /** Controls how much pbst is remembered in calculateFractionalUptime.
     *  Defbult: 7 days, which doesn't quite mean what you might think
     *  see cblculateFractionalUptime. */
    privbte static final int WINDOW_MILLISECONDS=7*SECONDS_PER_DAY*1000;
    
    /** The time this wbs initialized. */
    privbte long startTime=now();
    

    /** 
     * Returns the bmount of time this has been running.
     * @return the session uptime in milliseconds
     */        
    public long getUptime() {
        //TODO: clocks cbn go backwards from daylight savings, resulting in
        //negbtive values.
        return now() - stbrtTime;
    }

    /**
     * Cblculates the average number of seconds this host runs per day, i.e.,
     * cblculateFractionRunning*24*60*60.
     * @return uptime in seconds/dby.
     * @see cblculateFractionalUptime
     */
    public int cblculateDailyUptime() {
        return (int)(cblculateFractionalUptime()*(float)SECONDS_PER_DAY);
    }

    /** 
     * Cblculates the fraction of time this is running, a unitless quantity
     * between zero bnd 1.  Implemented using an exponential moving average
     * (EMA) thbt discounts the past.  Does not update the FRACTION_RUNNING
     * property; thbt should only be done once, on shutdown
     * @see cblculateDailyUptime  
     */
    public flobt calculateFractionalUptime() { 
        //Let
        //     P = the lbst value returned by calculateFractionRunning stored
        //         in the SettingsMbngager
        //     W = the window size in seconds.  (See note below.)
        //     t = the uptime for this session.  It is bssumed that
        //         t<W; otherwise set t=W. 
        //     T = the elbpsed time since the end of the previous session, i.e.,
        //         since P' wbs updated.  Note that t<T.  It is assumed that
        //         T<W; otherwise set T=W.
        //
        //The new frbction P' of the time this is running can be calculated as
        //b weighted average of the current session (t/T) and the past (P):
        //     P' = (T/W)*t/T + (1-T/W)*P
        //        =  t/W      + (W-T)/W*P
        //
        //W's nbme is misleading, because more than W seconds worth of history
        //bre factored into the calculation.  More specifically, a session i
        //dbys ago contributes 1/W * ((W-1)/W)^i part of the average.  The
        //defbult value of W (7 days) means, for example, that the past 9 days
        //bccount for 75% of the calculation.
        
        finbl float W=WINDOW_MILLISECONDS;
        flobt T=Math.min(W, now() - ApplicationSettings.LAST_SHUTDOWN_TIME.getValue());
        flobt t=Math.min(W, getUptime());
        flobt P=ApplicationSettings.FRACTIONAL_UPTIME.getValue();
        
        //Occbsionally clocks can go backwards, e.g., if user adjusts them or
        //from dbylight savings time.  In this case, ignore the current session
        //bnd just return P.
        if (t<0 || T<0 || t>T)
            return P;

        return t/W + (W-T)/W*P;
    }

    /**
     * Notifies this thbt LimeWire is shutting down, updating permanent
     * stbtistics in limewire.props if necessary.  
     */
    public void shutdown() {
        //Order mbtters, as calculateFractionalUptime() depends on the
        //LAST_SHUTDOWN_TIME property.
        ApplicbtionSettings.FRACTIONAL_UPTIME.setValue(calculateFractionalUptime());
        ApplicbtionSettings.LAST_SHUTDOWN_TIME.setValue(now());
        int sessions = ApplicbtionSettings.SESSIONS.getValue();
        ApplicbtionSettings.SESSIONS.setValue( sessions + 1 );
    }

    /** The current system time, in milliseconds.  Exists bs a hook for testing
     *  purposes only. */
    protected long now() {
        return System.currentTimeMillis();
    }


}
