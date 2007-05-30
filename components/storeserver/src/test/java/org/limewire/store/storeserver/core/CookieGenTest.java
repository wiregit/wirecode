package org.limewire.store.storeserver.core;

import org.limewire.store.storeserver.api.IServer;
import org.limewire.store.storeserver.util.Util;

import junit.framework.TestCase;

public class CookieGenTest extends TestCase {

  public void test() {

    final char[] badChars = {' ', '\t', '\b', '\r', ';' };

    for (int i = 0; i < 5; i++) {
      String k = Util.generateKey();
      assertTrue(k.length() == IServer.Constants.KEY_LENGTH);
      for (char c : badChars)
        assertTrue(k, k.indexOf(c) == -1);
    }
    
  }
}
