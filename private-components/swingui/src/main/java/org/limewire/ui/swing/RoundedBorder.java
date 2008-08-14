package org.limewire.ui.swing;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.border.AbstractBorder;

/**
 * This class ...
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class RoundedBorder extends AbstractBorder {

    private Insets insets;
    private int cornerRadius;

    public RoundedBorder(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        insets = new Insets(
            cornerRadius, cornerRadius, cornerRadius, cornerRadius);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(
        Component c, Graphics g, int x, int y, int width, int height) {

        Graphics2D g2d = (Graphics2D) g.create();

        g2d.drawRoundRect(x, y, width - 1, height - 1,
            cornerRadius, cornerRadius);

        g2d.dispose();
    }
}