package com.limegroup.gnutella.connection;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;


/**
 * Encapsulates the single thread that reads and writes from all messaging
 * connections with non-blocking IO.  Provides the listener to take appropriate
 * actions when the connection is initialized, messages are read, etc.  Typically
 * this just involves delegating to ConnectionManager or MessageRouter.  
 */
public class ConnectionDriver implements ConnectionListener {  
    /** Sibling classes in the backend. */
    ConnectionManager _manager;
    MessageRouter _router;
    ActivityCallback _callback;

    /** The source of all message reads and writes */
    private Selector _selector;
    /** The list of messaging connections needing to be registered.  This is a
     *  byproduct of our bizarre threading model; it will be obsolete when
     *  connections are generated by non-blocking IO in the run() method. */
    private List /* of Connection */ _needRegister=
        Collections.synchronizedList(new LinkedList());
    /** The list of messaging connections needing to be registered for writes as
     *  well as reads.  Updated in MessageRouter.handleMessage -> ... ->
     *  Connection.write -> this.needsWrite when a write failed. */
    private List /* of Connection */ _needWriteRegister=
        Collections.synchronizedList(new LinkedList());
    

    public void initialize(ConnectionManager manager,
                           MessageRouter router,
                           ActivityCallback callback) {
        this._manager=manager;
        this._router=router;
        this._callback=callback;
        try {
            this._selector=Selector.open();
        } catch (IOException e) {
            //TODO?
            Assert.that(false, "Couldn't open selector");
        }

        Thread runner=new Thread() {
            public void run() {
                loopForMessages();
            }
        };
        runner.setDaemon(true);
        runner.start();
    }

 
    ///////////////////////////// ConnectionListener Methods //////////////////

    public void initialized(Connection c) { 
        //Eventually this will be done BEFORE handshaking is over.  We can't
        //just call selector.register(..) here, as that can block.  So we add it
        //to queue than wake up selector thread.
        _needRegister.add(c);
        _selector.wakeup();
        
    }
        
    public void read(Connection c, Message m) {
        if (c instanceof ManagedConnection) {
            //TODO: inline this method.  (This isn't a performance issue; it
            //just doesn't belong in ManagedConnection.)
            ((ManagedConnection)c).handleRead(m);
        }
    }

    public void read(Connection c, BadPacketException error) {
        //ignore
    }

    public void needsWrite(Connection c) {
        //The call to wakeup is typically not needed, as (a) the calls is done
        //from loopForMessages()->read()->handleMessage(), just before we
        //register writes or (b) the call is done from initiating a query from
        //the GUI and some sort of read event will be generated shortly.  But this 
        //make testing easier.
        _needWriteRegister.add(c);
        _selector.wakeup();
    }

    public void error(Connection c) {
        if (c instanceof ManagedConnection) {
            //No need to actually unregister the socket; just make sure it's not
            //in the broadcast list.
            ManagedConnection mc=(ManagedConnection)c;
            _manager.remove(mc);
            //_catcher.doneWithMessageLoop(e);  TODO
        }
    }

    ///////////////////////////////// Dispatch Loop ////////////////////////////

    /**
     * Repeatedly reads and writes messages from all initialized connections,
     * which includes reject connections.
     */
    private void loopForMessages() {
        //TODO: factor this
        //TODO: ChannelClosedException in register

        while (true) {
            try {
            //Register any new connections for read events.  (Write operations
            //are only generated if a write failed.)  See initialized(c).
            synchronized (_needRegister) {
                for (Iterator iter=_needRegister.iterator(); iter.hasNext(); ) {
                    Connection c=(Connection)iter.next();
                    register(c, false);
                }
                _needRegister.clear();
            }
            
            //Wait for something to be readable or writeable.
            try {
                _selector.select();
            } catch (IOException e) {
                //It's really not clear why this can happen, but it does
                //occasionally.  Ignore
            }

            //Handle all reads.  This generates event to listener (this), which
            //in turns routes the message.
            for (java.util.Iterator iter=_selector.selectedKeys().iterator(); 
                    iter.hasNext(); ) {
                SelectionKey key=(SelectionKey)iter.next();
                try {
                    if (key.isReadable()) {       
                        //System.out.println("Read");
                        Connection connection=(Connection)key.attachment();
                        connection.read();  //typically calls MessageRouter.handle
                    }
                } catch (CancelledKeyException e) {
                    //Channel was closed.  Nothing to do.
                } finally {
                    iter.remove();          //remove from selected set
                }
            }

            //Register any writes generated by handling messages above, or
            //by other events.
            synchronized (_needWriteRegister) {
                Iterator iter=_needWriteRegister.iterator(); 
                while (iter.hasNext()) {
                    //System.out.println("Write register");
                    Connection connection=(Connection)iter.next();
                    register(connection, true);
                }
                _needWriteRegister.clear();
            }

            //Handle all writes that failed earlier but now can proceed.  Note
            //that this will typically not include any of the connections added
            //above.
            for (java.util.Iterator iter=_selector.selectedKeys().iterator(); 
                    iter.hasNext(); ) {
                SelectionKey key=(SelectionKey)iter.next();
                try {
                    if (key.isWritable()) {
                        //System.out.println("Write");
                        Connection connection=(Connection)key.attachment();
                        boolean needsMoreWrite=connection.write();
                        //If all data sent, Change registration status to only read.
                        if (! needsMoreWrite)
                            register(connection, false);
                    }
                } catch (CancelledKeyException e) {
                    //Channel was closed.  Nothing to do.
                } finally {
                    iter.remove();          //remove from selected set
                }
            }
            } catch (Exception e) {
                if (_callback!=null)
                    _callback.error(ActivityCallback.INTERNAL_ERROR, e);
            }
        }
    }


    /** 
     * Register c with this.
     * @param write true if READ-WRITE, false if WRITE
     */
    private void register(Connection c, boolean write) {
        try {
            int ops=write ? SelectionKey.OP_READ|SelectionKey.OP_WRITE 
                          : SelectionKey.OP_READ;
            c.channel().register(_selector, ops, c);
        } catch (ClosedChannelException e) {
            //Connection already closed.  Don't bother to register
        }
    }
}
