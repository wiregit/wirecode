package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.AbstractBorder;

/**
 * Line border class that support individual specification of the top, left,
 * bottom and right borders.
 */
public class CustomLineBorder extends AbstractBorder {

    private final Color topColor;
    private final int topThickness;
    
    private final Color leftColor;
    private final int leftThickness;
    
    private final Color bottomColor;
    private final int bottomThickness;
    
    private final Color rightColor;
    private final int rightThickness;

    /**
     * Constructs a CustomLineBorder with black color and a thickness of 1 on
     * all four sides.
     */
    public CustomLineBorder() {
        this(Color.BLACK, 1);
    }

    /**
     * Constructs a CustomLineBorder with the specified color and thickness
     * on all four sides.
     */
    public CustomLineBorder(Color color, int thickness) {
        this(color, thickness, color, thickness, 
            color, thickness, color, thickness);
    }

    /** 
     * Constructs a CustomLineBorder with the specified color thickness on
     * each side.
     */
    public CustomLineBorder(Color topColor, int top, Color leftColor, int left,
        Color bottomColor, int bottom, Color rightColor, int right) {
        this.topColor = topColor;
        this.topThickness = top;
        this.leftColor = leftColor;
        this.leftThickness = left;
        this.bottomColor = bottomColor;
        this.bottomThickness = bottom;
        this.rightColor = rightColor;
        this.rightThickness = right;
    }

    /**
     * Returns the insets of the border.
     */
    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(topThickness, leftThickness, bottomThickness, rightThickness);
    }

    /**
     * Reinitializes the insets parameter with this Border's current Insets.
     */
    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.top = topThickness;
        insets.left = leftThickness;
        insets.bottom = bottomThickness;
        insets.right = rightThickness;
        return insets;
    }

    /**
     * Paints the border for the specified component with the specified
     * position and size.
     */
    @Override
    public void paintBorder(Component c, Graphics g,
        int x, int y, int width, int height) {

        // Save current color.
        Color oldColor = g.getColor();

        // Paint individual borders.
        g.setColor(leftColor);
        for (int i = 0; i < leftThickness; i++) {
            g.drawLine(x + i, y, x + i, y + height - 1);
        }
        
        g.setColor(rightColor);
        for (int i = 0; i < rightThickness; i++) {
            g.drawLine(x + width - i - 1, y, x + width - i - 1, y + height - 1);
        }
        
        g.setColor(topColor);
        for (int i = 0; i < topThickness; i++) {
            g.drawLine(x, y + i, x + width - 1, y + i);
        }
        
        g.setColor(bottomColor);
        for (int i = 0; i < bottomThickness; i++) {
            g.drawLine(x, y + height - i - 1, x + width - 1, y + height - i - 1);
        }

        // Restore original color.
        g.setColor(oldColor);
    }
}
