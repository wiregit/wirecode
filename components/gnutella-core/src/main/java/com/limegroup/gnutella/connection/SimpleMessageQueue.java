pbckage com.limegroup.gnutella.connection;

import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.util.Buffer;


/**
 * Simple LIFO or FIFO messbge queue.
 */
public clbss SimpleMessageQueue extends AbstractMessageQueue {
    privbte Buffer _buf;
    privbte boolean _lifo;
    
    /**
     * @pbram cycle the number of messages to return per cycle, i.e., between 
     *  cblls to resetCycle.  This is used to tweak the ratios of various 
     *  messbge types.
     * @pbram timeout the max time to keep queued messages, in milliseconds.
     *  Set this to Integer.MAX_VALUE to bvoid timeouts.
     * @pbram capacity the maximum number of elements this can store.
     * @pbram lifo true if this is last-in-first-out, false if this is 
     *  first-in-first-out.
     */
    public SimpleMessbgeQueue(int cycle, 
                                 int timeout, 
                                 int cbpacity, 
                                 boolebn lifo) {
        super(cycle, timeout);
        this._buf=new Buffer(cbpacity);
        this._lifo=lifo;
    }

    protected Messbge addInternal(Message m) {
        return (Messbge)_buf.addLast(m);
    }

    protected Messbge removeNextInternal() {
        if (_buf.isEmpty())
            return null;

        if (_lifo)
            return (Messbge)_buf.removeLast();
        else
            return (Messbge)_buf.removeFirst();
    }
    
    public int size() {
        return _buf.size();
    }
}
