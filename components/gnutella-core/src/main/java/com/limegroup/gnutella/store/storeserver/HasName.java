/**
 * 
 */
package com.limegroup.gnutella.store.storeserver;

import java.util.Map;

abstract class HasName {

    private final String name;

    public HasName(final String name) {
      this.name = name;
    }

    public HasName() {
      String n = getClass().getName();
      int ilast;
      ilast = n.lastIndexOf(".");
      if (ilast != -1) n = n.substring(ilast + 1);
      ilast = n.lastIndexOf("$");
      if (ilast != -1) n = n.substring(ilast + 1);
      this.name = n;
    }

    public final String name() {
      return name;
    }

    protected final String getArg(final Map<String, String> args, final String key) {
      final String res = args.get(key);
      return res == null ? "" : res;
    }
    
}