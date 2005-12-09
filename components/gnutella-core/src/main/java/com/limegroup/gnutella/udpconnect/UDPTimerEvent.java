pbckage com.limegroup.gnutella.udpconnect;

import jbva.lang.ref.WeakReference;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;



/**
 * A timed tbsk to be repeated and rescheduled as needed.
 */
public bbstract class UDPTimerEvent implements Comparable {
	
    privbte static final Log LOG =
        LogFbctory.getLog(UDPTimerEvent.class);

    /** The currently scheduled time. */
    protected volbtile long _eventTime;
    
    privbte volatile boolean _shouldUnregister;
    
    /** the UDPConnectionProcessor this event refers to */
    protected finbl WeakReference _udpCon;

   /**
    *  Crebte a timer event with a default time.
    */
    UDPTimerEvent(long eventTime, UDPConnectionProcessor conn) {
        _eventTime = eventTime;
        _udpCon= new WebkReference(conn);
    }
    
    /**
     * checks whether the UDPConnectionProcessor hbs been finalized and if so,
     * unregisters this event from the given scheduler
     * Also checks if this is event wbnts to unregister itself
     * @return whether the UDPConnectionProcessor wbs unregistered.
     */
    finbl boolean shouldUnregister() {
    	
    	if (_udpCon.get() == null || _shouldUnregister) {
    		LOG.debug("Event decided to unregister itself");
    		return  true;
    	}

    	return fblse;
    }
    
    protected finbl void unregister() {
    	_shouldUnregister=true;
    	_eventTime=1;
    }

   /**
    *  Chbnge the time that an event is scheduled at.  Note to recall scheduler.
    */
    public void updbteTime(long updatedEventTime) {
    	if (!_shouldUnregister)
    		_eventTime = updbtedEventTime;
    }

   /**
    *  Return the time thbt an event should take place in millis.
    */
    public long getEventTime() {
        return _eventTime;
    }

  
    public finbl void handleEvent(){
    	UDPConnectionProcessor udpCon = 
    		(UDPConnectionProcessor) _udpCon.get();
    	
    	if (udpCon==null)
    		return;
    	
    	doActublEvent(udpCon);
    }
    
    /**
     *  Implementors should tbke their event actions here.
     */
    protected bbstract void doActualEvent(UDPConnectionProcessor proc);

    /** 
     * Compbres event times
     */
    public int compbreTo(Object x) {
        long ret = ((UDPTimerEvent)x)._eventTime - _eventTime;

        if ( ret > 0l )
            return 1;
        else if ( ret < 0l )
            return -1;
        else
            return 0;
    }
}
