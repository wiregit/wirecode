package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.statistics.MessageReadErrorStat;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.util.CommonUtils;
import com.sun.java.util.collections.Collections;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.LinkedList;
import com.sun.java.util.collections.List;

/**
 * This class handles the use of non-blocking sockets for systems using
 * Java 1.4 and above.  For these systesm, non-blocking sockets allow us
 * to use one thread for all message processing and to offload socket 
 * selection to the operating system.
 */
public final class NIODispatcher implements Runnable {
	
	/**
	 * Constant instance of NIOSocketDispatcher following singleton.
	 */
	private static NIODispatcher INSTANCE = new NIODispatcher();
	
	/**
	 * Constant <tt>Selector</tt> for demultiplexing incoming traffic.
	 */
	private Selector _selector;
	
	/**
	 * Synchronized <tt>List</tt> of new readers that need to be registered for
     * read events.
	 */
	private final List READERS = 
		Collections.synchronizedList(new LinkedList());
	
	/**
	 * Synchronized <tt>List</tt> of new writers that need to be registered for
     * write events.
	 */
	private final List WRITERS = 
		Collections.synchronizedList(new LinkedList());

	
	/**
	 * Accessor for the <tt>NIODispatcher</tt> instance.
	 * 
     * @return the <tt>NIODispatcher</tt> instance
	 */
	public static NIODispatcher instance() {
		return INSTANCE;
	}

	/**
	 * Constructs the single <tt>NIODispatcher</tt> instance and starts its 
     * thread.
	 */
	private NIODispatcher() {
		Thread nioThread = new Thread(this, "nio thread");
		nioThread.setDaemon(true);
		nioThread.start();
	}
	
	/**
	 * Adds the specified <tt>Connection</tt> as needing to be registered for 
     * read events.  Read events occur whenever data comes in from the network 
     * on that channel.
	 * 
	 * @param conn the <tt>Connection</tt> instance that will be reading data 
     * from the network
     * @throws NullPointerException if the <tt>conn</tt> argument is 
     *  <tt>null</tt>
	 */
    public void addReader(Connection conn) {
        if(conn == null) {
            throw new NullPointerException("adding null connection");
        }
		READERS.add(conn);
		_selector.wakeup();
	}
	
