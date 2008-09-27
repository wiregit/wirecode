package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

/**
 * A component that draws a line.
 */
public class Line extends JComponent {
    
    public static Line createHorizontalLine(Color color, int height) {
        return new Line(true, color, height);
    }

    public static Line createHorizontalLine(Color color) {
        return new Line(true, color, 1);
    }
    
    public static Line createHorizontalLine() {
        return new Line(true, Color.BLACK, 1);
    }
    
    public static Line createVerticalLine(Color color, int width) {
        return new Line(false, color, width);
    }
    
    public static Line createVerticalLine(Color color) {
        return new Line(false, color, 1);
    }
    
    public static Line createVerticalLine() {
        return new Line(false, Color.BLACK, 1);
    }

    private boolean horizontal;
    private Color color;

    private Line(boolean horizontal, Color color, int dimension) {
        if(color == null)
            throw new IllegalArgumentException("color must not be null");
        
        this.horizontal = horizontal;
        this.color = color;
        initSize(horizontal, dimension);
    }
       
    private void initSize(boolean horizontal, int dimension) {
        if(horizontal) {
            setPreferredSize(new Dimension(1, dimension));
            setMinimumSize(new Dimension(0, dimension));
            setMaximumSize(new Dimension(Short.MAX_VALUE, dimension));
        } else {
            setPreferredSize(new Dimension(dimension, 1));
            setMinimumSize(new Dimension(dimension, 0));
            setMaximumSize(new Dimension(dimension, Short.MAX_VALUE));       
        }
	}
    
    public void setThickness(int dimension) {
        initSize(horizontal, dimension);
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    public Color getColor() {
        return color;
    }
 
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Color oldColor = g.getColor();
        g.setColor(color);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(oldColor);
    }
    
}