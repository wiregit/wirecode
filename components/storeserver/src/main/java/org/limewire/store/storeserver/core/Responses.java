package org.limewire.store.storeserver.core;

/**
 * Reponses sent back from servers.
 * 
 * @author jpalm
 */
public final class Responses {
  
  private Responses() { }

  /**
   * Success.
   */
  public final static String OK = "ok";
  
  /**
   * When there was a command sent to the local host, but no
   * {@link Dispatchee} was set up to handle it.
   */
  public static final String NO_DISPATCHEE = "no.dispatcher";
  
  /**
   * When there was a {@link Dispatchee} to handle this command, but it didn't understand it.
   */
  public static final String UNKNOWN_COMMAND = "unknown.command";


}
