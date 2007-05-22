package org.limewire.store.storeserver.core;

import java.util.Map;

import org.limewire.store.storeserver.core.Constants;
import org.limewire.store.storeserver.core.LocalServer;
import org.limewire.store.storeserver.util.Util;


import junit.framework.TestCase;

public class CookieGenTest extends TestCase {

  public void test() {

    final char[] badChars = {' ', '\t', '\b', '\r', ';' };

    for (int i = 0; i < 5; i++) {
      String k = Util.generateKey();
      assertTrue(k.length() == Constants.KEY_LENGTH);
      for (char c : badChars)
        assertTrue(k, k.indexOf(c) == -1);
    }
    
  }
}
