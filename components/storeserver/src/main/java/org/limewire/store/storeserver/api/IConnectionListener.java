package org.limewire.store.storeserver.api;

/**
 * Listens for notification to when we are connected and disconnected.
 * 
 * @author jeff
 */
public interface IConnectionListener {

    /**
     * Called when a connected event changes.
     * 
     * @param isConnected <tt>true</tt> if were are connected, <tt>false</tt>
     *        otherwise
     */
    void connectionChanged(boolean isConnected);

    /**
     * Can add and remove {@link IConnectionListener} instances.
     * 
     * @author jeff
     */
    public interface HasSome {

        /**
         * Returns <tt>true</tt> if <tt>lis</tt> was added as a listener,
         * <tt>false</tt> otherwise.
         * 
         * @param lis new listener
         * @return <tt>true</tt> if <tt>lis</tt> was added as a listener,
         *         <tt>false</tt> otherwise.
         */
        boolean addConnectionListener(IConnectionListener lis);

        /**
         * Returns <tt>true</tt> if <tt>lis</tt> was removed as a listener,
         * <tt>false</tt> otherwise.
         * 
         * @param lis old listener
         * @return <tt>true</tt> if <tt>lis</tt> was removed as a listener,
         *         <tt>false</tt> otherwise.
         */
        boolean removeConnectionListener(IConnectionListener lis);
    }
}