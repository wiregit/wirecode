package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * A component that draws a line.
 */
public class Line extends JComponent {
    
    private Color color;
	private Color uiColor;
    
    /**
     * Creates a line that uses a <tt>color</tt>.
     */
    public Line(Color color) {
        this(color, 1);
    }
    
    /**
     * Creates a line <tt>height</tt> pixels high that uses a <tt>color</tt>.
     */
    public Line(Color color, int height) {
        if(color == null)
            throw new IllegalArgumentException("color must not be null");
        
        setColor(color);
        initSize(height);
    }
    
    /**
     * Creates a line that uses a color from the current theme.
     */
	public Line() {
	    this(1);
    }
	
	/**
	 * Creates a line <tt>height</tt> pixels high, that uses a color from the
	 * current theme.
	 */
	public Line(int height) {
	    uiColor = UIManager.getColor("controlShadow");
	    initSize(height);
	}
       
    private void initSize(int height) {
        setPreferredSize(new Dimension(1, height));
        setMaximumSize(new Dimension(Short.MAX_VALUE, height));		
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
        if (uiColor != null) {
        	g.setColor(uiColor);
        } else if (color != null) {
        	g.setColor(color);
        } // fall back to default foreground color
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(oldColor);
    }
 
    @Override
    public void updateUI() {
    	super.updateUI();
    	if (uiColor != null) {
    		uiColor = UIManager.getColor("controlShadow");
    	}
    }
    
}