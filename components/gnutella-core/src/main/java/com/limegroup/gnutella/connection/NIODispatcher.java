package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.statistics.MessageReadErrorStat;
import com.limegroup.gnutella.util.NetworkUtils;
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
	private final Selector SELECTOR;
	
	/**
	 * Synchronized <tt>List</tt> of new readers that need to be registered for
     * read events.
	 */
	private final List READERS = 
		Collections.synchronizedList(new LinkedList());
    
    /**
     * Synchronized <tt>List</tt> of outgoing connections that need their 
     * connections to be completed.
     */    
    private final List CONNECTORS =
        Collections.synchronizedList(new LinkedList());
	
	/**
	 * Synchronized <tt>List</tt> of new writers that need to be registered for
     * write events.
	 */
	private final List WRITERS = 
		Collections.synchronizedList(new LinkedList());

	/**
     * Flag used only for testing -- set using PrivilegedAccessor.
	 */
    private boolean _testing;
    
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
        Selector selector = null;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            // this should hopefully never happen
            ErrorService.error(e);
        }
        SELECTOR = selector;
		Thread nioThread = new Thread(this, "nio thread");
		nioThread.setDaemon(true);
		nioThread.start();
	}
	
	/**
	 * Adds the specified <tt>Connection</tt> as needing to be registered 
     * for read events.  Read events occur whenever data comes in from the 
     * network on that channel.
	 * 
	 * @param conn the <tt>Connection</tt> instance that will be reading 
     *  data from the network
     * @throws NullPointerException if the <tt>conn</tt> argument is 
     *  <tt>null</tt>
	 */
    public void addReader(Connection conn) {
        if(conn == null) {
            throw new NullPointerException("adding null connection");
        }
		READERS.add(conn);
		SELECTOR.wakeup();
	}
	
	/**
	 * Adds the specified <tt>Connection</tt> as needing an additional 
     * write.  This is only called when a former call to write on the channel 
     * did not write all of the requested data.  In that case, this method 
     * should be called so that the connection registers itself for write 
     * events.  Write events occur whenever there is room in the TCP buffers, 
     * allowing the unwritten data to go through.
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
		SELECTOR.wakeup();
	}
    
    /**
     * Adds the specified <tt>Connection</tt> to the list of connections that
     * need to have their TCP connections completed -- they need to be fully
     * connected.
     * 
     * @param conn the <tt>Connection</tt> whose TCP connection is not yet 
     *  fully established
     */
    public void addConnector(Connection conn)  {
        if(conn == null)  {
            throw new NullPointerException("adding null connection");
        }
        CONNECTORS.add(conn);
        
        SELECTOR.wakeup();
    }

    /**
     * Runs the selector event processing thread.
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
        int n = -1;
		while(true) {
            
            try {
                n = SELECTOR.select();
            } catch(NullPointerException e) {
                // windows bug -- need to catch it
                continue;
            } catch(CancelledKeyException e) {
                // this can also happen even though it's not supposed to --
                // just continue
                continue;
            }
            
            // register any new connectors...
            registerConnectors();
			
			// register any new readers...
			registerReaders();
            
            // register any new writers...
            registerWriters();
            
			if(n == 0) {
				continue;
			}
            
            java.util.Iterator iter = SELECTOR.selectedKeys().iterator();
            while(iter.hasNext())  {
                SelectionKey key = (SelectionKey)iter.next();
                
                // remove the current entry 
                iter.remove();
                
                // Check the state of the key.  We need to check all states 
                // because individual channels can be registered for multiple 
                // events, so we need to handle all of them.
                if(key.isReadable())  {
                    handleReader(key);
                }
                
                if(key.isWritable())  {
                    handleWriter(key);
                }
                
                if(key.isConnectable())  {
                    handleConnector(key);
                }
            }
		}
    }
    
    /**
     * Registers any new connections for connect events.  This is relevant
     * only for new outgoing sockets that finish the TCP connect process
     * without blocking.
     */
    private void registerConnectors()  {
        synchronized(CONNECTORS)  {
            if(CONNECTORS.isEmpty()) return;
            for(Iterator iter = CONNECTORS.iterator(); iter.hasNext();) {
                Connection conn = (Connection)iter.next();
                try {
                    conn.getSocket().
                        getChannel().register(SELECTOR, 
                            SelectionKey.OP_CONNECT, conn);
                } catch (ClosedChannelException e) {
                    continue;
                }
            }
            CONNECTORS.clear();
        }
    }
    
    /**
     * Handles a connection event for an individual channel.  This responds to
     * the event by attempting to complete TCP connection establishment on the
     * channel.
     * 
     * @param key the <tt>SelectionKey</tt> for the connection event
     */
    private void handleConnector(SelectionKey key)  {
        SocketChannel sc = (SocketChannel)key.channel();
        
        // if the connection does not need completing, return
        if(!sc.isConnectionPending()) return;        
        // Attempt to complete the connection sequence
        try {
            if(sc.finishConnect()) {
                key.cancel();
                
                // now that we're fully connected, finish the connection 
                // handshaking
                Connection conn = (Connection)key.attachment();
                addReader(conn);
                if(!conn.handshaker().handshake())  {
                    addWriter(conn);
                }
            }
        } catch (IOException e) {
            // TODO: we need to notify ConnectionManager to remove this from
            // its connecting list
            NetworkUtils.close(sc.socket());
            try {                        
                sc.close();
            } catch(IOException ioe) {
                // nothing to do
            }
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
				
				// try to make sure the channel's open instead of just
				// hammering our way into ClosedChannelExceptions -- 
				// more efficient and cleaner
				if(channel.isOpen()) {
					try {
                        channel.register(SELECTOR, SelectionKey.OP_READ, conn);
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
		java.util.Iterator keyIter = SELECTOR.selectedKeys().iterator();
		while(keyIter.hasNext()) {
			SelectionKey key = (SelectionKey)keyIter.next();
			keyIter.remove();
			
			// ignore invalid keys
			if(!key.isValid()) continue;
            if(!key.isReadable()) continue;
            Connection conn = (Connection)key.attachment();
			try {
                MessageReader reader = conn.reader();
				Message msg = reader.createMessageFromTCP(key);
				
				if(msg == null) {
                    // the message was not read completely -- we'll get
                    // another read event on the channel and keep reading
					continue;
				}

                reader.routeMessage(msg);
			} catch (BadPacketException e) {
                MessageReadErrorStat.BAD_PACKET_EXCEPTIONS.incrementStat();
			} catch (IOException e) {
                // remove the connection if we got an IOException
                RouterService.removeConnection(conn);
                MessageReadErrorStat.IO_EXCEPTIONS.incrementStat();
			}
		}
	}
    
    /**
     * Handles a read event for the specified <tt>SelectionKey</tt> instance 
     * and it's contained channel.  The event indicates that there's data
     * available for reading from the channel, and we should read it.
     * 
     * @param key the <tt>SelectionKey</tt> for the read event, containing the
     *  channel with data to read
     */
    private void handleReader(SelectionKey key)  {
        // ignore invalid keys
        if(!key.isValid()) return;
        Connection conn = (Connection)key.attachment();
        if(conn.handshaker().handshakeComplete())  {
            try {
                MessageReader reader = conn.reader();
                Message msg = reader.createMessageFromTCP(key);
                    
                if(msg == null) {
                    // the message was not read completely -- we'll get
                    // another read event on the channel and keep reading
                    return;
                }
    
                reader.routeMessage(msg);
            } catch (BadPacketException e) {
                MessageReadErrorStat.BAD_PACKET_EXCEPTIONS.incrementStat();
            } catch (IOException e) {
                // remove the connection if we got an IOException
                RouterService.removeConnection(conn);
                MessageReadErrorStat.IO_EXCEPTIONS.incrementStat();
            }
        } else  {
            try {
                conn.handshaker().read();
            } catch (IOException e) {
                // TODO: remove from initializing connections in ConnectionManager
            }
        }
    }
	
	/**
	 * Writes any new data to the network.  Writers are only registered for
     * write events when the TCP buffers have filled up, causing messages 
     * to not be fully written in previous calls to Channel.write(...).
	 */
	private void handleWriters() {
		
		java.util.Iterator keyIter = SELECTOR.selectedKeys().iterator();
		while(keyIter.hasNext()) {
			SelectionKey key = (SelectionKey)keyIter.next();
			keyIter.remove();
			if(!key.isValid()) continue;
			if(key.isWritable()) {
				Connection conn = (Connection)key.attachment();
				try {
                    if(conn.write()) {
                        // if the message was successfully written, switch it 
                        // back to only being registered for read events
                        register(conn, SelectionKey.OP_READ);
                        conn.setWriteRegistered(false);
                    } 
                } catch (IOException e) {
                    RouterService.removeConnection(conn);
                }
			}
		}			
	}
    
    /**
     * Writes any new data to the network.  Writers are only registered for
     * write events when the TCP buffers have filled up, causing messages 
     * to not be fully written in previous calls to Channel.write(...).
     * 
     * @param key the <tt>SelectionKey</tt> for the channel that's ready to
     *  write
     */
    private void handleWriter(SelectionKey key)  {
        if(!key.isValid()) return;

        Connection conn = (Connection)key.attachment();
        try {
            if(conn.write()) {
                // if the message was successfully written, switch it 
                // back to only being registered for read events
                register(conn, SelectionKey.OP_READ);
                conn.setWriteRegistered(false);
            } 
        } catch (IOException e) {
            RouterService.removeConnection(conn);
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
                channel.register(SELECTOR, ops, conn);
            } catch (ClosedChannelException e) {
                // no problem -- just don't register it
            }
		}
	}
}
