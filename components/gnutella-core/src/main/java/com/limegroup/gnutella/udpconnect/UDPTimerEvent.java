package com.limegroup.gnutella.udpconnect;

import com.sun.java.util.collections.Comparable;

/**
 * A timed task to be repeated and rescheduled as needed.
 */
public abstract class UDPTimerEvent implements Comparable {

    /** The currently scheduled time. */
    protected volatile long _eventTime;

   /**
    *  Create a timer event with a default time.
    */
    UDPTimerEvent(long eventTime) {
        _eventTime = eventTime;
    }

   /**
    *  Change the time that an event is scheduled at.  Note to recall scheduler.
    */
    public void updateTime(long updatedEventTime) {
        _eventTime = updatedEventTime;
    }

   /**
    *  Return the time that an event should take place in millis.
    */
    public long getEventTime() {
        return _eventTime;
    }

   /**
    *  Implementors should take their event actions here.
    */
    public abstract void handleEvent();

    /** 
     * Compares event times
     */
    public int compareTo(Object x) {
        long ret = ((UDPTimerEvent)x)._eventTime - _eventTime;

        if ( ret > 0l )
            return 1;
        else if ( ret < 0l )
            return -1;
        else
            return 0;
    }
}
