package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * NIO server used to test blocking vs. non-blocking performance.
 */
public class NonBlockingServer implements Runnable {

    public static void main(String[] args) {
        try {
            new NonBlockingServer();
        } catch (IOException e) {
            e.printStackTrace();
        }    
    }
    
    
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
     * Synchronized <tt>List</tt> of new writers that need to be registered for
     * write events.
     */
    private final List WRITERS = 
        Collections.synchronizedList(new LinkedList());
    
    /**
     * Creates a new non-blocking server.
     */
    private NonBlockingServer() throws IOException {
        Selector selector = null;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            // this should hopefully never happen
            ErrorService.error(e);
        }
        SELECTOR = selector;

        Thread selectorThread = new Thread(this, "selector thread");
        selectorThread.start();
        
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ServerSocket ss = ssc.socket();  
        ss.bind(new InetSocketAddress(7777));

        ssc.register(SELECTOR, SelectionKey.OP_ACCEPT);
        
        //while(true) {
          //  Socket client = ss.accept();
            //client.getChannel().configureBlocking(false);
            //addReader(client);
        //}      
    }

    /**
     * Runs the selector event processing thread.
     */
    public void run() {
        try {
            loopForMessages();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    private void addReader(Socket client) {
        READERS.add(new SocketHandler(client));
        SELECTOR.wakeup();
    }

    //private void addWriter(SocketHandler handler) {
      //  WRITERS.add(handler);
        //SELECTOR.wakeup();
    //}
    

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
                e.printStackTrace();
                // windows bug -- need to catch it
                continue;
            } catch(CancelledKeyException e) {
                // this should never happen.
                e.printStackTrace();
                return;
            }
            
            
            // register any new readers...
            registerReaders();
            
            // register any new writers...
            registerWriters();
            
            if(n == 0) {
                continue;
            }
            
            Set selectedKeys = SELECTOR.selectedKeys();
            java.util.Iterator iter = selectedKeys.iterator();
            while(iter.hasNext())  {
                SelectionKey key = (SelectionKey)iter.next();
                
                // remove the current entry 
                //iter.remove();
                
                try {
                    if(key.isAcceptable()) {
                        handleAcceptable(key);
                    }
                    // Check the state of the key.  We need to check all states 
                    // because individual channels can be registered for 
                    // multiple events, so we need to handle all of them.
                    if(key.isReadable())  {
                        handleReader(key);
                    }
                    
                    //else if(key.isWritable())  {
                      //  handleWriter(key);
                    //}
                    
                } catch(CancelledKeyException e) {
                    e.printStackTrace();
                }
                
                
            }
            
            selectedKeys.clear();
        }
    }



    /**
     * @param key
     */
    private void handleAcceptable(SelectionKey key) {
        ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
        
        try {
            //ssc.configureBlocking(false);
            Socket client = ssc.accept().socket();
            
            client.getChannel().configureBlocking(false);
            addReader(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    /**
     * @param key
     */
    private void handleReader(SelectionKey key) {
        SocketHandler handler = (SocketHandler)key.attachment();
        try {
            handler.read();
            handler.write();
        } catch (IOException e) {
            // should not happen
            e.printStackTrace();
        }
    }

    /**
     * @param key
     */
    /*
    private void handleWriter(SelectionKey key) {
        SocketHandler handler = (SocketHandler)key.attachment();
        try {
            handler.write(false);
        } catch (IOException e) {
            // should not happen
            e.printStackTrace();
        }
    }
    */

    
    /**
     * Registers any new connections that should be registered for
     * read events.
     */
    private void registerReaders() {
        synchronized(READERS) {
            // do nothing if there are no new readers
            //if(READERS.isEmpty()) return;
            for(Iterator iter = READERS.iterator(); iter.hasNext();) {
                SocketHandler sh = (SocketHandler)iter.next();
                
                try {                        
                    sh.getChannel().register(SELECTOR, SelectionKey.OP_READ, sh);
                } catch (ClosedChannelException e) {
                    // this should not happen
                    e.printStackTrace();
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
            //if(WRITERS.isEmpty()) return;
            for(Iterator iter = WRITERS.iterator(); iter.hasNext();) {
                SocketHandler sh = (SocketHandler)iter.next();
                SocketChannel channel = sh.getChannel();

                try {
                    channel.register(SELECTOR, 
                        SelectionKey.OP_WRITE | SelectionKey.OP_READ, sh);
                } catch (ClosedChannelException e) {
                    // should not happen
                    e.printStackTrace();
                }
            }
            WRITERS.clear();
        }
    }
    
    /**
     * Takes care of reads and writes to a specific connection.
     */
    private final class SocketHandler {
        //final Socket CLIENT;
        
        final SocketChannel CHANNEL;
        
        final ByteBuffer READ_BUFFER = ByteBuffer.allocate(20);

        final ByteBuffer WRITE_BUFFER = ByteBuffer.allocate(12);

        int _numberOfWrites = 0;

        SocketHandler(Socket client) {
            CHANNEL = client.getChannel();
            WRITE_BUFFER.put("hello client".getBytes());
            WRITE_BUFFER.flip();
        }
        
        SocketChannel getChannel() {
            return CHANNEL;
        }

        /**
         * @return
         */
        public boolean write() throws IOException {
            CHANNEL.write(WRITE_BUFFER);
            if(WRITE_BUFFER.hasRemaining()) {
                System.out.println("could not write");
                return false;
            }
            WRITE_BUFFER.flip();
            return true;
        }

        void read() throws IOException {
            CHANNEL.read(READ_BUFFER);
            
            //byte[] readBytes = new byte[READ_BUFFER.position()];
            //READ_BUFFER.flip();
            //READ_BUFFER.get(readBytes);
            
            //System.out.println(new String(readBytes));
            READ_BUFFER.clear();
        }
    }

}
