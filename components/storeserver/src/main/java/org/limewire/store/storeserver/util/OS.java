package org.limewire.store.storeserver.util;

/**
 * A simple representation of an OS.
 */
public final class OS {
  
  private OS() { }

  public static boolean isMac() {
    return check("mac");
  }
  
  public static boolean isWindows() {
    return check("windows");
  }
  
  public static boolean isOther() {
    return !isWindows() && !isMac();
  }
  
  private static boolean check(final String s) {
    final String os = System.getProperty("os.name", null);
    if (os == null) return false;
    final String lc = os.toLowerCase();
    return lc.indexOf(s) != -1;
  }
  
}
