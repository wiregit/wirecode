package org.limewire.lws.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Convenience class for adding all the test cases on to a test suite
 * found in a path <tt>bin</tt>
 */
public class TestUtil {

  /**
   * Adds all the {@link TestCase} classes to <tt>suite</tt>.
   * 
   * @param bin
   *          the bin directory
   * @param suite
   *          suite to which we add
   */
  public static TestSuite addAllTestCases(final String bin, final TestSuite suite) {
    final File dir = new File(bin);
    try {
      final int len = dir.getCanonicalPath().length() + 1;
      final List<File> q = new LinkedList<File>();
      q.add(dir);
      while (!q.isEmpty()) {
        final File d = q.remove(0);
        final File[] fs = d.listFiles();
        for (File f : fs) {
          if (f.isDirectory()) {
            q.add(f);
            continue;
          }
          if (!f.getName().endsWith(".class"))
            continue;
          final String path = f.getCanonicalPath();
          final String className = path.replace('/', '.').replace('\\', '.').replace(File.separatorChar, '.').substring(len, path.lastIndexOf('.'));
          if (className.contains("$"))
            continue;
          Class cls = null;
          try {
            cls = Class.forName(className);
          } catch (ClassNotFoundException e) { e = null; }
          if (cls == null) continue;
          if (Modifier.isAbstract(cls.getModifiers())) continue;
          for (Class trav = cls; trav != null && !trav.equals(Object.class); trav = trav.getSuperclass()) {
            if (trav.equals(TestCase.class)) {
              suite.addTestSuite(cls);
              break;
            }
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return suite;
  }

}
