package com.limegroup.gnutella;

import java.net.*;
import java.io.*;

/**
 * Listens on ports, accepts incoming connections, and dispatches
 * threads to handle those connections.  Currently HTTP and
 * limited HTTP connections over TCP are supported; more may
 * be supported in the future.<p>
 *
 * <i>TODO: Currently this doesn't actually do the listening; that is still
 * done by ConnectionManager.  This will change in the future,
 * however.</i>
 */
public class Acceptor {
    private ConnectionManager manager;

    /** 
     * Creates a new Acceptor that will pass incoming Gnutella
     * connections to manager.  <i>TODO: this will take more arguments
     * in the future.</i> 
     */
    public Acceptor(ConnectionManager manager) {
        this.manager=manager;
    }
        
    /** 
     * @modifies socket, this' managers
     * @effects starts a new thread to handle the given socket and
     *  registers it with the appropriate protocol-specific manager.
     *  Returns once the thread has been started.  If socket does
     *  not speak a known protocol, closes the socket immediately and 
     *  returns.  
     */
    public void dispatch(Socket socket) {
        Thread t=new Thread(new ConnectionDispatchRunner(socket), "ConnectionDispatchRunner");
        t.setDaemon(true);
        t.start();
    }

    private class ConnectionDispatchRunner implements Runnable {
        private Socket socket;
        ConnectionDispatchRunner(Socket socket) {
            this.socket=socket;
        }
        
        public void run() {        
            try {
                InputStream in=socket.getInputStream();
                socket.setSoTimeout(SettingsManager.instance().getTimeout());
                String word=readWord(in);               
                socket.setSoTimeout(0);

                //1. Gnutella connection
                if (word.equals(SettingsManager.instance().getConnectStringFirstWord())) {
                    //a) Normal case
                    if (manager.getNumConnections() 
                            < SettingsManager.instance().getMaxConn()) {
                        Connection c = new Connection( 
                            socket.getInetAddress().getHostAddress(), 
                            socket.getPort(), true);
                        manager.tryingToConnect(c, true);
                        try {
                            c.initIncoming(socket); 
                        } catch (IOException e) {
                            manager.failedToConnect(c);
                            throw e;
                        }
                        c.setManager(manager);
                        manager.add(c);      
                        Thread.currentThread().setName("Connection (incoming)");
                        c.run();   //Ok, since we've already spawned a thread.
                    }
                    //b) We have more connections than we can handle. Be polite.
                    else{
                        RejectConnection rc = new RejectConnection(socket);
                        rc.setManager(manager);
                        rc.run();  //Ok, since we've already spawned a thread.
                    }
                } 
                //2. Incoming upload via HTTP
                else if (word.equals("GET")) {
                    HTTPManager mgr = new HTTPManager(socket, manager, false);
                } 
                //3. Incoming download via push/HTTP.
                else if (word.equals("GIV")) {
                    HTTPManager mgr = new HTTPManager(socket, manager, true);
                }
                //4. Unknown protocol
                else {
                    throw new IOException();
                }
            } catch (IOException e) { 
                //handshake failed: try to close connection.
                try { socket.close(); } catch (IOException e2) { }          
            }
        }
    }


    /** 
     * @modifies sock
     * @effects Returns the first word (i.e., no whitespace) of less
     *  than 8 characters read from sock, or throws IOException if none
     *  found. 
     */
    private static String readWord(InputStream sock) throws IOException {
        final int N=9;  //number of characters to look at
        char[] buf=new char[N];
        for (int i=0 ; i<N ; i++) {
            int got=sock.read();
            if (got==-1)  //EOF
                throw new IOException();
            if ((char)got==' ') { //got word.  Exclude space.
                return new String(buf,0,i);
            }
            buf[i]=(char)got;
        }
        throw new IOException();            
    }
}





