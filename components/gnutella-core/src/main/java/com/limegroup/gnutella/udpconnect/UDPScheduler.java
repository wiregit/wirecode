package com.limegroup.gnutella.udpconnect;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.ManagedThread;

/** 
 *  Manage the timing of messages within UDPConnection processing. To use the
 *  scheduler, you must first register and then schedule an event.  Unregister 
 *  events when you are finally done with them.  Recall scheduleEvent if the
 *  time of your event changes.  Events are submitted as 
 *  objects that extend UDPTimerEvent with a handleEvent method defined.
 */
public class UDPScheduler extends ManagedThread {

    /** This is the default event when nothing is scheduled */
	public static final  UDPTimerEvent NO_EVENT  = new NoEvent(Long.MAX_VALUE);

    /** This wait time is for waiting while nothing is scheduled */
	private static final long   LONG_WAIT_TIME = 10*60*1000;

    /** The name that the scheduler thread will have */
	private static final String NAME_OF_THREAD = "UDPScheduler";

    /** The active list of scheduled events */
	private ArrayList           _connectionEvents;

    /** The next event to be handled */
	private UDPTimerEvent       _scheduledEvent;

    /** Waiting is a flag regarding whether it is safe to interrupt the 
        execution thread.  It should be set to true unless you are 
        executing external code. */
	private boolean             _waiting;

    private boolean             _started;

	/** Keep track of a singleton instance */
    private static UDPScheduler _instance    = null;

    /**
     *  Return the UDPScheduler singleton.
     */
    public static synchronized UDPScheduler instance() {
		// Create the singleton if it doesn't yet exist
		if ( _instance == null ) {
			_instance = new UDPScheduler();
		}
		return _instance;
    }

    /**
     *  Initialize the UDPScheduler.
     */
    private UDPScheduler() {
        super(NAME_OF_THREAD);
		_connectionEvents    = new ArrayList();
		_scheduledEvent      = NO_EVENT;
		_waiting             = true;
        _started             = false;
    }

    /**
     *  Register a UDPTimerEvent for scheduling events
     */
	public synchronized void register(UDPTimerEvent evt) {
        _connectionEvents.add(evt);

        // Fire it up if it wasnt already running
		if ( !_started ) {
			setDaemon(true);
			start();
            _started = true;
		}
	}

    /**
     *  Unregister a UDPTimerEvent for scheduling events
     */
	public synchronized void unregister(UDPTimerEvent evt) {
		_connectionEvents.remove(evt);
	
		// Replace the removed connection in schedule if necessary
		if ( _scheduledEvent == evt )
 			reworkSchedule();
	}

    /**
     *  Notify the scheduler that a connection has a new scheduled event
     */
	public synchronized void scheduleEvent(UDPTimerEvent evt) {
		if ( evt.getEventTime() < _scheduledEvent.getEventTime() ) {
			_scheduledEvent      = evt;
			if (_waiting) {
				interrupt();
			}
		}
	}

    /**
	 *  Wait for scheduled events on UDPTimerEvent, 
     *  run them and reschedule
     */
 	public void managedRun() {
		long  waitTime;
	
        // Specify that an interrupt is okay
		_waiting = true;
		while (true) {
            // wait for an existing or future event
            try {
                synchronized(this) {
                    if ( _scheduledEvent == NO_EVENT ) {
                        // Wait a long time since there is nothing to do
                        waitTime = LONG_WAIT_TIME;
                    } else {
                        // Wait for specific event
                        waitTime = _scheduledEvent.getEventTime() - 
                          System.currentTimeMillis();
                    }
                }
                if ( waitTime > 0 ) 
                    Thread.sleep(waitTime);

            } catch(InterruptedException e) {
            }

            // Determine whether to run existing event
            // or to just sleep on a possibly changed event
            synchronized(this) {
                if ( _scheduledEvent == NO_EVENT )
                    continue;
                waitTime = _scheduledEvent.getEventTime() - 
                  System.currentTimeMillis();
                if ( waitTime > 0 )
                    continue;
            }
            
            // Deactivate interrupts, run event and then allow interrupts
            _waiting = false;
            runEvent();
            _waiting = true;
            reworkSchedule();
		}
	}

    /**
	 *  Run the scheduled UDPTimerEvent event
     */
 	private synchronized void runEvent() {
		if ( _scheduledEvent != NO_EVENT ) {
			_scheduledEvent.handleEvent();
		}
	}

    /**
	 *  Go through the active UDPTimerEvent and find the next event.
     *  For now, I don't think it is necessary to resort the list.
     */
 	private synchronized void reworkSchedule() {
		UDPTimerEvent evt;
		long          time;

		_scheduledEvent      = NO_EVENT;
		for (int i = 0; i < _connectionEvents.size(); i++) {
			evt = (UDPTimerEvent) _connectionEvents.get(i);
			time = evt.getEventTime();
			if ( evt  != NO_EVENT && 
				 (time < _scheduledEvent.getEventTime() || 
                 _scheduledEvent == NO_EVENT)) {
				_scheduledEvent = evt;
			}
		}
	}

}

class NoEvent extends UDPTimerEvent {
    public NoEvent(long time) {
        super(time);
    }

    public void handleEvent() {
    }
} 
