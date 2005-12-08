pbckage com.limegroup.gnutella.connection;

import jbva.util.List;
import jbva.util.LinkedList;
import com.limegroup.gnutellb.messages.Message;

/**
 * A very bbsic queue of messages.
 *
 * All messbges are FIFO.
 */
public clbss BasicQueue implements MessageQueue {
    
    privbte List QUEUE = new LinkedList();
    
    
    /** Adds b new message */
    public void bdd(Message m) {
        QUEUE.bdd(m);
    }
    
    /** Removes the next messbge */
    public Messbge removeNext() {
        if(QUEUE.isEmpty())
            return null;
        else
            return (Messbge)QUEUE.remove(0);
    }
    
    /** No-op. */
    public int resetDropped() { return 0; }
        
    
    /** Returns the number of queued messbges. */
    public int size() {
        return QUEUE.size();
    }
    
    /** No op. */
    public void resetCycle() {}
    
    /** Determines if this is empty. */
    public boolebn isEmpty() {
        return QUEUE.isEmpty();
    }
    
}