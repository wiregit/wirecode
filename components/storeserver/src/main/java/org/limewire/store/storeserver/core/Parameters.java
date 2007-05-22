package org.limewire.store.storeserver.core;

/**
 * Parameter names.
 * 
 * @author jpalm
 */
public final class Parameters {

  private Parameters() { }

  /**
   * Name of the callback function.
   */
  public static final String CALLBACK = "callback";
  
  /**
   * Private key.
   */
  public static final String PRIVATE = "private";
  
  /**
   * Public key.
   */
  public static final String PUBLIC = "public";
  
  /**
   * Name of the command to send to the {@link Dispatchee}.
   */
  public static final String COMMAND = "command";
  
  /**
   * Message to send to the <tt>ECHO</tt> command.
   */
  public static final String MSG = "msg";

}
