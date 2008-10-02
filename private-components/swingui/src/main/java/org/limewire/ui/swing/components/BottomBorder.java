package org.limewire.ui.swing.components;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.Border;

/**
 * This class draws a line border only at the bottom of a component.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class BottomBorder implements Border {

    private static int THICKNESS = 5;

    public Insets getBorderInsets(Component c) {
        return new Insets(0, 0, THICKNESS, 0);
    }

    public void paintBorder(
        Component c, Graphics g, int x, int y, int width, int height) {

        int bottomY = y + height - 1;
        g.drawLine(x, bottomY, x + width - 1, bottomY);
    }

    public boolean isBorderOpaque() {
        return true;
    }
}