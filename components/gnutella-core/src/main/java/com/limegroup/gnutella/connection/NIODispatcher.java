/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.*;
import com.sun.java.util.collections.*;

/**
 * @author afisk
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public final class NIODispatcher implements Runnable {
	
	/**
	 * Constant instance of NIOSocketDispatcher following singleton.
	 */
	private static NIODispatcher INSTANCE = new NIODispatcher();
	
	private final ByteBuffer BUFFER = ByteBuffer.allocateDirect(1024);
	
	private static Selector _selector;
	
	private static final List READERS = 
			Collections.synchronizedList(new LinkedList());
			
	private static final List WRITERS = 
		Collections.synchronizedList(new LinkedList());

	
	public static NIODispatcher instance() {
		return INSTANCE;
	}

	/**
	 * Ensure this class cannot be constructed.
	 */
	private NIODispatcher() {
		Thread nioThread = new Thread(this, "nio thread");
		nioThread.setDaemon(true);
		nioThread.start();
	}
	

    public void addReader(Connection conn) {

		System.out.println("NIODispatcher::addReader*********");
		
		//READERS.add(conn);
		//WRITERS.add(conn);
		READERS.add(conn);
		_selector.wakeup();
		//SelectableChannel channel = conn.getSelectableChannel();
		//channel.configureBlocking(false);
		//channel.register(_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, conn);
		System.out.println("NIODispatcher::addReader********* END END END");
	}
	
	public void addWriter(Connection conn) {

		System.out.println("NIODispatcher::addWriter*********");
		
		//READERS.add(conn);
		//WRITERS.add(conn);
		WRITERS.add(conn);
		_selector.wakeup();
		//SelectableChannel channel = conn.getSelectableChannel();
		//channel.configureBlocking(false);
		//channel.register(_selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, conn);
		System.out.println("NIODispatcher::addWriter********* END END END");
	}

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
    	try {
            loopForMessages();
        } catch (IOException e) {
        	e.printStackTrace();
            // TODO not sure what to do here!
        }
    }
    
    private void loopForMessages() throws IOException {
    	System.out.println("NIODispatcher::loopForMessages");
		if(_selector == null) {
			_selector = Selector.open();
		}
		while(true) {
			synchronized(READERS) {
				for(Iterator iter = READERS.iterator(); iter.hasNext();) {
					Connection conn = (Connection)iter.next();
					SelectableChannel channel = conn.getChannel();
					if(channel.isOpen()) {
						System.out.println("NIODispatcher::loopForMessages::registering reader");
						channel.register(_selector, SelectionKey.OP_READ, conn);
					}
				}
				READERS.clear();
			}
			
			
			int n = _selector.select();
			if(n == 0) {
				continue;
			}
			//System.out.println("GOT SELECTION KEY!!!!!!!");
			java.util.Iterator keyIter = _selector.selectedKeys().iterator();
			while(keyIter.hasNext()) {
				SelectionKey key = (SelectionKey)keyIter.next();
				keyIter.remove();
				if(!key.isValid()) continue;
				//System.out.println("NIODispatcher::loopForMessages::GOT A KEY!!!!");
				if(key.isReadable()) {
					try {
						//System.out.println("NIODispatcher::loopForMessages::GOT A READABLE!!!!");
						Message msg = MessageReader.createMessageFromTCP(key);
						
						// TODO:: don't use Message Router
						RouterService.getMessageRouter().handleMessage(msg, (ManagedConnection)key.attachment());
					} catch (IOException e) {
						// TODO record stats for this
					} catch (BadPacketException e) {
						// TODO record stats for this
					}
				}
			}
			
			synchronized(WRITERS) {
				for(Iterator iter = WRITERS.iterator(); iter.hasNext();) {
					Connection conn = (Connection)iter.next();
					registerWriter(conn);
				}
				WRITERS.clear();
			}
			
			keyIter = _selector.selectedKeys().iterator();
			while(keyIter.hasNext()) {
				SelectionKey key = (SelectionKey)keyIter.next();
				keyIter.remove();
				if(!key.isValid()) continue;
				//System.out.println("NIODispatcher::loopForMessages::GOT A KEY!!!!");
				if(key.isWritable()) {
					Connection conn = (Connection)key.attachment();
					if(conn.write()) {
						registerWriter(conn);
					}
					//MessageWriter.write();
				}
			}			
		}
        
    }
    
    private static void registerWriter(Connection conn) {
		SelectableChannel channel = conn.getChannel();
		if(channel.isOpen()) {
				try {
                    channel.register(_selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, 
                    	conn);
                } catch (ClosedChannelException e) {
                    // no problem -- just don't register it
                }
		}
	}
}
