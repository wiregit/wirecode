package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.messages.Message;

/**
 * Blocking message writer.  Blocks writing messages to the network.  This 
 * class MUST only be used if the user is not running Java 1.4.x, or the 
 * properties specify not to use NIO.
 */
public final class BIOMessageWriter implements MessageWriter, Runnable {

    /**
     * The <tt>ManagedConnection</tt> instance for this writer.
     */
    private final ManagedConnection CONNECTION;
    
    /** 
     * A lock to protect changes to the message queue for this connection. 
     */
    private final Object OUTPUT_QUEUE_LOCK = new Object();
    
    /**
     * Cache the 'connection closed' exception, so we have to allocate
     * one for every closed connection.
     */
    protected static final IOException CONNECTION_CLOSED =
        new IOException("connection closed");    

    /**
     * Handle to the message queue that keeps track of priorities for messages
     * to be sent.
     */
    private CompositeQueue _queue;
    
    /**
     * Creates a new <tt>MessageWriter</tt> instance for the specified 
     * connection.
     * 
     * @param conn the <tt>ManagedConnection</tt> containing this writer
     * @return a new <tt>MessageWriter</tt> instance
     */
    public static MessageWriter createWriter(ManagedConnection conn) {
        return new BIOMessageWriter(conn);    
    }
    
    /**
     * Constructor for creating a new blocking message writer instance.
     * 
     * @param conn the <tt>ManagedConnection</tt> for this writer
     */
    private BIOMessageWriter(ManagedConnection conn) {
        CONNECTION = conn;        
    }
    
    /**
     * Starts the thread for this reader.  
     */
    public void start() {
        Thread blockingReader = new Thread(this, "blocking message reader");
        blockingReader.setDaemon(true);
        blockingReader.start();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#write()
     */
    public boolean write() throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#write(com.limegroup.gnutella.messages.Message)
     */
    public boolean write(Message m) throws IOException {
        synchronized (OUTPUT_QUEUE_LOCK) {
            if(_queue == null) {
                _queue = CompositeQueue.createQueue(CONNECTION);
            }
            
            _queue.add(m);
            OUTPUT_QUEUE_LOCK.notify();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageWriter#hasPendingMessage()
     */
    public boolean hasPendingMessage() {
        // TODO Auto-generated method stub
        return false;
    }


    /** While the connection is not closed, sends all data delay. */
    public void run() {
        //Exceptions are only caught to set the _runnerDied variable
        //to make testing easier.  For non-IOExceptions, Throwable
        //is caught to notify ErrorService.
        try {
            while (true) {
                repOk();
                waitForQueued();
                sendQueued();
                repOk();
            }                
        } catch (IOException e) {
            CONNECTION.setSenderDied(true);
        } catch(Throwable t) {
            CONNECTION.setSenderDied(true);     
            ErrorService.error(t);
        }
    }

    /** 
     * Wait until the queue is (probably) non-empty or closed. 
     * @exception IOException this was closed while waiting
     */
    private final void waitForQueued() throws IOException {
        //The synchronized statement is outside the while loop to
        //protect _queued.
        synchronized (OUTPUT_QUEUE_LOCK) {
            while (CONNECTION.isOpen() && 
                   _queue.size()==0) {           
                try {
                    OUTPUT_QUEUE_LOCK.wait();
                } catch (InterruptedException e) {
                    Assert.that(false, "OutputRunner Interrupted");
                }
            }
        }
        
        if (! CONNECTION.isOpen())
            throw CONNECTION_CLOSED;
    }
    
    /** Send several queued message of each type. */
    private final void sendQueued() throws IOException {        
        //1. For each priority i send as many messages as desired for that
        //type.  As an optimization, we start with the buffer of the last
        //message sent, wrapping around the buffer.  You can also search
        //from 0 to the end.
        //int start = _lastPriority;
        //int i=start;
        //do {                   
            //IMPORTANT: we only obtain _outputQueueLock while touching the
            //queue, not while actually sending (which can block).
            //boolean emptied = false;
            while (true) {
                Message msg = null;
                synchronized (OUTPUT_QUEUE_LOCK) {
                    msg = _queue.removeNext();
                    //int dropped=queue.resetDropped();
                    //addSentDropped(dropped);
                    //_queued-=(m==null?0:1)+dropped;  //maintain invariant
                    //if (_queue.size() == 0) {
                      //  emptied = true;        
                    //}
                    
                    // if m is null, the queue is empty                
                    if (msg == null)
                        break;
                }

                //Note that if the ougoing stream is compressed
                //(isWriteDeflated()), this call may not actually
                //do anything.  This is because the Deflater waits
                //until an optimal time to start deflating, buffering
                //up incoming data until that time is reached, or the
                //data is explicitly flushed.
                CONNECTION.send(msg);
            }
            
            //Optimization: the if statement below is not needed for
            //correctness but works nicely with the _priorityHint trick.
            //if (emptied)
              //  break;
            //i=(i+1)%PRIORITIES;
        //} while (i!=start);
        
        
        //2. Now force data from Connection's BufferedOutputStream into the
        //kernel's TCP send buffer.  It doesn't force TCP to
        //actually send the data to the network.  That is determined
        //by the receiver's window size and Nagle's algorithm.
        //Note that if the outgoing stream is compressed 
        //(isWriteDeflated()), then this call may block while the
        //Deflater deflates the data.
        CONNECTION.flush();
    }

    /** 
     * Tests representation invariants.  For performance reasons, this is
     * private and final.  Make protected if ManagedConnection is subclassed.
     */
    private final void repOk() {
        /*
        //Check _queued invariant.
        synchronized (_outputQueueLock) {
            int sum=0;
            for (int i=0; i<_outputQueue.length; i++) 
                sum+=_outputQueue[i].size();
            Assert.that(sum==_queued, "Expected "+sum+", got "+_queued);
        }
        */
    }
}
