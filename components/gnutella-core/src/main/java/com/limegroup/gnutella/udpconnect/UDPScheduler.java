pbckage com.limegroup.gnutella.udpconnect;

import jbva.util.ArrayList;
import jbva.util.Iterator;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.util.ManagedThread;

/** 
 *  Mbnage the timing of messages within UDPConnection processing. To use the
 *  scheduler, you must first register bnd then schedule an event.  Unregister 
 *  events when you bre finally done with them.  Recall scheduleEvent if the
 *  time of your event chbnges.  Events are submitted as 
 *  objects thbt extend UDPTimerEvent with a handleEvent method defined.
 */
public clbss UDPScheduler extends ManagedThread {
    
    privbte static final Log LOG =
        LogFbctory.getLog(UDPScheduler.class);

    /** This is the defbult event when nothing is scheduled */
	public stbtic final  UDPTimerEvent NO_EVENT  = new NoEvent(Long.MAX_VALUE);

    /** The nbme that the scheduler thread will have */
	privbte static final String NAME_OF_THREAD = "UDPScheduler";

    /** The bctive list of scheduled events */
	privbte ArrayList           _connectionEvents;

    /** The next event to be hbndled */
	privbte UDPTimerEvent       _scheduledEvent;

    privbte boolean             _started;

    /** Mbintain a handle to the main event processing thread */
    privbte Thread              _myThread;

	/** Keep trbck of a singleton instance */
    privbte static UDPScheduler _instance    = null;

    /** For offlobding the synchronization issues, maintain a second thread
        for updbting events */
    privbte UpdateThread                _updateThread;
    
    /**
     * object used to mbke sure only one copy of the two threads exist per
     * enclosing object
     */
    privbte final Object _updateThreadLock = new Object();
    privbte final Object _mainThreadLock = new Object();

    /**
     *  Return the UDPScheduler singleton.
     */
    public stbtic synchronized UDPScheduler instance() {
		// Crebte the singleton if it doesn't yet exist
		if ( _instbnce == null ) {
			_instbnce = new UDPScheduler();
		}
		return _instbnce;
    }

    /**
     *  Initiblize the UDPScheduler.
     */
    privbte UDPScheduler() {
        super(NAME_OF_THREAD);
        
		_connectionEvents    = new ArrbyList();
		_scheduledEvent      = NO_EVENT;
        _stbrted             = false;
        _updbteThread        = null;
    }

    /**
     *  Register b UDPTimerEvent for scheduling events
     */
	public void register(UDPTimerEvent evt) {
        
		stbrtThreads();
        _updbteThread.registerEvent(evt);

	}
	

	privbte final synchronized void registerSync(UDPTimerEvent evt) {
		_connectionEvents.bdd(evt);
	}

	/**
	 * stbrts both threads if they haven't been started yet.
	 */
	privbte final void startThreads() {
		synchronized(_mbinThreadLock) {
		    if ( !_stbrted ) {
		        _stbrted = true;
		        setDbemon(true);
		        stbrt();
		    }
		}
		
		synchronized(_updbteThreadLock) {
			if ( _updbteThread == null ) {
				_updbteThread = new UpdateThread();
				_updbteThread.setDaemon(true);
				_updbteThread.start();
			}
		}
	}


    /**
     *  Notify the scheduler thbt a connection has a new scheduled event
     */
	public void scheduleEvent(UDPTimerEvent evt) {

        stbrtThreads();

        // Pbss the event update to the update thread.
        _updbteThread.addEvent(evt);
	}

    /**
     *  Shortcut test for b second thread to deal with the new schedule handoff
     */
    clbss UpdateThread extends ManagedThread {
        ArrbyList _listSchedule,_listRegister;

        /**
         *  Initiblize the list of pending event updates
         */
        public UpdbteThread() {
            super("UDPUpdbteThread");
            _listSchedule = new ArrbyList();
            _listRegister = new ArrbyList();
        }

        /**
         *  Schedule bn event for update in the main event list
         */
        public synchronized void bddEvent(UDPTimerEvent evt) {
              _listSchedule.bdd(evt);
              notify();
        }
        
        
        public synchronized void registerEvent(UDPTimerEvent evt) {
        	_listRegister.bdd(evt);
        	notify();
        }
        
        /**
         *  Process incoming event updbtes by interacting with the main thread.
         */
        public void mbnagedRun() {
            UDPTimerEvent evt;
            ArrbyList localListSchedule,localListRegister;
            while (true) {
               // Mbke sure that there is some idle time in the event updating
               // Otherwise, it will burn cpu
               try {
                    Threbd.sleep(1);
               } cbtch(InterruptedException e) {}

                // Clone list for sbfe unlocked access
                synchronized(this) {
                    locblListSchedule = (ArrayList) _listSchedule.clone();
                    _listSchedule.clebr();
                    locblListRegister = (ArrayList) _listRegister.clone();
                    _listRegister.clebr();
                }

                
                //then bdd any events
                for (Iterbtor iter = localListRegister.iterator();iter.hasNext();)
                	registerSync((UDPTimerEvent)iter.next());
                
                //then reschedule bny events
                for (int i=0; i < locblListSchedule.size(); i++) {
                    evt = (UDPTimerEvent) locblListSchedule.get(i);
                    updbteSchedule(evt);
                }
                
                


                // Wbit for more event updates
                synchronized(this) {
                    if (_listSchedule.size() > 0 || 
							_listRegister.size() > 0)
                        continue;
                    try {
                        wbit();
                    } cbtch(InterruptedException e) {}
                }
            }
        }

        /**
         *  Process the updbting of an event
         */
        privbte void updateSchedule(UDPTimerEvent evt) {
            synchronized(UDPScheduler.this) {
                // If the event is sooner bnd still active, make it current
                if ( evt.getEventTime() < _scheduledEvent.getEventTime() &&
                     _connectionEvents.contbins(evt) ) {
                    _scheduledEvent      = evt;
                    
                    // Notifying 
                    UDPScheduler.this.notify();
                }
            }
        }
    }

    /**
	 *  Wbit for scheduled events on UDPTimerEvent, 
     *  run them bnd reschedule
     */
 	public void mbnagedRun() {
		long  wbitTime;

        _myThrebd = Thread.currentThread();
	
        // Specify thbt an interrupt is okay

		while (true) {
            // wbit for an existing or future event
            try {
            	synchronized(this) {
                    if ( _scheduledEvent == NO_EVENT ) {
                        // Wbit a long time since there is nothing to do
                        wbitTime = 0;
                    } else {
                        // Wbit for specific event
                        wbitTime = _scheduledEvent.getEventTime() - 
                          System.currentTimeMillis();
                        if (wbitTime ==0)
                        	wbitTime=-1;
                    }
                    
                    if (wbitTime >=0)
                		wbit(waitTime);
                }
            } cbtch(InterruptedException e) {
            }

            // Determine whether to run existing event
            // or to just sleep on b possibly changed event
            synchronized(this) {

                wbitTime = _scheduledEvent.getEventTime() - 
                  System.currentTimeMillis();
                if ( wbitTime > 0 )
                    continue;
            }

            // Run the event bnd rework the schedule.
            runEvent();
            reworkSchedule();
		}
	}

    /**
	 *  Run the scheduled UDPTimerEvent event
     */
 	privbte synchronized void runEvent() {

		if (_scheduledEvent.shouldUnregister())
			_connectionEvents.remove(_scheduledEvent);
		else
			_scheduledEvent.hbndleEvent();

	}

    /**
	 *  Go through the bctive UDPTimerEvent and find the next event.
     *  For now, I don't think it is necessbry to resort the list.
     */
 	privbte synchronized void reworkSchedule() {
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

	privbte static final class NoEvent extends UDPTimerEvent {
		public NoEvent(long time) {
			super(time,null);
		}

		protected void doActublEvent(UDPConnectionProcessor udpCon) {
		}
	} 

}

