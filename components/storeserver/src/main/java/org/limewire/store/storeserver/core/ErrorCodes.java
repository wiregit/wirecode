/**
 * 
 */
package org.limewire.store.storeserver.core;

/**
 * Codes that are sent to the code (javascript) when an error occurs.
 * 
 * @author jpalm
 */
public final class ErrorCodes {

  private ErrorCodes() { }

  public static final String INVALID_PUBLIC_KEY             = "invalid.public.key";
  public static final String INVALID_PRIVATE_KEY            = "invalid.private.key";
  public static final String INVALID_PUBLIC_KEY_OR_IP       = "invalid.public.key.or.ip.address";
  public static final String MISSING_CALLBACK_PARAMETER     = "missing.callback.parameter";
  public static final String UNKNOWN_COMMAND                = "unkown.command";
  public static final String UNITIALIZED_PRIVATE_KEY        = "uninitialized.private.key";
  public static final String MISSING_PRIVATE_KEY_PARAMETER  = "missing.private.parameter";
  public static final String MISSING_PUBLIC_KEY_PARAMETER   = "missing.public.parameter";
  public static final String MISSING_COMMAND_PARAMETER      = "missing.command.parameter";
  
}