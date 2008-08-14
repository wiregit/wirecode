package org.limewire.ui.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.AbstractBorder;

/**
 * This class draws a line border only at the bottom of a component.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class BottomBorder extends AbstractBorder {

    private static int THICKNESS = 50;

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(0, 0, THICKNESS, 0);
    }

    @Override
    public void paintBorder(
        Component c, Graphics g, int x, int y, int width, int height) {

        int bottomY = y + height - 1;
        g.drawLine(x, bottomY, x + width - 1, bottomY);
    }
}