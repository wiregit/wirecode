package org.limewire.store.storeserver;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.StringTokenizer;

import org.limewire.store.storeserver.core.CookieGenTest;
import org.limewire.store.storeserver.util.TestUtil;


import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Runs all the tests.
 * 
 * @author jpalm
 */
public final class AllTests {
  
  private AllTests() { }

  public static Test suite() {
    final StringTokenizer st = new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);
    final String bin = st.nextToken();
    return TestUtil.addAllTestCases(bin, new TestSuite("Test for com.limewire.store.server"));
  }

}
