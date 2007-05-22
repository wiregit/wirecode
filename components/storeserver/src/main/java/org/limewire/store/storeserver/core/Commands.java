package org.limewire.store.storeserver.core;

/**
 * Collection of all the commands we send.
 * 
 * @author jpalm
 */
public final class Commands {

  private Commands() { }

  /**
   * Sent from Code to Local with no parameters.
   */
  public final static String START_COM      = "StartCom";
  
  /**
   * Sent from Local to Remote with parameters.
   * <ul>
   * <li>{@link Parameters#PUBLIC}</li>
   * <li>{@link Parameters#PRIVATE}</li>
   * </ul>
   */  
  public final static String STORE_KEY      = "StoreKey";
  
  /**
   * Sent from Code to Remote with parameters.
   * <ul>
   * <li>{@link Parameters#PRIVATE}</li>
   * </ul>
   */ 
  public final static String GIVE_KEY       = "GiveKey";
  
  /**
   * Send from Code to Local with no parameters.
   */
  public final static String DETATCH        = "Detatch";
  
  /**
   * Sent from Code to Local  with parameters.
   * <ul>
   * <li>{@link Parameters#PRIVATE}</li>
   * </ul>
   */   
  public static final String AUTHENTICATE   = "Authenticate";

  /* Testing */
  
  /**
   * Sent from Code to Local with parameters.
   * <ul>
   * <li>{@link Parameters#MSG}</li>
   * </ul>
   */   
  public final static String ECHO = "Echo";
  public final static String ALERT = "Alert";
}
