package org.limewire.store.storeserver.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

/**
 * Centers windows.
 */
public class CenterHelper {

    /**
     * Centers <code>f</code>.
     * <br>
     * You are assured that the following is true
     * <pre>
     * Component c = ...;
     * c == CenterHelper.center(c)
     * </pre>
     * but we include this to make passing components around easy.
     * 
     * @param f component to center
     */
    public final static Component center(final Component f) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((d.getWidth() - f.getWidth()) / 2);
        int y = (int) ((d.getHeight() - f.getHeight()) / 2);
        f.setLocation(x, y);
        return f;
    }
}
