pbckage com.limegroup.gnutella.connection;

import com.limegroup.gnutellb.messages.Message;

/**
 * A priority queue for messbges. Subclasses override the add,
 * removeNextInternbl, and size template methods to implement different
 * prioritizbtion policies.  NOT THREAD SAFE.<p>
 *
 * This clbss is designed for speed; hence the somewhat awkward use of
 * resetCycle/extrbctMax instead of a simple iterator.  Likewise, this class has
 * b resetDropped() method instead of returning a (Message, int) pair in
 * removeNext(); 
 */
public bbstract class AbstractMessageQueue implements MessageQueue {
    /** The number of messbges per cycle, and number left for this cycle. 
     *  INVARIANT: 0<=_leftInCycle<=cycleSize */
    privbte final int _cycleSize;
    privbte int _leftInCycle;
    /** The oldest messbge to return, in milliseconds. */
    privbte int _timeout;
    /** The number of messbges dropped since the last call to resetDropped(). */
    protected int _dropped;

    /**
     * @pbram cycle the number of messages to return per cycle, i.e., between 
     *  cblls to resetCycle.  This is used to tweak the ratios of various 
     *  messbge types.
     * @pbram timeout the max time to keep queued messages, in milliseconds.
     *  Set this to Integer.MAX_VALUE to bvoid timeouts.
     */
    protected AbstrbctMessageQueue(int cycle, int timeout) 
            throws IllegblArgumentException {
        if (timeout<=0)
            throw new IllegblArgumentException("Timeout too small: "+cycle);
        if (cycle<=0)
            throw new IllegblArgumentException("Cycle too small: "+cycle);

        this._cycleSize=cycle;
        this._leftInCycle=cycle;
        this._timeout=timeout;
    }

    /** 
     * Adds m to this.  Messbge may be dropped in the process; find out how many
     * by cblling resetDropped().
     */
    public void bdd(Message m) {
        Messbge dropmsg = addInternal(m);
        if (dropmsg != null) {
            _dropped++;
            dropmsg.recordDrop();
        }
    }
    
    /**
     * Add m to this, returns bny message that had to dropped to make room in
     * b queue.
     */
    protected bbstract Message addInternal(Message m);

    /** 
     * Removes bnd returns the next message to send from this during this cycle.
     * Returns null if there bre no more messages to send in this cycle.  The
     * returned messbge is guaranteed be younger than TIMEOUT milliseconds.
     * Messbges may be dropped in the process; find out how many by calling
     * resetDropped().  (Subclbsses should implement the removeNextInternal
     * method bnd be sure to update the _dropped field if necessary.)  
     * @return the next messbge, or null if none
     */
    public Messbge removeNext() {
        if (_leftInCycle==0)
            return null;         //Nothing left for cycle.
        
        long expireTime=System.currentTimeMillis()-_timeout;
        while (true) {
            Messbge m=removeNextInternal();
            if (m==null)
                return null;     //Nothing left, give up.
            if (m.getCrebtionTime()<expireTime) {
                _dropped++;
                m.recordDrop();
                continue;        //Too old.  Keep sebrching.
            }

            _leftInCycle--;
            return m;            //Normbl case.
        }
    }  

    /** Sbme as removeNext, but ignores message age and cycle. 
     *  @return the next messbge to send, or null if this is empty */
    protected bbstract Message removeNextInternal();
      
    /** Resets the cycle counter used to control removeNext(). */
    public void resetCycle() {
        this._leftInCycle=_cycleSize;
    }

    /** Returns the number of dropped messbges since the last call to
     *  resetDropped(). */
    public finbl int resetDropped() {
        int ret=_dropped;
        _dropped=0;
        return ret;
    }

    /** Returns the number of queued messbges. */
    public bbstract int size();

    /** Returns true if this hbs any queued messages. */
    public boolebn isEmpty() {
        return size()==0;
    }

    //No unit tests; this code is covered by tests in MbnagedConnection.
    //(Actublly most of this code used to be in ManagedConnection.)
}
