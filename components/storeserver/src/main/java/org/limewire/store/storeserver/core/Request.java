package org.limewire.store.storeserver.core;

/**
 * Contains information about the incoming request. This could expand, but
 * currently just holds an IP address.
 * 
 * @author jpalm
 */
public class Request {

  private final String ip;
  
  public Request(final String ip) {
    this.ip = ip;
  }
  
  /**
   * Returns the IP from which this request was made.
   * 
   * @return the IP from which this request was made
   */
  public final String getIP() {
    return this.ip;
  }
  
}
