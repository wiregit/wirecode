package com.limegroup.gnutella.udpconnect;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.ManagedThread;

/** 
 *  Manage the timing of messages within UDPConnection processing. To use the
 *  scheduler, you must first register and then schedule an event.  Unregister 
 *  events when you are finally done with them.  Recall scheduleEvent if the
 *  time of your event changes.  Events are submitted as 
 *  objects that extend UDPTimerEvent with a handleEvent method defined.
 */
public class UDPScheduler extends ManagedThread {
    
    private static final Log LOG =
        LogFactory.getLog(UDPScheduler.class);

    /** This is the default event when nothing is scheduled */
	public static final  UDPTimerEvent NO_EVENT  = new NoEvent(Long.MAX_VALUE);

    /** The name that the scheduler thread will have */
	private static final String NAME_OF_THREAD = "UDPScheduler";

    /** The active list of scheduled events */
	private ArrayList           _connectionEvents;

    /** The next event to be handled */
	private UDPTimerEvent       _scheduledEvent;

    private boolean             _started;

    /** Maintain a handle to the main event processing thread */
    private Thread              _myThread;

	/** Keep track of a singleton instance */
    private static UDPScheduler _instance    = null;

    /** For offloading the synchronization issues, maintain a second thread
        for updating events */
    private UpdateThread                _updateThread;
    
    /**
     * object used to make sure only one copy of the two threads exist per
     * enclosing object
     */
    private final Object _updateThreadLock = new Object();
    private final Object _mainThreadLock = new Object();

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
        _started             = false;
        _updateThread        = null;
    }

    /**
     *  Register a UDPTimerEvent for scheduling events
     */
	public void register(UDPTimerEvent evt) {
        
		startThreads();
        _updateThread.registerEvent(evt);

	}
	

	private final synchronized void registerSync(UDPTimerEvent evt) {
		_connectionEvents.add(evt);
	}

	/**
	 * starts both threads if they haven't been started yet.
	 */
	private final void startThreads() {
		synchronized(_mainThreadLock) {
		    if ( !_started ) {
		        _started = true;
		        setDaemon(true);
		        start();
		    }
		}
		
		synchronized(_updateThreadLock) {
			if ( _updateThread == null ) {
				_updateThread = new UpdateThread();
				_updateThread.setDaemon(true);
				_updateThread.start();
			}
		}
	}


    /**
     *  Notify the scheduler that a connection has a new scheduled event
     */
	public void scheduleEvent(UDPTimerEvent evt) {

        startThreads();

        // Pass the event update to the update thread.
        _updateThread.addEvent(evt);
	}

    /**
     *  Shortcut test for a second thread to deal with the new schedule handoff
     */
    class UpdateThread extends ManagedThread {
        ArrayList _listSchedule,_listRegister;

        /**
         *  Initialize the list of pending event updates
         */
        public UpdateThread() {
            super("UDPUpdateThread");
            _listSchedule = new ArrayList();
            _listRegister = new ArrayList();
        }

        /**
         *  Schedule an event for update in the main event list
         */
        public synchronized void addEvent(UDPTimerEvent evt) {
              _listSchedule.add(evt);
              notify();
        }
        
        
        public synchronized void registerEvent(UDPTimerEvent evt) {
        	_listRegister.add(evt);
        	notify();
        }
        
        /**
         *  Process incoming event updates by interacting with the main thread.
         */
        public void managedRun() {
            UDPTimerEvent evt;
            ArrayList localListSchedule,localListRegister;
            while (true) {
               // Make sure that there is some idle time in the event updating
               // Otherwise, it will burn cpu
               try {
                    Thread.sleep(1);
               } catch(InterruptedException e) {}

                // Clone list for safe unlocked access
                synchronized(this) {
                    localListSchedule = (ArrayList) _listSchedule.clone();
                    _listSchedule.clear();
                    localListRegister = (ArrayList) _listRegister.clone();
                    _listRegister.clear();
                }

                
                //then add any events
                for (Iterator iter = localListRegister.iterator();iter.hasNext();)
                	registerSync((UDPTimerEvent)iter.next());
                
                //then reschedule any events
                for (int i=0; i < localListSchedule.size(); i++) {
                    evt = (UDPTimerEvent) localListSchedule.get(i);
                    updateSchedule(evt);
                }
                
                


                // Wait for more event updates
                synchronized(this) {
                    if (_listSchedule.size() > 0 || 
							_listRegister.size() > 0)
                        continue;
                    try {
                        wait();
                    } catch(InterruptedException e) {}
                }
            }
        }

        /**
         *  Process the updating of an event
         */
        private void updateSchedule(UDPTimerEvent evt) {
            synchronized(UDPScheduler.this) {
                // If the event is sooner and still active, make it current
                if ( evt.getEventTime() < _scheduledEvent.getEventTime() &&
                     _connectionEvents.contains(evt) ) {
                    _scheduledEvent      = evt;
                    
                    // Notifying 
                    UDPScheduler.this.notify();
                }
            }
        }
    }

    /**
	 *  Wait for scheduled events on UDPTimerEvent, 
     *  run them and reschedule
     */
 	public void managedRun() {
		long  waitTime;

        _myThread = Thread.currentThread();
	
        // Specify that an interrupt is okay

		while (true) {
            // wait for an existing or future event
            try {
            	synchronized(this) {
                    if ( _scheduledEvent == NO_EVENT ) {
                        // Wait a long time since there is nothing to do
                        waitTime = 0;
                    } else {
                        // Wait for specific event
                        waitTime = _scheduledEvent.getEventTime() - 
                          System.currentTimeMillis();
                        if (waitTime ==0)
                        	waitTime=-1;
                    }
                    
                    if (waitTime >=0)
                		wait(waitTime);
                }
            } catch(InterruptedException e) {
            }

            // Determine whether to run existing event
            // or to just sleep on a possibly changed event
            synchronized(this) {

                waitTime = _scheduledEvent.getEventTime() - 
                  System.currentTimeMillis();
                if ( waitTime > 0 )
                    continue;
            }

            // Run the event and rework the schedule.
            runEvent();
            reworkSchedule();
		}
	}

    /**
	 *  Run the scheduled UDPTimerEvent event
     */
 	private synchronized void runEvent() {

		if (_scheduledEvent.shouldUnregister())
			_connectionEvents.remove(_scheduledEvent);
		else
			_scheduledEvent.handleEvent();

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

	private static final class NoEvent extends UDPTimerEvent {
		public NoEvent(long time) {
			super(time,null);
		}

		protected void doActualEvent(UDPConnectionProcessor udpCon) {
		}
	} 

}