	/**
	 * Adds the specified <tt>Connection</tt> as needing an additional write.  
     * This is only called when a former call to write on the channel did not 
     * write all of the requested data.  In that case, this method should be 
     * called so that the connection registers itself for write events.  Write 
     * events occur whenever there is room in the TCP buffers, allowing the 
     * unwritten data to go through.
	 * 
	 * @param conn the <tt>Connection</tt> instance containing a message that 
     *  was not fully written
     * @throws NullPointerException if the <tt>conn</tt> argument is 
     *  <tt>null</tt>
	 */
	public void addWriter(Connection conn) {
        if(conn == null) {
            throw new NullPointerException("adding null connection");
        }
		WRITERS.add(conn);
		
		// we have not added this writer to the selector yet, as this call is 
        // made from a separate thread, so we need to wake up the selector so 
        // that it doesn't keep blocking in select() and instead registers the 
        // new Channel for write events
		_selector.wakeup();
	}

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
    	try {
            loopForMessages();
        } catch (Throwable e) {
        	ErrorService.error(e);
        }
    }
    
	/**
     * Handles the registering of new channels for reading and writing of
	 * messages and the handling of read and written messages.
	 *
	 * @throws IOException if the <tt>Selector</tt> is not opened successfully
	 */
    private void loopForMessages() throws IOException {
		if(_selector == null) {
			_selector = Selector.open();
		}
		while(true) {
            
			int n = -1;
            
            try {
                n = _selector.select();
            } catch(NullPointerException e) {
                // windows bug -- need to catch it
                continue;
            } catch(CancelledKeyException e) {
                // this can also happen even though it's not supposed to --
                // just continue
                continue;
            }
			
			// register any new readers and writers
			registerReaders();
			if(n == 0) {
				continue;
			}
			handleReaders();
			
			registerWriters();
			handleWriters();
		}
    }
    
    /**
     * Registers any new connections that should be registered for
	 * read events.
     */
    private void registerReaders() {
        synchronized(READERS) {
			// do nothing if there are no new readers
			if(READERS.isEmpty()) return;
			for(Iterator iter = READERS.iterator(); iter.hasNext();) {
				Connection conn = (Connection)iter.next();
				SelectableChannel channel = conn.getSocket().getChannel();
                Assert.that(channel != null, "channel is null");
				
				// try to make sure the channel's open instead of just
				// hammering our way into ClosedChannelExceptions -- 
				// more efficient and cleaner
				if(channel.isOpen()) {
					try {
                        channel.register(_selector, SelectionKey.OP_READ, conn);
                    } catch (ClosedChannelException e) {
                    	// keep registering the other connections
                        continue;
                    }
				}
			}
			READERS.clear();
		}  
    }

	 /**
     * Registers any new connections that should be registered for
	 * write events.
     */
    private void registerWriters() {
        synchronized(WRITERS) {
			// do nothing if there are no new writers
			if(WRITERS.isEmpty()) return;
			for(Iterator iter = WRITERS.iterator(); iter.hasNext();) {
				Connection conn = (Connection)iter.next();
                register(conn, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
			}
			WRITERS.clear();
		}
    }

    

    /**
     * Reads any new messages from all connections and dispatches
	 * them to the message processing infrastructure.
     */
	private void handleReaders() {
		java.util.Iterator keyIter = _selector.selectedKeys().iterator();
		while(keyIter.hasNext()) {
			SelectionKey key = (SelectionKey)keyIter.next();
			keyIter.remove();
			
			// ignore invalid keys
			if(!key.isValid()) continue;
			if(key.isReadable()) {
                ManagedConnection mc = null;
				try {
                    mc = (ManagedConnection)key.attachment();
                    if(!mc.isOpen()) {
                        // continue if the connection is no longer open
                        continue;
                    }
					Message msg = mc.getReader().createMessageFromTCP(key);
					
					if(msg == null) {
                        // the message was not read completely -- we'll get
                        // another read event on the channel and keep reading
						continue;
					}

                    mc.addReceived();
                    // make sure this message isn't considered spam                    
                    if(!mc.isSpam(msg)) {
                        // TODO:: don't use RouterService
                        RouterService.getMessageRouter().handleMessage(msg, 
                            (ManagedConnection)key.attachment());
                    } else {
                        if(!CommonUtils.isJava118()) {
                            ReceivedMessageStatHandler.TCP_FILTERED_MESSAGES.
                                addMessage(msg);
                        }
                        mc.addReceivedDropped();
                    }
				} catch (BadPacketException e) {
                    MessageReadErrorStat.BAD_PACKET_EXCEPTIONS.incrementStat();
				} catch (IOException e) {
                    // remove the connection if we got an IOException
                    RouterService.removeConnection(mc);
                    MessageReadErrorStat.IO_EXCEPTIONS.incrementStat();
				}
			}
		}
	}
	
	/**
	 * Writes any new data to the network.  Writers are only registered for
     * write events when the TCP buffers have filled up, causing messages 
     * to not be fully written in previous calls to Channel.write(...).
	 */
	private void handleWriters() {
		
		java.util.Iterator keyIter = _selector.selectedKeys().iterator();
		while(keyIter.hasNext()) {
			SelectionKey key = (SelectionKey)keyIter.next();
			keyIter.remove();
			if(!key.isValid()) continue;
			if(key.isWritable()) {
				Connection conn = (Connection)key.attachment();
				if(conn.getWriter().write()) {
                    // if the message was successfully written, switch it back 
                    // to only being registered for read events
                    register(conn, SelectionKey.OP_READ);
				}
			}
		}			
	}
    
    /**
     * Helper method that registers the given <tt>Connection</tt> for future 
     * write events (for future open space in our TCP buffers).
     * 
     * @param conn the <tt>Connection</tt> whose channel should be registered
     *  for write events
     */
    private void register(Connection conn, int ops) {
		SelectableChannel channel = conn.getSocket().getChannel();

		// try to make sure the channel's open instead of just
		// hammering our way into ClosedChannelExceptions -- 
		// more efficient and cleaner
		if(channel.isOpen()) {
			try {
                channel.register(_selector, ops, conn);
            } catch (ClosedChannelException e) {
                // no problem -- just don't register it
            }
		}
	}
}
