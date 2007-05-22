package org.limewire.store.storeserver.core;

import java.util.Date;

/**
 * Command base class for {@link App}s.
 * 
 * @author jpalm
 */
public abstract class App {  

  /**
   * Prints <tt>msg</tt> to the stderr.
   * 
   * @param msg   information message
   */
  protected final void note(final Object msg) {
    System.err.println("[" + new Date() + " // " + simpleName() + "] " + msg);
  }
  
  private String simpleName() {
    String s = getClass().getName();
    int ilastDot = s.lastIndexOf(".");
    return ilastDot == -1 ? s : s.substring(ilastDot + 1);
  }
}
