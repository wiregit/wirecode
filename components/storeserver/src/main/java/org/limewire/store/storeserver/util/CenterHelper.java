package org.limewire.store.storeserver.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

/**
 * Centers windows.
 * 
 * @author jpalm
 */
public class CenterHelper {

  public final static void center(final Component f) {
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (int) ((d.getWidth() - f.getWidth()) / 2);
    int y = (int) ((d.getHeight() - f.getHeight()) / 2);
    f.setLocation(x, y);
  }
}
