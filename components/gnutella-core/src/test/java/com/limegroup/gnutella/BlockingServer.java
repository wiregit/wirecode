package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Test blocking server that accepts a variable number of connections.  This is
 * used to test against a non-blocking server to more accurately measure 
 * performance differences between the two implementations for different 
 * numbers of connections.
 */
public class BlockingServer {
    
    public static void main(String[] args) {
        try {
            new BlockingServer();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Constant for the port to use.
     */
    final static int PORT = 7777;
    

    /**
     * Creates a new blocking server that continually reads and sends responses.
     */
    public BlockingServer() throws IOException {
        startServer();
    }
    
    private void startServer() {
        Thread serverSocketThread = new Thread(new Runnable() {
            public void run() {
                try {
                    ServerSocket server = new ServerSocket(PORT);
                    while(true) {
                        Socket client = server.accept();
                        new SocketHandler(client);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "server socket thread");
        
        serverSocketThread.start();
    }
    
    /**
     * Utility class that reads and writes messages from and to an individual
     * socket.
     */
    private static final class SocketHandler implements Runnable {
        
        private final Socket CLIENT;
        
        private final byte[] READ_BYTES = new byte[20];
        
        
        
        private final SocketWriter WRITER;
        
        SocketHandler(Socket client) {
            CLIENT = client;
            WRITER = new SocketWriter(client);
            Thread writeThread = new Thread(WRITER, "writer");
            writeThread.setDaemon(true);
            writeThread.start();

            Thread readThread = new Thread(this, "reader");
            readThread.setDaemon(true);
            readThread.start();
        }
        
        public void run() {
            try {
                read();
            } catch (IOException e) {
                e.printStackTrace();
            }         
        }

        private void read() throws IOException {
            InputStream is = CLIENT.getInputStream();
            while(true) {
                is.read(READ_BYTES);
                System.out.println(new String(READ_BYTES));
                WRITER.addWriter();
            }
        }
    }
    

    /**
     * Utility class for writing messages to an individual socket.  This just
     * continually writes the same "hello client" message.
     */
    private static final class SocketWriter implements Runnable {
        
        private int _writesToPerform;

        private final Socket CLIENT;
        
        private final Object QUEUE_LOCK = new Object();
        
        private final byte[] WRITE_BYTES = "hello client".getBytes();
        
        SocketWriter(Socket client) {
            CLIENT = client;    
        }
        
        public void addWriter() {
            synchronized(QUEUE_LOCK) {
                _writesToPerform++;
                QUEUE_LOCK.notify();
            }
        }
        
        public void run() {
            while(true) {
                waitForWrites();
                try {
                    write();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void waitForWrites() {
            synchronized(QUEUE_LOCK) {
                while(_writesToPerform == 0) {
                    try {
                        QUEUE_LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        private void write() throws IOException, InterruptedException {
            OutputStream os = CLIENT.getOutputStream();
            while(_writesToPerform > 0) {
                os.write(WRITE_BYTES);
            }
        }
    }
}
