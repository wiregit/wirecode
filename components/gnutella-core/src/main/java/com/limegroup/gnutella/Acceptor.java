package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * Listens on ports, accepts incoming connections, and dispatches
 * threads to handle those connections.  Currently HTTP and
 * limited HTTP connections over TCP are supported; more may
 * be supported in the future.<p>
 */
public class Acceptor extends Thread {
    /**
     * The socket that listens for incoming connections. Can be changed to
     * listen to new ports.
     *
     * LOCKING: obtain _socketLock before modifying either.  Notify _socketLock
     * when done.
     */
    private volatile ServerSocket _socket=null;
    private int _port=0;
    private Object _socketLock=new Object();
    private byte[] _address;

    private Vector _badHosts = new Vector();

    private ConnectionManager _manager;
    private MessageRouter _router;
    private ActivityCallback _callback;

    /**
     * Creates an acceptor that listens on the default port. Equivalent to
     * Acceptor(SettingsManager.instance().getPort())
     */
    public Acceptor(ActivityCallback callback) {
        this(SettingsManager.instance().getPort(), callback);
    }

    /**
     * Creates an acceptor that tries to listen to incoming connections
     * on the given port.  If this is a bad port, the port will be
     * changed when run is called and SettingsManager will be updated.
     * If that fails, ActivityCallback.error will be called.  A port
     * of 0 means do not accept incoming connections.
     */
    public Acceptor(int port, ActivityCallback callback) {
        _port = port;
        _callback = callback;

        try {
            _address = InetAddress.getLocalHost().getAddress();
        } catch (Exception e) {
            //In case of UnknownHostException or SecurityException, we have
            //no choice but to use a fake address: all zeroes.
            _address = new byte[4];
        }

        String[] allHosts = SettingsManager.instance().getBannedIps();
        for (int i=0; i<allHosts.length; i++)
            _badHosts.add(allHosts[i]);
    }

    /**
     * Links the HostCatcher up with the other back end pieces and launches
     * the port monitoring thread
     */
    public void initialize(ConnectionManager manager, MessageRouter router) {
        _manager = manager;
        _router = router;

        setDaemon(true);
        start();
    }

    /**
     * Returns this' address to use for ping replies, query replies,
     * and pushes.
     */
    public byte[] getAddress() {
        //TODO3: if FORCE_LOCAL_IP is true, then use that value instead.
        //       (Alternative implementation: just set this._address accordingly
        //        during initialization.)
        return _address;
    }

    /**
     * Returns the port at which the Connection Manager listens for incoming
     * connections
     * @return the listening port
     */
    public int getPort() {
        return _port;
    }

    /**
     * @requires only one thread is calling this method at a time
     * @modifies this
     * @effects sets the port on which the ConnectionManager is listening.
     *  If that fails, this is <i>not</i> modified and IOException is thrown.
     *  If port==0, tells this to stop listening to incoming connections.
     *  This is properly synchronized and can be called even while run() is
     *  being called.
     */
    public void setListeningPort(int port) throws IOException {
        //1. Special case: if unchanged, do nothing.
        if (_socket!=null && _port==port)
            return;
        //2. Special case if port==0.  This ALWAYS works.
        //Note that we must close the socket BEFORE grabbing
        //the lock.  Otherwise deadlock will occur since
        //the acceptor thread is listening to the socket
        //while holding the lock.  Also note that port
        //will not have changed before we grab the lock.
        else if (port==0) {
            //Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } catch (IOException e) { }
            }
            synchronized (_socketLock) {
                _socket=null;
                _port=0;
                _socketLock.notify();
            }
            return;
        }
        //3. Normal case.  See note about locking above.
        else {
            //a) Try new port.
            ServerSocket newSocket=null;
            try {
                newSocket=new ServerSocket(port);
            } catch (IOException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                throw new IOException();
            }
            //b) Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } catch (IOException e) { }
            }
            //c) Replace with new sock.  Notify the accept thread.
            synchronized (_socketLock) {
                _socket=newSocket;
                _port=port;
                _socketLock.notify();
            }
            return;
        }
    }

    /** @modifies this, network, SettingsManager
     *  @effects accepts new incoming connections on a designated port
     *   and services incoming requests.  If the port was changed
     *   in order to accept incoming connections, SettingsManager is
     *   changed accordingly.
     */
    public void run() {
        // Create the server socket, bind it to a port, and listen for
        // incoming connections.  If there are problems, we can continue
        // onward.
        //1. Try suggested port.
        int oldPort = _port;
        try {
            setListeningPort(_port);
        } catch (IOException e) {
            //2. Try 10 different ports
            for (int i=0; i<10; i++) {
                _port=i+6346;
                try {
                    setListeningPort(_port);
                } catch (IOException e2) { }
            }

            // If we still don't have a socket, there's an error
            if(_socket == null)
                _callback.error(ActivityCallback.ERROR_0);
        }

        if (_port!=oldPort) {
            SettingsManager.instance().setPort(_port);
            _callback.setPort(_port);
        }

        while (true) {
            try {
                //Accept an incoming connection, make it into a
                //Connection object, handshake, and give it a thread
                //to service it.  If not bound to a port, wait until
                //we are.  If the port is changed while we are
                //waiting, IOException will be thrown, forcing us to
                //release the lock.
                Socket client=null;
                synchronized (_socketLock) {
                    if (_socket!=null) {
                        try {
                            client=_socket.accept();
                        } catch (IOException e) {
                            continue;
                        }
                    } else {
                        // When the socket lock is notified, the socket will
                        // be available.  So, just wait for that to happen and
                        // go around the loop again.
                        try {
                            _socketLock.wait();
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                }

                //Check if IP address of the incoming socket is in _badHosts
                if (_badHosts.contains(
                        client.getInetAddress().getHostAddress())) {
                    client.close();
                    continue;
                }

                //Dispatch asynchronously.
                new ConnectionDispatchRunner(client);

            } catch (SecurityException e) {
                _callback.error(ActivityCallback.ERROR_3);
                return;
            } catch (Exception e) {
                //Internal error!
                _callback.error(ActivityCallback.ERROR_20, e);
            }
        }
    }

    private class ConnectionDispatchRunner extends Thread {
        private Socket _socket;

        /**
         * @modifies socket, this' managers
         * @effects starts a new thread to handle the given socket and
         *  registers it with the appropriate protocol-specific manager.
         *  Returns once the thread has been started.  If socket does
         *  not speak a known protocol, closes the socket immediately and
         *  returns.
         */
        public ConnectionDispatchRunner(Socket socket) {
            super("ConnectionDispatchRunner");
            _socket=socket;
            setDaemon(true);
            start();
        }

        public void run() {
            try {
                InputStream in=_socket.getInputStream();
                _socket.setSoTimeout(SettingsManager.instance().getTimeout());
                String word=readWord(in);
                _socket.setSoTimeout(0);

                //1. Gnutella connection
                if (word.equals(SettingsManager.instance().
                        getConnectStringFirstWord())) {
                    _manager.acceptConnection(_socket);
                }
                //2. Incoming upload via HTTP
                else if (word.equals("GET")) {
                    HTTPManager mgr = new HTTPManager(_socket, _router,
                        Acceptor.this, _callback, false);
                }
                //3. Incoming download via push/HTTP.
                else if (word.equals("GIV")) {
                    HTTPManager mgr = new HTTPManager(_socket, _router,
                        Acceptor.this, _callback, true);
                }
                //4. Unknown protocol
                else {
                    throw new IOException();
                }
            } catch (IOException e) {
                //handshake failed: try to close connection.
                try { _socket.close(); } catch (IOException e2) { }
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





