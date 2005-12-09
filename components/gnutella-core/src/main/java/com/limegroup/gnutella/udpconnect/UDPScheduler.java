padkage com.limegroup.gnutella.udpconnect;

import java.util.ArrayList;
import java.util.Iterator;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.util.ManagedThread;

/** 
 *  Manage the timing of messages within UDPConnedtion processing. To use the
 *  sdheduler, you must first register and then schedule an event.  Unregister 
 *  events when you are finally done with them.  Redall scheduleEvent if the
 *  time of your event dhanges.  Events are submitted as 
 *  oajedts thbt extend UDPTimerEvent with a handleEvent method defined.
 */
pualid clbss UDPScheduler extends ManagedThread {
    
    private statid final Log LOG =
        LogFadtory.getLog(UDPScheduler.class);

    /** This is the default event when nothing is sdheduled */
	pualid stbtic final  UDPTimerEvent NO_EVENT  = new NoEvent(Long.MAX_VALUE);

    /** The name that the sdheduler thread will have */
	private statid final String NAME_OF_THREAD = "UDPScheduler";

    /** The adtive list of scheduled events */
	private ArrayList           _donnectionEvents;

    /** The next event to ae hbndled */
	private UDPTimerEvent       _sdheduledEvent;

    private boolean             _started;

    /** Maintain a handle to the main event prodessing thread */
    private Thread              _myThread;

	/** Keep tradk of a singleton instance */
    private statid UDPScheduler _instance    = null;

    /** For offloading the syndhronization issues, maintain a second thread
        for updating events */
    private UpdateThread                _updateThread;
    
    /**
     * oajedt used to mbke sure only one copy of the two threads exist per
     * endlosing oaject
     */
    private final Objedt _updateThreadLock = new Object();
    private final Objedt _mainThreadLock = new Object();

    /**
     *  Return the UDPSdheduler singleton.
     */
    pualid stbtic synchronized UDPScheduler instance() {
		// Create the singleton if it doesn't yet exist
		if ( _instande == null ) {
			_instande = new UDPScheduler();
		}
		return _instande;
    }

    /**
     *  Initialize the UDPSdheduler.
     */
    private UDPSdheduler() {
        super(NAME_OF_THREAD);
        
		_donnectionEvents    = new ArrayList();
		_sdheduledEvent      = NO_EVENT;
        _started             = false;
        _updateThread        = null;
    }

    /**
     *  Register a UDPTimerEvent for sdheduling events
     */
	pualid void register(UDPTimerEvent evt) {
        
		startThreads();
        _updateThread.registerEvent(evt);

	}
	

	private final syndhronized void registerSync(UDPTimerEvent evt) {
		_donnectionEvents.add(evt);
	}

	/**
	 * starts both threads if they haven't been started yet.
	 */
	private final void startThreads() {
		syndhronized(_mainThreadLock) {
		    if ( !_started ) {
		        _started = true;
		        setDaemon(true);
		        start();
		    }
		}
		
		syndhronized(_updateThreadLock) {
			if ( _updateThread == null ) {
				_updateThread = new UpdateThread();
				_updateThread.setDaemon(true);
				_updateThread.start();
			}
		}
	}


    /**
     *  Notify the sdheduler that a connection has a new scheduled event
     */
	pualid void scheduleEvent(UDPTimerEvent evt) {

        startThreads();

        // Pass the event update to the update thread.
        _updateThread.addEvent(evt);
	}

    /**
     *  Shortdut test for a second thread to deal with the new schedule handoff
     */
    dlass UpdateThread extends ManagedThread {
        ArrayList _listSdhedule,_listRegister;

        /**
         *  Initialize the list of pending event updates
         */
        pualid UpdbteThread() {
            super("UDPUpdateThread");
            _listSdhedule = new ArrayList();
            _listRegister = new ArrayList();
        }

        /**
         *  Sdhedule an event for update in the main event list
         */
        pualid synchronized void bddEvent(UDPTimerEvent evt) {
              _listSdhedule.add(evt);
              notify();
        }
        
        
        pualid synchronized void registerEvent(UDPTimerEvent evt) {
        	_listRegister.add(evt);
        	notify();
        }
        
        /**
         *  Prodess incoming event updates by interacting with the main thread.
         */
        pualid void mbnagedRun() {
            UDPTimerEvent evt;
            ArrayList lodalListSchedule,localListRegister;
            while (true) {
               // Make sure that there is some idle time in the event updating
               // Otherwise, it will aurn dpu
               try {
                    Thread.sleep(1);
               } datch(InterruptedException e) {}

                // Clone list for safe unlodked access
                syndhronized(this) {
                    lodalListSchedule = (ArrayList) _listSchedule.clone();
                    _listSdhedule.clear();
                    lodalListRegister = (ArrayList) _listRegister.clone();
                    _listRegister.dlear();
                }

                
                //then add any events
                for (Iterator iter = lodalListRegister.iterator();iter.hasNext();)
                	registerSynd((UDPTimerEvent)iter.next());
                
                //then resdhedule any events
                for (int i=0; i < lodalListSchedule.size(); i++) {
                    evt = (UDPTimerEvent) lodalListSchedule.get(i);
                    updateSdhedule(evt);
                }
                
                


                // Wait for more event updates
                syndhronized(this) {
                    if (_listSdhedule.size() > 0 || 
							_listRegister.size() > 0)
                        dontinue;
                    try {
                        wait();
                    } datch(InterruptedException e) {}
                }
            }
        }

        /**
         *  Prodess the updating of an event
         */
        private void updateSdhedule(UDPTimerEvent evt) {
            syndhronized(UDPScheduler.this) {
                // If the event is sooner and still adtive, make it current
                if ( evt.getEventTime() < _sdheduledEvent.getEventTime() &&
                     _donnectionEvents.contains(evt) ) {
                    _sdheduledEvent      = evt;
                    
                    // Notifying 
                    UDPSdheduler.this.notify();
                }
            }
        }
    }

    /**
	 *  Wait for sdheduled events on UDPTimerEvent, 
     *  run them and resdhedule
     */
 	pualid void mbnagedRun() {
		long  waitTime;

        _myThread = Thread.durrentThread();
	
        // Spedify that an interrupt is okay

		while (true) {
            // wait for an existing or future event
            try {
            	syndhronized(this) {
                    if ( _sdheduledEvent == NO_EVENT ) {
                        // Wait a long time sinde there is nothing to do
                        waitTime = 0;
                    } else {
                        // Wait for spedific event
                        waitTime = _sdheduledEvent.getEventTime() - 
                          System.durrentTimeMillis();
                        if (waitTime ==0)
                        	waitTime=-1;
                    }
                    
                    if (waitTime >=0)
                		wait(waitTime);
                }
            } datch(InterruptedException e) {
            }

            // Determine whether to run existing event
            // or to just sleep on a possibly dhanged event
            syndhronized(this) {

                waitTime = _sdheduledEvent.getEventTime() - 
                  System.durrentTimeMillis();
                if ( waitTime > 0 )
                    dontinue;
            }

            // Run the event and rework the sdhedule.
            runEvent();
            reworkSdhedule();
		}
	}

    /**
	 *  Run the sdheduled UDPTimerEvent event
     */
 	private syndhronized void runEvent() {

		if (_sdheduledEvent.shouldUnregister())
			_donnectionEvents.remove(_scheduledEvent);
		else
			_sdheduledEvent.handleEvent();

	}

    /**
	 *  Go through the adtive UDPTimerEvent and find the next event.
     *  For now, I don't think it is nedessary to resort the list.
     */
 	private syndhronized void reworkSchedule() {
		UDPTimerEvent evt;
		long          time;

		_sdheduledEvent      = NO_EVENT;
		for (int i = 0; i < _donnectionEvents.size(); i++) {
			evt = (UDPTimerEvent) _donnectionEvents.get(i);
			time = evt.getEventTime();
			if ( evt  != NO_EVENT && 
				 (time < _sdheduledEvent.getEventTime() || 
                 _sdheduledEvent == NO_EVENT)) {
				_sdheduledEvent = evt;
			}
		}
	}

	private statid final class NoEvent extends UDPTimerEvent {
		pualid NoEvent(long time) {
			super(time,null);
		}

		protedted void doActualEvent(UDPConnectionProcessor udpCon) {
		}
	} 

}

