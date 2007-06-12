package org.limewire.store.server;



/**
 * This represents an instance of a local server.
 */
public interface Server {

    /**
     * Single instance of {@link Factory}.
     */
    public final static Factory FACTORY = new Factory() {
        public Server newInstance(int port, DispatcherSupport.OpensSocket openner) {
            return new DemoLocalServer("localhost", 8090, openner);
        }
    };
    
    /**
     * Returns the instance {@link DispatcherSupport}.
     * 
     * @return the instance {@link DispatcherSupport}
     */
    Dispatcher getDispatcher();

    /**
     * Starts up the server.
     */
    void start();

    /**
     * Shuts down the server, waiting for <code>millis</code> milliseconds.
     * 
     * @param millis milliseconds to wait before abruptly shutting everything
     *        down
     */
    void shutDown(long millis);

    /**
     * Reports a message.
     * 
     * @param msg message to report
     * @return reponse back to code after reporting <code>msg</code>
     */
    String report(String msg);    

    public interface Factory {

        /**
         * Constructs a new {@link Server} on port <code>port</code>
         * 
         * @param port port on which to construct the server
         * @param openner delegate which opens streams on our behalf
         * @return a new instance of {@link Server}, not started
         */
        Server newInstance(int port, DispatcherSupport.OpensSocket openner);
    }
}
