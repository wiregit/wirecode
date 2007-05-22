package org.limewire.store.storeserver.core;

/**
   * Listens for notification to when we are connected and disconnected.
   * 
   * @author jeff
   */
  public interface ConnectionListener {
      
      /**
       * Called when a connected event changes.
       * 
       * @param isConnected <tt>true</tt> if were are connected, <tt>false</tt> otherwise
       */
      void connectionChanged(boolean isConnected);
      
      /**
       * Can add and removes {@link ConnectionListener} instances.
       * 
       * @author jeff
       */
      public interface HasSome {
          
          /**
           * Returns <tt>true</tt> if <tt>lis</tt> was added as a listener, <tt>false</tt> otherwise.
           * 
           * @param lis new listener
           * @return <tt>true</tt> if <tt>lis</tt> was added as a listener, <tt>false</tt> otherwise.
           */
          boolean addConnectionListener(ConnectionListener lis);

          /**
           * Returns <tt>true</tt> if <tt>lis</tt> was removed as a listener, <tt>false</tt> otherwise.
           * 
           * @param lis old listener
           * @return <tt>true</tt> if <tt>lis</tt> was removed as a listener, <tt>false</tt> otherwise.
           */  
          boolean removeConnectionListener(ConnectionListener lis);          
      }
  }