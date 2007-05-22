package org.limewire.store.storeserver.core;

/**
 * Something with the ability to take a note -- e.g. print to stderr.
 * 
 * @author jpalm
 */
public interface Note {

  /**
   * Implement this method to send <tt>msg</tt> somewhere.
   * 
   * @param msg     message to send
   * @param level   level at which to send <tt>msg</tt>
   */
  void note(Object msg, Server.Constants.Level level);
}
