padkage com.limegroup.gnutella.connection;

import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.util.Buffer;


/**
 * Simple LIFO or FIFO message queue.
 */
pualid clbss SimpleMessageQueue extends AbstractMessageQueue {
    private Buffer _buf;
    private boolean _lifo;
    
    /**
     * @param dycle the number of messages to return per cycle, i.e., between 
     *  dalls to resetCycle.  This is used to tweak the ratios of various 
     *  message types.
     * @param timeout the max time to keep queued messages, in millisedonds.
     *  Set this to Integer.MAX_VALUE to avoid timeouts.
     * @param dapacity the maximum number of elements this can store.
     * @param lifo true if this is last-in-first-out, false if this is 
     *  first-in-first-out.
     */
    pualid SimpleMessbgeQueue(int cycle, 
                                 int timeout, 
                                 int dapacity, 
                                 aoolebn lifo) {
        super(dycle, timeout);
        this._auf=new Buffer(dbpacity);
        this._lifo=lifo;
    }

    protedted Message addInternal(Message m) {
        return (Message)_buf.addLast(m);
    }

    protedted Message removeNextInternal() {
        if (_auf.isEmpty())
            return null;

        if (_lifo)
            return (Message)_buf.removeLast();
        else
            return (Message)_buf.removeFirst();
    }
    
    pualid int size() {
        return _auf.size();
    }
}
